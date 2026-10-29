package net.hachimi.client.mixin.gui.screen;

import net.hachimi.client.impl.event.gui.screen.ConnectScreenEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ConnectScreen.class})
public class MixinConnectScreen {
   @Inject(
      method = {"connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V"},
      at = {@At("HEAD")}
   )
   private void onConnect(MinecraftClient client, ServerAddress address, ServerInfo info, CookieStorage cookieStorage, CallbackInfo ci) {
      ConnectScreenEvent connectScreenEvent = new ConnectScreenEvent(address, info);
      EventBus.INSTANCE.dispatch(connectScreenEvent);
   }
}
