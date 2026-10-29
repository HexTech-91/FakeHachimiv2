package net.hachimi.client.mixin.entity;

import net.hachimi.client.impl.event.entity.ItemTickEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemEntity.class})
public class MixinItemEntity {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTick(CallbackInfo ci) {
      ItemTickEvent itemTickEvent = new ItemTickEvent();
      EventBus.INSTANCE.dispatch(itemTickEvent);
      if (itemTickEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
