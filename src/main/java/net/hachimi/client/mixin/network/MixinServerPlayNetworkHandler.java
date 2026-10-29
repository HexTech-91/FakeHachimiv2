package net.hachimi.client.mixin.network;

import net.hachimi.client.impl.event.network.ServerTickEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayNetworkHandler.class})
public class MixinServerPlayNetworkHandler {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void hookTick(CallbackInfo ci) {
      ServerTickEvent serverTickEvent = new ServerTickEvent();
      EventBus.INSTANCE.dispatch(serverTickEvent);
   }
}
