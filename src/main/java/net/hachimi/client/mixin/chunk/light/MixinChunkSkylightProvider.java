package net.hachimi.client.mixin.chunk.light;

import net.hachimi.client.impl.event.chunk.light.RenderSkylightEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.world.chunk.light.ChunkSkyLightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChunkSkyLightProvider.class})
public class MixinChunkSkylightProvider {
   @Inject(
      method = {"method_51531"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRecalculateLevel(long blockPos, long l, int lightLevel, CallbackInfo ci) {
      RenderSkylightEvent renderSkylightEvent = new RenderSkylightEvent();
      EventBus.INSTANCE.dispatch(renderSkylightEvent);
      if (renderSkylightEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
