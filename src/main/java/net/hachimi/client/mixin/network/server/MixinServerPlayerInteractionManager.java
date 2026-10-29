package net.hachimi.client.mixin.network.server;

import net.hachimi.client.impl.event.network.server.FinishMiningServerEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayerInteractionManager.class})
public class MixinServerPlayerInteractionManager {
   @Inject(
      method = {"finishMining"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookFinishMining(BlockPos pos, int sequence, String reason, CallbackInfo ci) {
      FinishMiningServerEvent finishMiningServerEvent = new FinishMiningServerEvent();
      EventBus.INSTANCE.dispatch(finishMiningServerEvent);
      if (finishMiningServerEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
