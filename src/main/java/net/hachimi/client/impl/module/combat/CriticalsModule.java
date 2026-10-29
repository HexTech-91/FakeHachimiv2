package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.imixin.IPlayerInteractEntityC2SPacket;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.network.InteractType;
import net.hachimi.client.util.player.InventoryUtil;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class CriticalsModule extends ToggleModule {
   private static CriticalsModule INSTANCE;
   Config<Boolean> multitaskConfig = this.register(new BooleanConfig("Multitask", "Allows crits when other combat modules are enabled", true));
   Config<CriticalsModule.CritMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "Mode for critical attack modifier", CriticalsModule.CritMode.PACKET, CriticalsModule.CritMode.values())
   );
   Config<Boolean> phaseOnlyConfig = this.register(
      new BooleanConfig(
         "PhasedOnly",
         "Only attempts criticals when phased",
         false,
         () -> this.modeConfig.getValue() == CriticalsModule.CritMode.GRIM_V3 || this.modeConfig.getValue() == CriticalsModule.CritMode.GRIM
      )
   );
   Config<Boolean> wallsOnlyConfig = this.register(
      new BooleanConfig(
         "WallsOnly",
         "Only attempts criticals in walls",
         false,
         () -> (this.modeConfig.getValue() == CriticalsModule.CritMode.GRIM_V3 || this.modeConfig.getValue() == CriticalsModule.CritMode.GRIM)
            && this.phaseOnlyConfig.getValue()
      )
   );
   Config<Boolean> moveFixConfig = this.register(
      new BooleanConfig(
         "MoveFix",
         "Pauses crits when moving",
         false,
         () -> this.modeConfig.getValue() == CriticalsModule.CritMode.GRIM_V3 || this.modeConfig.getValue() == CriticalsModule.CritMode.GRIM
      )
   );
   private final Timer attackTimer = new CacheTimer();
   private boolean postUpdateGround;
   private boolean postUpdateSprint;

   public CriticalsModule() {
      super("Criticals", "Modifies attacks to always land critical hits", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static CriticalsModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @Override
   public void onDisable() {
      this.postUpdateGround = false;
      this.postUpdateSprint = false;
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null) {
         if (!AutoCrystalModule.getInstance().isAttacking() && !AutoCrystalModule.getInstance().isPlacing()) {
            if (this.multitaskConfig.getValue()
               || !SurroundModule.getInstance().isPlacing()
                  && !SelfTrapModule.getInstance().isPlacing()
                  && !AutoTrapModule.getInstance().isPlacing()
                  && !AutoCrawlTrapModule.getInstance().isPlacing()
                  && !AutoWebModule.getInstance().isPlacing()
                  && !AutoXPModule.getInstance().isEnabled()
                  && !AutoMineModule.getInstance().isEnabled()) {
               if (event.getPacket() instanceof IPlayerInteractEntityC2SPacket packet && packet.getType() == InteractType.ATTACK) {
                  if (mc.player.isRiding()
                     || mc.player.isFallFlying()
                     || mc.player.isTouchingWater()
                     || mc.player.isInLava()
                     || mc.player.isHoldingOntoLadder()
                     || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                     || InventoryUtil.isHolding32k()) {
                     return;
                  }

                  Entity e = packet.getEntity();
                  if (e == null || !e.isAlive() || !(e instanceof LivingEntity)) {
                     return;
                  }

                  if (EntityUtil.isVehicle(e)) {
                     if (this.modeConfig.getValue() == CriticalsModule.CritMode.PACKET) {
                        for (int i = 0; i < 5; i++) {
                           Managers.NETWORK.sendQuietPacket(PlayerInteractEntityC2SPacket.attack(e, Managers.POSITION.isSneaking()));
                           Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                        }
                     }

                     return;
                  }

                  this.postUpdateSprint = mc.player.isSprinting();
                  if (this.postUpdateSprint) {
                     Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  }

                  this.attackSpoofJump(e);
               }
            }
         }
      }
   }

   public void attackSpoofJump(Entity e) {
      double x = Managers.POSITION.getX();
      double y = Managers.POSITION.getY();
      double z = Managers.POSITION.getZ();
      switch ((CriticalsModule.CritMode)(Object)this.modeConfig.getValue()) {
         case PACKET:
            if (mc.player.isOnGround() && !mc.player.input.jumping) {
               Managers.NETWORK.sendPacket(new PositionAndOnGround(x, y + 0.0625, z, false));
               Managers.NETWORK.sendPacket(new PositionAndOnGround(x, y, z, false));
               mc.player.addCritParticles(e);
            }
            break;
         case PACKET_STRICT:
            if (this.attackTimer.passed(500) && mc.player.isOnGround() && !mc.player.input.jumping) {
               Managers.NETWORK.sendPacket(new PositionAndOnGround(x, y + 1.1E-7F, z, false));
               Managers.NETWORK.sendPacket(new PositionAndOnGround(x, y + 1.0E-8F, z, false));
               this.postUpdateGround = true;
               this.attackTimer.reset();
            }
            break;
         case VANILLA:
            if (mc.player.isOnGround() && !mc.player.input.jumping) {
               double d = 1.0E-7 + 1.0E-7 * (1.0 + RANDOM.nextInt(RANDOM.nextBoolean() ? 34 : 43));
               Managers.NETWORK.sendPacket(new PositionAndOnGround(x, y + 0.1016F + d * 3.0, z, false));
               Managers.NETWORK.sendPacket(new PositionAndOnGround(x, y + 0.0202F + d * 2.0, z, false));
               Managers.NETWORK.sendPacket(new PositionAndOnGround(x, y + 3.239E-4 + d, z, false));
               mc.player.addCritParticles(e);
            }
            break;
         case GRIM:
            if (this.phaseOnlyConfig.getValue() && (this.wallsOnlyConfig.getValue() ? !this.isDoublePhased() : !this.isPhased())) {
               return;
            }

            if (this.moveFixConfig.getValue() && MovementUtil.isMovingInput()) {
               return;
            }

            if (this.attackTimer.passed(250) && mc.player.isOnGround() && !mc.player.isCrawling()) {
               float yaw = Managers.ROTATION.getServerYaw();
               float pitch = Managers.ROTATION.getServerPitch();
               if (Managers.ROTATION.isRotating()) {
                  yaw = Managers.ROTATION.getRotationYaw();
                  pitch = Managers.ROTATION.getRotationPitch();
               }

               Managers.NETWORK.sendPacket(new Full(x, y + 0.0625, z, yaw, pitch, false));
               Managers.NETWORK.sendPacket(new Full(x, y + 0.0625013579, z, yaw, pitch, false));
               Managers.NETWORK.sendPacket(new Full(x, y + 1.3579E-6, z, yaw, pitch, false));
               this.attackTimer.reset();
            }
            break;
         case GRIM_V3:
            if (this.phaseOnlyConfig.getValue() && (this.wallsOnlyConfig.getValue() ? !this.isDoublePhased() : !this.isPhased())) {
               return;
            }

            if (this.moveFixConfig.getValue() && MovementUtil.isMovingInput()) {
               return;
            }

            if (mc.player.isOnGround() && !mc.player.isCrawling()) {
               float yaw = Managers.ROTATION.getServerYaw();
               float pitch = Managers.ROTATION.getServerPitch();
               if (Managers.ROTATION.isRotating()) {
                  yaw = Managers.ROTATION.getRotationYaw();
                  pitch = Managers.ROTATION.getRotationPitch();
               }

               Managers.NETWORK.sendPacket(new Full(x, y, z, yaw, pitch, true));
               Managers.NETWORK.sendPacket(new Full(x, y + 0.0625, z, yaw, pitch, false));
               Managers.NETWORK.sendPacket(new Full(x, y + 0.04535F, z, yaw, pitch, false));
            }
            break;
         case LOW_HOP:
            Managers.MOVEMENT.setMotionY(0.3425);
      }
   }

   @EventListener
   public void onPacketOutboundPost(PacketEvent.OutboundPost event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            if (this.postUpdateGround) {
               Managers.NETWORK.sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
               this.postUpdateGround = false;
            }

            if (this.postUpdateSprint) {
               Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
               this.postUpdateSprint = false;
            }
         }
      }
   }

   public boolean isGrim() {
      return this.modeConfig.getValue() == CriticalsModule.CritMode.GRIM;
   }

   public boolean isDoublePhased() {
      for (BlockPos pos : PositionUtil.getAllInBox(mc.player.getBoundingBox(), mc.player.getBlockPos())) {
         BlockState state = mc.world.getBlockState(pos);
         BlockState state2 = mc.world.getBlockState(pos.up());
         if (state.blocksMovement() && state2.blocksMovement()) {
            return true;
         }
      }

      return false;
   }

   public boolean isPhased() {
      for (BlockPos pos : PositionUtil.getAllInBox(mc.player.getBoundingBox())) {
         if (mc.world.getBlockState(pos).blocksMovement()) {
            return true;
         }
      }

      return false;
   }

   public static enum CritMode {
      PACKET,
      PACKET_STRICT,
      VANILLA,
      GRIM,
      GRIM_V3,
      LOW_HOP;
   }
}
