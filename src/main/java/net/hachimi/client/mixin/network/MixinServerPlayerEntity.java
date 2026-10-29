package net.hachimi.client.mixin.network;

import net.hachimi.client.impl.event.world.LoadWorldEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayerEntity.class})
public class MixinServerPlayerEntity {
   @Inject(
      method = {"worldChanged"},
      at = {@At("HEAD")}
   )
   private void hookMoveToWorld(ServerWorld origin, CallbackInfo ci) {
      LoadWorldEvent loadWorldEvent = new LoadWorldEvent();
      EventBus.INSTANCE.dispatch(loadWorldEvent);
   }
}
