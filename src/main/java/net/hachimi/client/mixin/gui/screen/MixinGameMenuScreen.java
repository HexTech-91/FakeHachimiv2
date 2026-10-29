package net.hachimi.client.mixin.gui.screen;

import net.hachimi.client.impl.event.gui.screen.MenuDisconnectEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameMenuScreen.class})
public class MixinGameMenuScreen {
   @Inject(
      method = {"disconnect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookDisconnect(CallbackInfo ci) {
      MenuDisconnectEvent menuDisconnectEvent = new MenuDisconnectEvent();
      EventBus.INSTANCE.dispatch(menuDisconnectEvent);
      if (menuDisconnectEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
