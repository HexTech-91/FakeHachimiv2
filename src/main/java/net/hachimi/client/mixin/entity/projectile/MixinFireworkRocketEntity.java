package net.hachimi.client.mixin.entity.projectile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.hachimi.client.impl.event.entity.FireworkTickEvent;
import net.hachimi.client.impl.event.entity.projectile.FireworkVelocityEvent;
import net.hachimi.client.impl.event.entity.projectile.RemoveFireworkEvent;
import net.hachimi.client.impl.module.movement.FakeFlyModule;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FireworkRocketEntity.class})
public class MixinFireworkRocketEntity implements Globals {
   @Shadow
   private int life;
   @Shadow
   private int lifeTime;

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTick(CallbackInfo ci) {
      FireworkTickEvent event = new FireworkTickEvent((FireworkRocketEntity)(Object)this);
      EventBus.INSTANCE.dispatch(event);
      if (event.isCanceled()) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
      )
   )
   private void hookSetVelocity(LivingEntity entity, Vec3d velocity) {
      if (entity instanceof ClientPlayerEntity) {
         FireworkVelocityEvent fireworkVelocityEvent = new FireworkVelocityEvent();
         EventBus.INSTANCE.dispatch(fireworkVelocityEvent);
         if (!fireworkVelocityEvent.isCanceled()) {
            entity.setVelocity(velocity);
         }
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/projectile/FireworkRocketEntity;updateRotation()V",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void hookTickPre(CallbackInfo ci) {
      if (this.life > this.lifeTime) {
         FireworkRocketEntity rocketEntity = (FireworkRocketEntity)(Object)this;
         RemoveFireworkEvent removeFireworkEvent = new RemoveFireworkEvent(rocketEntity);
         EventBus.INSTANCE.dispatch(removeFireworkEvent);
         if (removeFireworkEvent.isCanceled()) {
            ci.cancel();
            if (this.life == 0 && !rocketEntity.isSilent()) {
               mc.world
                  .playSound(
                     null,
                     rocketEntity.getX(),
                     rocketEntity.getY(),
                     rocketEntity.getZ(),
                     SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,
                     SoundCategory.AMBIENT,
                     3.0F,
                     1.0F
                  );
            }

            this.life++;
            if (mc.world.isClient && this.life % 2 < 2) {
               mc.world
                  .addParticle(
                     ParticleTypes.FIREWORK,
                     rocketEntity.getX(),
                     rocketEntity.getY(),
                     rocketEntity.getZ(),
                     mc.world.random.nextGaussian() * 0.05,
                     -rocketEntity.getVelocity().y * 0.5,
                     mc.world.random.nextGaussian() * 0.05
                  );
            }
         }
      }
   }

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
      )}
   )
   private Vec3d hookGetRotationVector(LivingEntity instance, Operation<Vec3d> original) {
      FakeFlyModule lsmf = FakeFlyModule.getInstance();
      return instance == mc.player && lsmf != null && lsmf.isEnabled() && lsmf.isGrimDurabilityMode() && lsmf.isControlEnabled()
         ? instance.getRotationVector(lsmf.getModulePitch(), lsmf.getModuleYaw())
         : (Vec3d)original.call(new Object[]{instance});
   }
}
