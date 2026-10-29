package net.hachimi.client.impl.module.movement;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.NumberDisplay;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.player.PushEntityEvent;
import net.hachimi.client.impl.event.entity.player.PushFluidsEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.network.PushOutOfBlocksEvent;
import net.hachimi.client.impl.module.combat.SurroundModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorBundlePacket;
import net.hachimi.client.mixin.accessor.AccessorClientWorld;
import net.hachimi.client.mixin.accessor.AccessorEntityVelocityUpdateS2CPacket;
import net.hachimi.client.mixin.accessor.AccessorExplosionS2CPacket;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class VelocityModule extends ToggleModule {
   private static VelocityModule INSTANCE;
   Config<Boolean> knockbackConfig = this.register(new BooleanConfig("Knockback", "Removes player knockback velocity", true));
   Config<Boolean> explosionConfig = this.register(new BooleanConfig("Explosion", "Removes player explosion velocity", true));
   Config<VelocityModule.VelocityMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for velocity", VelocityModule.VelocityMode.NORMAL, VelocityModule.VelocityMode.values())
   );
   Config<Float> horizontalConfig = this.register(
      new NumberConfig<>(
         "Horizontal",
         "How much horizontal knock-back to take",
         0.0F,
         0.0F,
         100.0F,
         NumberDisplay.PERCENT,
         () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL || this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS
      )
   );
   Config<Float> verticalConfig = this.register(
      new NumberConfig<>(
         "Vertical",
         "How much vertical knock-back to take",
         0.0F,
         0.0F,
         100.0F,
         NumberDisplay.PERCENT,
         () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL || this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS
      )
   );
   Config<Boolean> concealConfig = this.register(new BooleanConfig("Conceal", "Fixes velocity on servers with excessive setbacks", false));
   Config<Boolean> wallsAirConfig = this.register(
      new BooleanConfig(
         "GroundOnly", "Only applies velocity in walls while on ground", false, () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS
      )
   );
   Config<Boolean> wallsTrappedConfig = this.register(
      new BooleanConfig(
         "Trapped", "Applies velocity while player head is trapped", false, () -> this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS
      )
   );
   Config<Boolean> pushEntitiesConfig = this.register(new BooleanConfig("NoPush-Entities", "Prevents being pushed away from entities", true));
   Config<Boolean> pushBlocksConfig = this.register(new BooleanConfig("NoPush-Blocks", "Prevents being pushed out of blocks", true));
   Config<Boolean> pushLiquidsConfig = this.register(new BooleanConfig("NoPush-Liquids", "Prevents being pushed by flowing liquids", true));
   Config<Boolean> pushFishhookConfig = this.register(new BooleanConfig("NoPush-Fishhook", "Prevents being pulled by fishing rod hooks", true));
   private boolean cancelVelocity;
   private boolean concealVelocity;

   public VelocityModule() {
      super("Velocity", "Reduces the amount of player knockback velocity", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static VelocityModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      if (this.modeConfig.getValue() == VelocityModule.VelocityMode.NORMAL) {
         DecimalFormat decimal = new DecimalFormat("0.0");
         return String.format("H:%s%%, V:%s%%", decimal.format(this.horizontalConfig.getValue()), decimal.format(this.verticalConfig.getValue()));
      } else {
         return EnumFormatter.formatEnum(this.modeConfig.getValue());
      }
   }

   @Override
   public void onEnable() {
      this.cancelVelocity = false;
   }

   @Override
   public void onDisable() {
      if (this.cancelVelocity) {
         if (this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM) {
            float yaw = mc.player.getYaw();
            float pitch = mc.player.getPitch();
            if (Managers.ROTATION.isRotating()) {
               yaw = Managers.ROTATION.getRotationYaw();
               pitch = Managers.ROTATION.getRotationPitch();
            }

            Managers.NETWORK.sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
            Managers.NETWORK
               .sendPacket(
                  new PlayerActionC2SPacket(
                     Action.STOP_DESTROY_BLOCK, mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN
                  )
               );
         }

         this.cancelVelocity = false;
      }

      this.concealVelocity = false;
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof PlayerPositionLookS2CPacket && this.concealConfig.getValue()) {
            this.concealVelocity = true;
         }

         if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && this.knockbackConfig.getValue()) {
            if (packet.getEntityId() != mc.player.getId()) {
               return;
            }

            if (this.concealVelocity && packet.getVelocityX() == 0.0 && packet.getVelocityZ() == 0.0 && packet.getVelocityZ() == 0.0) {
               this.concealVelocity = false;
               return;
            }

            if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS) {
               if (!this.isPhased() && (!this.wallsTrappedConfig.getValue() || !this.isWallsTrapped())) {
                  return;
               }

               if (this.wallsAirConfig.getValue() && !Managers.POSITION.isOnGround()) {
                  return;
               }
            }

            switch ((VelocityModule.VelocityMode)(Object)this.modeConfig.getValue()) {
               case NORMAL:
               case WALLS:
                  if (this.horizontalConfig.getValue() == 0.0F && this.verticalConfig.getValue() == 0.0F) {
                     event.cancel();
                     return;
                  }

                  ((AccessorEntityVelocityUpdateS2CPacket)packet).setVelocityX((int)(packet.getVelocityX() * (this.horizontalConfig.getValue() / 100.0F)));
                  ((AccessorEntityVelocityUpdateS2CPacket)packet).setVelocityY((int)(packet.getVelocityY() * (this.verticalConfig.getValue() / 100.0F)));
                  ((AccessorEntityVelocityUpdateS2CPacket)packet).setVelocityZ((int)(packet.getVelocityZ() * (this.horizontalConfig.getValue() / 100.0F)));
                  break;
               case GRIM:
                  if (!Managers.ANTICHEAT.hasPassed(100L)) {
                     return;
                  }

                  event.cancel();
                  this.cancelVelocity = true;
                  break;
               case GRIM_V3:
                  event.setCanceled(this.isPhased());
            }
         } else if (event.getPacket() instanceof ExplosionS2CPacket packet && this.explosionConfig.getValue()) {
            if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS && !this.isPhased()) {
               return;
            }

            switch ((VelocityModule.VelocityMode)(Object)this.modeConfig.getValue()) {
               case NORMAL:
               case WALLS:
                  if (this.horizontalConfig.getValue() == 0.0F && this.verticalConfig.getValue() == 0.0F) {
                     event.cancel();
                     break;
                  }

                  ((AccessorExplosionS2CPacket)packet).setPlayerVelocityX(packet.getPlayerVelocityX() * (this.horizontalConfig.getValue() / 100.0F));
                  ((AccessorExplosionS2CPacket)packet).setPlayerVelocityY(packet.getPlayerVelocityY() * (this.verticalConfig.getValue() / 100.0F));
                  ((AccessorExplosionS2CPacket)packet).setPlayerVelocityZ(packet.getPlayerVelocityZ() * (this.horizontalConfig.getValue() / 100.0F));
                  break;
               case GRIM:
                  if (!Managers.ANTICHEAT.hasPassed(100L)) {
                     return;
                  }

                  event.cancel();
                  this.cancelVelocity = true;
                  break;
               case GRIM_V3:
                  event.setCanceled(this.isPhased());
            }

            if (event.isCanceled()) {
               mc.executeSync(
                  () -> ((AccessorClientWorld)mc.world)
                     .hookPlaySound(
                        packet.getX(),
                        packet.getY(),
                        packet.getZ(),
                        SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        SoundCategory.BLOCKS,
                        4.0F,
                        (1.0F + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2F) * 0.7F,
                        false,
                        RANDOM.nextLong()
                     )
               );
            }
         } else if (event.getPacket() instanceof BundleS2CPacket packet) {
            List<Packet<?>> allowedBundle = new ArrayList<>();

            for (Packet<?> packet1 : packet.getPackets()) {
               if (packet1 instanceof ExplosionS2CPacket packet2 && this.explosionConfig.getValue()) {
                  mc.executeSync(
                     () -> ((AccessorClientWorld)mc.world)
                        .hookPlaySound(
                           packet2.getX(),
                           packet2.getY(),
                           packet2.getZ(),
                           SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                           SoundCategory.BLOCKS,
                           4.0F,
                           (1.0F + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2F) * 0.7F,
                           false,
                           RANDOM.nextLong()
                        )
                  );
                  if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS && !this.isPhased()) {
                     allowedBundle.add(packet1);
                     continue;
                  }

                  switch ((VelocityModule.VelocityMode)(Object)this.modeConfig.getValue()) {
                     case NORMAL:
                     case WALLS:
                        if (this.horizontalConfig.getValue() == 0.0F && this.verticalConfig.getValue() == 0.0F) {
                           continue;
                        }

                        ((AccessorExplosionS2CPacket)packet2).setPlayerVelocityX(packet2.getPlayerVelocityX() * (this.horizontalConfig.getValue() / 100.0F));
                        ((AccessorExplosionS2CPacket)packet2).setPlayerVelocityY(packet2.getPlayerVelocityY() * (this.verticalConfig.getValue() / 100.0F));
                        ((AccessorExplosionS2CPacket)packet2).setPlayerVelocityZ(packet2.getPlayerVelocityZ() * (this.horizontalConfig.getValue() / 100.0F));
                        break;
                     case GRIM:
                        if (Managers.ANTICHEAT.hasPassed(100L)) {
                           allowedBundle.add(packet1);
                        } else {
                           this.cancelVelocity = true;
                        }
                        continue;
                     case GRIM_V3:
                        if (this.isPhased()) {
                           continue;
                        }
                  }
               } else if (packet1 instanceof EntityVelocityUpdateS2CPacket packet2 && this.knockbackConfig.getValue()) {
                  if (packet2.getEntityId() != mc.player.getId()) {
                     allowedBundle.add(packet1);
                     continue;
                  }

                  if (this.modeConfig.getValue() == VelocityModule.VelocityMode.WALLS) {
                     if (!this.isPhased() && (!this.wallsTrappedConfig.getValue() || !this.isWallsTrapped())) {
                        allowedBundle.add(packet1);
                        return;
                     }

                     if (this.wallsAirConfig.getValue() && !Managers.POSITION.isOnGround()) {
                        allowedBundle.add(packet1);
                        continue;
                     }
                  }

                  switch ((VelocityModule.VelocityMode)(Object)this.modeConfig.getValue()) {
                     case NORMAL:
                     case WALLS:
                        if (this.horizontalConfig.getValue() == 0.0F && this.verticalConfig.getValue() == 0.0F) {
                           continue;
                        }

                        ((AccessorEntityVelocityUpdateS2CPacket)packet2)
                           .setVelocityX((int)(packet2.getVelocityX() * (this.horizontalConfig.getValue() / 100.0F)));
                        ((AccessorEntityVelocityUpdateS2CPacket)packet2)
                           .setVelocityY((int)(packet2.getVelocityY() * (this.verticalConfig.getValue() / 100.0F)));
                        ((AccessorEntityVelocityUpdateS2CPacket)packet2)
                           .setVelocityZ((int)(packet2.getVelocityZ() * (this.horizontalConfig.getValue() / 100.0F)));
                        break;
                     case GRIM:
                        if (!Managers.ANTICHEAT.hasPassed(100L)) {
                           allowedBundle.add(packet1);
                        } else {
                           this.cancelVelocity = true;
                        }
                        continue;
                     case GRIM_V3:
                        if (this.isPhased()) {
                           continue;
                        }
                  }
               }

               allowedBundle.add(packet1);
            }

            ((AccessorBundlePacket)packet).setIterable(allowedBundle);
         } else if (event.getPacket() instanceof EntityDamageS2CPacket packet
            && packet.entityId() == mc.player.getId()
            && this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM_V3
            && this.isPhased()) {
            Managers.NETWORK.sendPacket(new OnGroundOnly(false));
            Managers.NETWORK.sendPacket(new OnGroundOnly(true));
         } else if (event.getPacket() instanceof EntityStatusS2CPacket packet
            && packet.getStatus() == 31
            && this.pushFishhookConfig.getValue()
            && packet.getEntity(mc.world) instanceof FishingBobberEntity hook
            && hook.getHookedEntity() == mc.player) {
            event.cancel();
         }
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      this.concealVelocity = false;
      if (this.cancelVelocity) {
         if (this.modeConfig.getValue() == VelocityModule.VelocityMode.GRIM) {
            float yaw = Managers.ROTATION.getServerYaw();
            float pitch = Managers.ROTATION.getServerPitch();
            if (Managers.ROTATION.isRotating()) {
               yaw = Managers.ROTATION.getRotationYaw();
               pitch = Managers.ROTATION.getRotationPitch();
            }

            Managers.NETWORK.sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
            Managers.NETWORK
               .sendPacket(
                  new PlayerActionC2SPacket(
                     Action.STOP_DESTROY_BLOCK, mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN
                  )
               );
         }

         this.cancelVelocity = false;
      }
   }

   @EventListener
   public void onPushEntity(PushEntityEvent event) {
      if (this.pushEntitiesConfig.getValue() && event.getPushed().equals(mc.player)) {
         event.cancel();
      }
   }

   @EventListener
   public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
      if (this.pushBlocksConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onPushFluid(PushFluidsEvent event) {
      if (this.pushLiquidsConfig.getValue()) {
         event.cancel();
      }
   }

   private boolean isWallsTrapped() {
      BlockPos headPos = mc.player.getBlockPos().up(mc.player.isCrawling() ? 1 : 2);
      return mc.world.getBlockState(headPos).isReplaceable()
         ? false
         : SurroundModule.getInstance()
            .getSurroundNoDown(mc.player)
            .stream()
            .noneMatch(blockPos -> mc.world.getBlockState(mc.player.isCrawling() ? blockPos : blockPos.up()).isReplaceable());
   }

   private boolean isPhased() {
      return PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream().anyMatch(blockPos -> !mc.world.getBlockState(blockPos).isReplaceable());
   }

   private static enum VelocityMode {
      NORMAL,
      WALLS,
      GRIM,
      GRIM_V3;
   }
}
