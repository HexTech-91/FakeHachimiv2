package net.hachimi.client.mixin.block;

import net.hachimi.client.impl.event.block.BlockSlipperinessEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Block.class})
public class MixinBlock {
   @Inject(
      method = {"getSlipperiness"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void hookGetSlipperiness(CallbackInfoReturnable<Float> cir) {
      BlockSlipperinessEvent blockSlipperinessEvent = new BlockSlipperinessEvent((Block)(Object)this, cir.getReturnValueF());
      EventBus.INSTANCE.dispatch(blockSlipperinessEvent);
      if (blockSlipperinessEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(blockSlipperinessEvent.getSlipperiness());
      }
   }
}
