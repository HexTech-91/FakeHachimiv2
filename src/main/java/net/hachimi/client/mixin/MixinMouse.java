package net.hachimi.client.mixin;

import net.hachimi.client.impl.event.MouseClickEvent;
import net.hachimi.client.impl.event.MouseUpdateEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MixinMouse {
   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      MouseClickEvent mouseClickEvent = new MouseClickEvent(button, action);
      EventBus.INSTANCE.dispatch(mouseClickEvent);
      if (mouseClickEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Redirect(
      method = {"updateMouse"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
      )
   )
   public void onUpdate(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
      MouseUpdateEvent mouseUpdateEvent = new MouseUpdateEvent(cursorDeltaX, cursorDeltaY);
      EventBus.INSTANCE.dispatch(mouseUpdateEvent);
      if (!mouseUpdateEvent.isCanceled()) {
         instance.changeLookDirection(cursorDeltaX, cursorDeltaY);
      }
   }
}
