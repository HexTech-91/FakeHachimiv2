package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.EntityTravelEvent;
import net.hachimi.client.impl.event.entity.player.TravelEvent;
import net.hachimi.client.impl.event.network.DismountVehicleEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.exploit.DisablerModule;
import net.hachimi.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.hachimi.client.util.player.MovementUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.math.MathHelper;

public class BoatFlyModule extends ToggleModule {
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "The horizontal flight speed", 0.1F, 2.5F, 10.0F));
   Config<Float> vspeedConfig = this.register(new NumberConfig<>("VerticalSpeed", "The vertical flight speed", 0.1F, 1.0F, 5.0F));
   Config<Boolean> remountConfig = this.register(new BooleanConfig("Remount", "Automatically remounts", false));
   Config<BoatFlyModule.AntiKick> antiKickConfig = this.register(
      new EnumConfig<>("AntiKick", "Prevents vanilla flight detection", BoatFlyModule.AntiKick.NORMAL, BoatFlyModule.AntiKick.values())
   );
   Config<Boolean> accelerateConfig = this.register(new BooleanConfig("Accelerate", "Accelerate as you fly", false));
   Config<Float> accelerateSpeedConfig = this.register(
      new NumberConfig<>("AccelerateSpeed", "Speed to accelerate as", 0.01F, 0.2F, 1.0F, () -> this.accelerateConfig.getValue())
   );
   Config<Float> maxSpeedConfig = this.register(
      new NumberConfig<>("MaxSpeed", "Max speed to accelerate to", 1.0F, 5.0F, 10.0F, () -> this.accelerateConfig.getValue())
   );
   private double speed;
   private double lastY;
   private boolean floating;
   private int floatingTicks;
   private boolean modifyY;

   public BoatFlyModule() {
      super("BoatFly", "Allows you fly with entities", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.speed = 0.0;
   }

   @Override
   public void onDisable() {
      this.modifyY = false;
   }

   @EventListener
   public void onTravel(EntityTravelEvent event) {
      if (mc.player.getVehicle() != null && event.getEntity().getId() == mc.player.getVehicle().getId()) {
         event.cancel();
      }
   }

   @EventListener
   public void onDismountVehicle(DismountVehicleEvent event) {
      if (mc.options.sneakKey.isPressed()) {
         event.cancel();
      }
   }

   @EventListener
   public void onPlayerTick(TravelEvent event) {
      if (mc.player.getVehicle() != null && !DisablerModule.getInstance().grimFireworkCheck()) {
         Entity ridingEntity = mc.player.getVehicle();
         if (this.accelerateConfig.getValue()) {
            if (!MovementUtil.isInputtingMovement() || ridingEntity.horizontalCollision) {
               this.speed = 0.0;
            }

            this.speed = this.speed + this.accelerateSpeedConfig.getValue().floatValue();
            if (this.speed > this.maxSpeedConfig.getValue().floatValue()) {
               this.speed = this.maxSpeedConfig.getValue().floatValue();
            }
         } else {
            this.speed = this.speedConfig.getValue().floatValue();
         }

         ridingEntity.setYaw(mc.player.getYaw());
         if (ridingEntity instanceof LlamaEntity llama) {
            llama.headYaw = mc.player.getYaw();
         }

         boolean stopVerticalMovement = false;
         if (this.floating) {
            this.floatingTicks++;
            if (this.floatingTicks >= 20) {
               if (this.antiKickConfig.getValue() == BoatFlyModule.AntiKick.PACKET) {
                  this.modifyY = true;
               } else if (this.antiKickConfig.getValue() == BoatFlyModule.AntiKick.NORMAL) {
                  ridingEntity.setPosition(ridingEntity.getX(), ridingEntity.getY() - 0.0313, ridingEntity.getZ());
               }

               this.floatingTicks = 0;
               this.floating = false;
               stopVerticalMovement = true;
            }
         }

         ridingEntity.setVelocity(ridingEntity.getVelocity().x, 0.0, ridingEntity.getVelocity().z);
         if (mc.options.jumpKey.isPressed() && !stopVerticalMovement) {
            ridingEntity.setVelocity(ridingEntity.getVelocity().x, this.vspeedConfig.getValue().floatValue(), ridingEntity.getVelocity().z);
         } else if (mc.options.sneakKey.isPressed()) {
            ridingEntity.setVelocity(ridingEntity.getVelocity().x, -this.vspeedConfig.getValue(), ridingEntity.getVelocity().z);
         }

         this.speed = Math.max(this.speed, 0.2873F);
         float forward = mc.player.input.movementForward;
         float strafe = mc.player.input.movementSideways;
         float yaw = mc.player.getYaw();
         if (forward != 0.0F || strafe != 0.0F) {
            double rx = Math.cos(Math.toRadians(yaw + 90.0F));
            double rz = Math.sin(Math.toRadians(yaw + 90.0F));
            double var10001 = forward * this.speed * rx + strafe * this.speed * rz;
            double var10003 = forward * this.speed * rz;
            double var10004 = strafe * this.speed;
            ridingEntity.setVelocity(var10001, ridingEntity.getVelocity().y, var10003 - var10004 * rx);
            event.cancel();
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.player.getVehicle() != null) {
         Entity ridingEntity = mc.player.getVehicle();
         if (event.getPacket() instanceof VehicleMoveC2SPacket packet && this.antiKickConfig.getValue() != BoatFlyModule.AntiKick.OFF) {
            double packetY = packet.getY();
            if (this.modifyY) {
               ((AccessorPlayerMoveC2SPacket)packet).hookSetY(this.lastY - 0.04);
               this.modifyY = false;
            } else {
               this.floating = this.floatingCheck(packet, ridingEntity);
               this.lastY = packetY;
            }
         }

         if (event.getPacket() instanceof ClientCommandC2SPacket packetx
            && packetx.getMode() == Mode.PRESS_SHIFT_KEY
            && mc.player.getVehicle() != null
            && !mc.player.getVehicle().isOnGround()) {
            event.cancel();
         }
      }
   }

   private boolean floatingCheck(VehicleMoveC2SPacket packet, Entity ridingEntity) {
      double e = MathHelper.clamp(packet.getY(), -2.0E7, 2.0E7);
      double s = e - this.lastY;
      return s >= -0.03125 && !ridingEntity.groundCollision && this.isEntityOnAir(ridingEntity);
   }

   private boolean isEntityOnAir(Entity entity) {
      return entity.getWorld().getStatesInBox(entity.getBoundingBox().expand(0.0625).stretch(0.0, -0.55, 0.0)).allMatch(AbstractBlockState::isAir);
   }

   public static enum AntiKick {
      NORMAL,
      PACKET,
      OFF;
   }
}
