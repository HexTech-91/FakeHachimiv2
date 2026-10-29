package net.hachimi.client.mixin.biome;

import net.hachimi.client.impl.event.biome.BiomeColorEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.world.biome.FoliageColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FoliageColors.class})
public class MixinFoliageColors {
   @Inject(
      method = {"getDefaultColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookGetDefaultColor(CallbackInfoReturnable<Integer> cir) {
      BiomeColorEvent.Foliage foliageEvent = new BiomeColorEvent.Foliage();
      EventBus.INSTANCE.dispatch(foliageEvent);
      if (foliageEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(foliageEvent.getColor());
      }
   }
}
