package net.hachimi.client.mixin.gui.screen.slot;

import net.hachimi.client.impl.event.gui.screen.slot.ShulkerNestedEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.ShulkerBoxSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ShulkerBoxSlot.class})
public class MixinShulkerBoxSlot {
   @Inject(
      method = {"canInsert"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      ShulkerNestedEvent shulkerNestedEvent = new ShulkerNestedEvent();
      EventBus.INSTANCE.dispatch(shulkerNestedEvent);
      if (shulkerNestedEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(true);
      }
   }
}
