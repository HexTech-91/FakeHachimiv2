package net.hachimi.client.mixin.color.world;

import net.hachimi.client.impl.event.color.world.BiomeColorEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BiomeColors.class})
public class MixinBiomeColors implements Globals {
   @Inject(
      method = {"getColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void hookGetColor(BlockRenderView world, BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> cir) {
      if (mc.world != null) {
         BiomeColorEvent biomeColorEvent = new BiomeColorEvent(resolver);
         EventBus.INSTANCE.dispatch(biomeColorEvent);
         if (biomeColorEvent.isCanceled()) {
            cir.cancel();
            cir.setReturnValue(biomeColorEvent.getRGB());
         }
      }
   }
}
