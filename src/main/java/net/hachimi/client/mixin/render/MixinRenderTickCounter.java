package net.hachimi.client.mixin.render;

import net.hachimi.client.impl.event.render.TickCounterEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.render.RenderTickCounter.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Dynamic.class})
public class MixinRenderTickCounter {
   @Shadow
   private float lastFrameDuration;
   @Shadow
   private float tickDelta;
   @Shadow
   private long prevTimeMillis;
   @Final
   @Shadow
   private float tickTime;

   @Inject(
      method = {"beginRenderTick(J)I"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
      TickCounterEvent tickCounterEvent = new TickCounterEvent();
      EventBus.INSTANCE.dispatch(tickCounterEvent);
      if (tickCounterEvent.isCanceled()) {
         this.lastFrameDuration = (float)(timeMillis - this.prevTimeMillis) / this.tickTime * tickCounterEvent.getTicks();
         this.prevTimeMillis = timeMillis;
         this.tickDelta = this.tickDelta + this.lastFrameDuration;
         int i = (int)this.tickDelta;
         this.tickDelta -= i;
         cir.setReturnValue(i);
      }
   }
}
