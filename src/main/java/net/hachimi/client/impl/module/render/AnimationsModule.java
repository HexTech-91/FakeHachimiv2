package net.hachimi.client.impl.module.render;

import java.util.ArrayList;
import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.entity.LimbAnimationEvent;
import net.hachimi.client.impl.event.entity.SwingSpeedEvent;
import net.hachimi.client.impl.event.entity.UpdateServerPositionEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.render.item.EatTransformationEvent;
import net.hachimi.client.impl.event.render.item.RenderSwingAnimationEvent;
import net.hachimi.client.impl.event.render.item.UpdateHeldItemsEvent;
import net.hachimi.client.mixin.accessor.AccessorBundlePacket;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;

public final class AnimationsModule extends ToggleModule {
   Config<Boolean> noSwitchConfig = this.register(new BooleanConfig("NoSwitchAnimation", "Removes the animation when switching items", false));
   Config<Boolean> oldSwingConfig = this.register(new BooleanConfig("OldSwingAnimation", "Reverts to the 1.8 swinging animations", false));
   Config<Boolean> swingSpeedConfig = this.register(new BooleanConfig("SwingSpeed", "Allows you to modify your swing speed.", false));
   Config<Integer> swingFactorConfig = this.register(
      new NumberConfig<>("SwingFactor", "The speed of your swing.", 1, 6, 20, () -> this.swingSpeedConfig.getValue())
   );
   Config<Boolean> selfOnlyConfig = this.register(new BooleanConfig("SelfOnly", "Make the module only affect yourself", true, () -> false));
   Config<Boolean> eatTransformConfig = this.register(new BooleanConfig("EatTransform", "Transforms the first person eating animation", false));
   Config<Float> eatTransformFactorConfig = this.register(
      new NumberConfig<>("EatTransform-Factor", "Factor for the first person eating animation", 0.0F, 1.0F, 1.0F, () -> this.eatTransformConfig.getValue())
   );
   Config<Boolean> limbSwing = this.register(new BooleanConfig("NoLimbSwing", "Allows you to cancel limb swing animations", false));
   Config<Boolean> interpolationConfig = this.register(
      new BooleanConfig("NoInterpolation", "Entities will be rendered at their server positions", false, () -> this.limbSwing.getValue())
   );

   public AnimationsModule() {
      super("Animations", "Allows you to modify vanilla animations", ModuleCategory.RENDER);
   }

   @EventListener
   public void onUpdateHeldItems(UpdateHeldItemsEvent event) {
      if (this.noSwitchConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onSwingSpeed(SwingSpeedEvent event) {
      if (this.swingSpeedConfig.getValue()) {
         event.cancel();
         event.setSwingSpeed(this.swingFactorConfig.getValue());
         event.setSelfOnly(this.selfOnlyConfig.getValue());
      }
   }

   @EventListener
   public void onEatTransformation(EatTransformationEvent event) {
      if (this.eatTransformConfig.getValue()) {
         event.cancel();
         event.setFactor(this.eatTransformFactorConfig.getValue());
      }
   }

   @EventListener
   public void onLimbAnimation(LimbAnimationEvent event) {
      if (this.limbSwing.getValue()) {
         event.cancel();
         event.setSpeed(0.0F);
      }
   }

   @EventListener
   public void onUpdateServerPosition(UpdateServerPositionEvent event) {
      if (this.limbSwing.getValue() && this.interpolationConfig.getValue()) {
         event.getLivingEntity().setPos(event.getX(), event.getY(), event.getZ());
         event.getLivingEntity().setYaw(event.getYaw());
         event.getLivingEntity().setPitch(event.getPitch());
      }
   }

   @EventListener
   public void onRenderSwing(RenderSwingAnimationEvent event) {
      if (this.oldSwingConfig.getValue()) {
         event.cancel();
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null) {
         if (event.getPacket() instanceof BundleS2CPacket packet) {
            List<Packet<?>> packets = new ArrayList<>();

            for (Packet<?> packet1 : packet.getPackets()) {
               if (!(
                  packet1 instanceof EntityAnimationS2CPacket packet2
                     && this.oldSwingConfig.getValue()
                     && packet2.getEntityId() == mc.player.getId()
                     && (packet2.getAnimationId() == 0 || packet2.getAnimationId() == 3)
               )) {
                  packets.add(packet1);
               }
            }

            ((AccessorBundlePacket)packet).setIterable(packets);
         } else if (event.getPacket() instanceof EntityAnimationS2CPacket packet
            && this.oldSwingConfig.getValue()
            && packet.getEntityId() == mc.player.getId()
            && (packet.getAnimationId() == 0 || packet.getAnimationId() == 3)) {
            event.cancel();
         }
      }
   }
}
