package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.exploit.DisablerModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.MathHelper;

public class FlightModule extends ToggleModule {
   private static FlightModule INSTANCE;
   Config<FlightModule.FlightMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for vanilla flight", FlightModule.FlightMode.NORMAL, FlightModule.FlightMode.values())
   );
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The horizontal flight speed", 0.1F, 2.5F, 10.0F));
   Config<Float> vspeedConfig = this.register(new NumberConfig<>("VerticalSpeed", "The vertical flight speed", 0.1F, 1.0F, 5.0F));
   Config<FlightModule.AntiKick> antiKickConfig = this.register(
      new EnumConfig<>("AntiKick", "Prevents vanilla flight detection", FlightModule.AntiKick.NORMAL, FlightModule.AntiKick.values())
   );
   Config<Boolean> accelerateConfig = this.register(new BooleanConfig("Accelerate", "Accelerate as you fly", false));
   Config<Float> accelerateSpeedConfig = this.register(
      new NumberConfig<>("AccelerateSpeed", "Speed to accelerate as", 0.01F, 0.2F, 1.0F, () -> this.accelerateConfig.getValue())
   );
   Config<Float> maxSpeedConfig = this.register(
      new NumberConfig<>("MaxSpeed", "Max speed to acceleratee to", 1.0F, 5.0F, 10.0F, () -> this.accelerateConfig.getValue())
   );
   private double speed;
   private double lastY;
   private boolean floating;
   private int floatingTicks;
   private boolean modifyY;

   public FlightModule() {
      super("Flight", "Allows the player to fly in survival", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static FlightModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @Override
   public void onEnable() {
      if (this.modeConfig.getValue() == FlightModule.FlightMode.VANILLA) {
         this.enableVanillaFly();
      }

      this.speed = 0.0;
   }

   @Override
   public void onDisable() {
      if (this.modeConfig.getValue() == FlightModule.FlightMode.VANILLA) {
         this.disableVanillaFly();
      }

      this.modifyY = false;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (this.accelerateConfig.getValue()) {
         if (!MovementUtil.isInputtingMovement() || mc.player.horizontalCollision) {
            this.speed = 0.0;
         }

         this.speed = this.speed + this.accelerateSpeedConfig.getValue().floatValue();
         if (this.speed > this.maxSpeedConfig.getValue().floatValue()) {
            this.speed = this.maxSpeedConfig.getValue().floatValue();
         }
      } else {
         this.speed = this.speedConfig.getValue().floatValue();
      }

      if (DisablerModule.getInstance().grimFireworkCheck()) {
         if (this.modeConfig.getValue() == FlightModule.FlightMode.VANILLA) {
            this.disableVanillaFly();
         }
      } else {
         if (this.modeConfig.getValue().equals(FlightModule.FlightMode.VANILLA)) {
            this.enableVanillaFly();
            mc.player.getAbilities().setFlySpeed((float)(this.speed * 0.05F));
         } else {
            this.disableVanillaFly();
         }

         boolean stopVerticalMovement = false;
         if (this.floating) {
            this.floatingTicks++;
            if (this.floatingTicks >= 20) {
               if (this.antiKickConfig.getValue() == FlightModule.AntiKick.PACKET) {
                  this.modifyY = true;
               } else if (this.antiKickConfig.getValue() == FlightModule.AntiKick.NORMAL) {
                  mc.player.setPosition(mc.player.getX(), mc.player.getY() - 0.0313, mc.player.getZ());
                  if (this.modeConfig.getValue() == FlightModule.FlightMode.VANILLA) {
                     this.disableVanillaFly();
                     Managers.MOVEMENT.setMotionY(0.0);
                     stopVerticalMovement = true;
                  }
               }

               this.floatingTicks = 0;
               this.floating = false;
            }
         }

         if (this.modeConfig.getValue() == FlightModule.FlightMode.NORMAL) {
            Managers.MOVEMENT.setMotionY(0.0);
            if (mc.options.jumpKey.isPressed() && !stopVerticalMovement) {
               Managers.MOVEMENT.setMotionY(this.vspeedConfig.getValue().floatValue());
            } else if (mc.options.sneakKey.isPressed()) {
               Managers.MOVEMENT.setMotionY(-this.vspeedConfig.getValue());
            }

            this.speed = Math.max(this.speed, 0.2873F);
            float forward = mc.player.input.movementForward;
            float strafe = mc.player.input.movementSideways;
            float yaw = mc.player.getYaw();
            if (forward == 0.0F && strafe == 0.0F) {
               Managers.MOVEMENT.setMotionXZ(0.0, 0.0);
               return;
            }

            double rx = Math.cos(Math.toRadians(yaw + 90.0F));
            double rz = Math.sin(Math.toRadians(yaw + 90.0F));
            Managers.MOVEMENT.setMotionXZ(forward * this.speed * rx + strafe * this.speed * rz, forward * this.speed * rz - strafe * this.speed * rx);
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof PlayerMoveC2SPacket packet && this.antiKickConfig.getValue() != FlightModule.AntiKick.OFF) {
            double packetY = packet.getY(Double.NaN);
            if (!Double.isNaN(packetY)) {
               if (this.modifyY) {
                  ((AccessorPlayerMoveC2SPacket)packet).hookSetY(this.lastY - 0.04);
                  this.modifyY = false;
                  return;
               }

               this.floating = this.floatingCheck(packet);
               this.lastY = packetY;
               return;
            }

            if (this.modifyY) {
               PlayerMoveC2SPacket packet1 = (PlayerMoveC2SPacket)(packet.changesLook()
                  ? new Full(mc.player.getX(), mc.player.getY() - 0.04, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), packet.isOnGround())
                  : new PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.04, mc.player.getZ(), packet.isOnGround()));
               event.cancel();
               Managers.NETWORK.sendQuietPacket(packet1);
               this.modifyY = false;
            }
         }
      }
   }

   private boolean floatingCheck(PlayerMoveC2SPacket packet) {
      double e = MathHelper.clamp(packet.getY(mc.player.getY()), -2.0E7, 2.0E7);
      double s = e - this.lastY;
      return s >= -0.03125 && !mc.player.groundCollision && this.isEntityOnAir(mc.player);
   }

   private boolean isEntityOnAir(Entity entity) {
      return entity.getWorld().getStatesInBox(entity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0)).allMatch(AbstractBlockState::isAir);
   }

   private void enableVanillaFly() {
      mc.player.getAbilities().allowFlying = true;
      mc.player.getAbilities().flying = true;
   }

   private void disableVanillaFly() {
      if (!mc.player.isCreative()) {
         mc.player.getAbilities().allowFlying = false;
      }

      mc.player.getAbilities().flying = false;
      mc.player.getAbilities().setFlySpeed(0.05F);
   }

   public static enum AntiKick {
      NORMAL,
      PACKET,
      OFF;
   }

   public static enum FlightMode {
      NORMAL,
      VANILLA;
   }
}
