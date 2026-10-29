package net.hachimi.client.mixin.item;

import net.hachimi.client.impl.event.network.ItemDesyncEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemUsageContext.class})
public final class MixinItemUsageContext implements Globals {
   @Inject(
      method = {"getStack"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookGetStack(CallbackInfoReturnable<ItemStack> info) {
      ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
      EventBus.INSTANCE.dispatch(itemDesyncEvent);
      if (mc.player != null && info.getReturnValue().equals(mc.player.getMainHandStack()) && itemDesyncEvent.isCanceled()) {
         info.setReturnValue(itemDesyncEvent.getServerItem());
      }
   }
}
