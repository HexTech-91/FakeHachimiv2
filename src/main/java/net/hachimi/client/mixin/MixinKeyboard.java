package net.hachimi.client.mixin;

import net.hachimi.client.impl.event.keyboard.KeyboardInputEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Keyboard.class})
public class MixinKeyboard {
   @Shadow
   @Final
   private MinecraftClient client;

   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookOnKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
      if (this.client.getWindow().getHandle() == window) {
         KeyboardInputEvent keyboardInputEvent = new KeyboardInputEvent(key, action);
         EventBus.INSTANCE.dispatch(keyboardInputEvent);
         if (keyboardInputEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }
}
