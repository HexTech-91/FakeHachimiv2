package net.hachimi.client.mixin;

import net.hachimi.client.impl.event.keyboard.KeyboardTickEvent;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public class MixinKeyboardInput {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookTick$Pre(boolean slowDown, float slowDownFactor, CallbackInfo info) {
      KeyboardTickEvent event = new KeyboardTickEvent((Input)(Object)this);
      event.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(event);
      if (event.isCanceled()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z",
         shift = Shift.BEFORE
      )},
      cancellable = true
   )
   private void hookTick$Post(boolean slowDown, float f, CallbackInfo ci) {
      KeyboardTickEvent keyboardTickEvent = new KeyboardTickEvent((Input)(Object)this);
      keyboardTickEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(keyboardTickEvent);
      if (keyboardTickEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
