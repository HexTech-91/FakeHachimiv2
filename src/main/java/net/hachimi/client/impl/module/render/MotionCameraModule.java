package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.camera.CameraPositionEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.option.Perspective;

public class MotionCameraModule extends ToggleModule {
   Config<Boolean> noFirstPersonConfig = this.register(new BooleanConfig("NoFirstPerson", "Disables the effect in first person view", true));
   Config<Float> speedConfig = this.register(new NumberConfig<>("Speed", "How fast the camera follows the player (0 = no lag, 1 = instant)", 0.0F, 0.3F, 1.0F));
   Config<Float> firstPersonSpeedConfig = this.register(
      new NumberConfig<>("FirstPersonSpeed", "Speed when in first person", 0.0F, 0.6F, 1.0F, () -> !this.noFirstPersonConfig.getValue())
   );
   private double fakeX;
   private double fakeY;
   private double fakeZ;
   private double prevFakeX;
   private double prevFakeY;
   private double prevFakeZ;
   private boolean initialized = false;

   public MotionCameraModule() {
      super("MotionCamera", "Camera smoothly lags behind player movement", ModuleCategory.RENDER);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.fakeX = mc.player.getX();
         this.fakeY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
         this.fakeZ = mc.player.getZ();
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         this.initialized = true;
      }
   }

   @Override
   public void onDisable() {
      this.initialized = false;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null) {
         this.prevFakeX = this.fakeX;
         this.prevFakeY = this.fakeY;
         this.prevFakeZ = this.fakeZ;
         boolean isFirstPerson = mc.options.getPerspective() == Perspective.FIRST_PERSON;
         float speed = isFirstPerson && !this.noFirstPersonConfig.getValue() ? this.firstPersonSpeedConfig.getValue() : this.speedConfig.getValue();
         double targetX = mc.player.getX();
         double targetY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
         double targetZ = mc.player.getZ();
         this.fakeX = this.animate(this.fakeX, targetX, speed);
         this.fakeY = this.animate(this.fakeY, targetY, speed);
         this.fakeZ = this.animate(this.fakeZ, targetZ, speed);
      }
   }

   @EventListener
   public void onCameraPosition(CameraPositionEvent event) {
      if (this.initialized) {
         if (!this.noFirstPersonConfig.getValue() || mc.options.getPerspective() != Perspective.FIRST_PERSON) {
            float delta = event.getTickDelta();
            event.setX(this.lerp(this.prevFakeX, this.fakeX, delta));
            event.setY(this.lerp(this.prevFakeY, this.fakeY, delta));
            event.setZ(this.lerp(this.prevFakeZ, this.fakeZ, delta));
         }
      }
   }

   private double animate(double current, double target, double speed) {
      if (speed >= 1.0) {
         return target;
      } else if (speed <= 0.0) {
         return current;
      } else {
         double diff = target - current;
         return Math.abs(diff) <= 0.001 ? target : current + diff * speed;
      }
   }

   private double lerp(double a, double b, float t) {
      return a + (b - a) * t;
   }
}
