package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.mixin.accessor.AccessorFireworkRocketEntity;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FireworkBoostModule extends ToggleModule {
   Config<Double> speedConfig = this.register(new NumberConfig<>("Speed", "The initial speed of the rocket.", 1.0, 1.5, 10.0));
   Config<Boolean> accelerateConfig = this.register(new BooleanConfig("Accelerate", "Whether or not to accelerate the rocket.", false));
   Config<Double> accelSpeedConfig = this.register(
      new NumberConfig<>("AccelSpeed", "The speed to accelerate the rocket at.", 0.1, 0.5, 5.0, () -> this.accelerateConfig.getValue())
   );
   Config<Double> maxSpeedConfig = this.register(
      new NumberConfig<>("MaxSpeed", "The initial speed of the rocket.", 1.0, 3.0, 10.0, () -> this.accelerateConfig.getValue())
   );
   private int tickCounter;
   private double speed;

   public FireworkBoostModule() {
      super("FireworkBoost", "Allows you to change the acceleration of your firework.", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.speed = 0.0;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && !mc.player.isInLava()) {
         if (this.isBoostedByRocket() && mc.player.isFallFlying()) {
            Vec3d vec3d = mc.player.getRotationVector();
            Vec3d vec3d2 = mc.player.getVelocity();
            if (this.accelerateConfig.getValue()) {
               this.speed = this.speed + this.accelSpeedConfig.getValue() * this.tickCounter;
               if (this.speed < this.maxSpeedConfig.getValue()) {
                  this.tickCounter++;
               } else {
                  this.speed = MathHelper.clamp(this.speed, 0.0, this.maxSpeedConfig.getValue());
               }
            } else {
               this.speed = this.speedConfig.getValue();
            }

            mc.player
               .setVelocity(
                  vec3d2.add(
                     vec3d.x * 0.1 + (vec3d.x * this.speed - vec3d2.x) * 0.5,
                     vec3d.y * 0.1 + (vec3d.y * this.speed - vec3d2.y) * 0.5,
                     vec3d.z * 0.1 + (vec3d.z * this.speed - vec3d2.z) * 0.5
                  )
               );
         } else {
            this.speed = 0.0;
         }
      }
   }

   private boolean isBoostedByRocket() {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof FireworkRocketEntity rocket
            && ((AccessorFireworkRocketEntity)rocket).hookWasShotByEntity()
            && ((AccessorFireworkRocketEntity)rocket).hookGetShooter() == mc.player) {
            return true;
         }
      }

      return false;
   }
}
