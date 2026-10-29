package net.hachimi.client.mixin.entity;

import net.hachimi.client.impl.event.entity.LimbAnimationEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LimbAnimator.class})
public final class MixinLimbAnimator {
   @Inject(
      method = {"getSpeed()F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetSpeed(CallbackInfoReturnable<Float> cir) {
      LimbAnimationEvent limbAnimationEvent = new LimbAnimationEvent();
      EventBus.INSTANCE.dispatch(limbAnimationEvent);
      if (limbAnimationEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(limbAnimationEvent.getSpeed());
      }
   }

   @Inject(
      method = {"getSpeed(F)F"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetSpeed(float tickDelta, CallbackInfoReturnable<Float> cir) {
      LimbAnimationEvent limbAnimationEvent = new LimbAnimationEvent();
      EventBus.INSTANCE.dispatch(limbAnimationEvent);
      if (limbAnimationEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(limbAnimationEvent.getSpeed());
      }
   }
}
