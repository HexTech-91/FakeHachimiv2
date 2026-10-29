package net.hachimi.client.mixin.entity.passive;

import net.hachimi.client.impl.event.entity.passive.EntitySteerEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.entity.passive.StriderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({StriderEntity.class})
public class MixinStriderEntity {
   @Inject(
      method = {"isSaddled"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookIsSaddled(CallbackInfoReturnable<Boolean> cir) {
      EntitySteerEvent entitySteerEvent = new EntitySteerEvent();
      EventBus.INSTANCE.dispatch(entitySteerEvent);
      if (entitySteerEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(true);
      }
   }
}
