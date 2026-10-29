package net.hachimi.client.mixin.biome;

import java.util.Optional;
import net.hachimi.client.impl.event.biome.BiomeEffectsEvent;
import net.hachimi.client.impl.event.world.SkyboxEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.BiomeParticleConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BiomeEffects.class})
public class MixinBiomeEffects {
   @Inject(
      method = {"getSkyColor"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetSkyColor(CallbackInfoReturnable<Integer> cir) {
      SkyboxEvent.Sky skyboxEvent = new SkyboxEvent.Sky();
      EventBus.INSTANCE.dispatch(skyboxEvent);
      if (skyboxEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(skyboxEvent.getRGB());
      }
   }

   @Inject(
      method = {"getParticleConfig"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetParticleConfig(CallbackInfoReturnable<Optional<BiomeParticleConfig>> cir) {
      BiomeEffectsEvent biomeEffectsEvent = new BiomeEffectsEvent();
      EventBus.INSTANCE.dispatch(biomeEffectsEvent);
      if (biomeEffectsEvent.isCanceled()) {
         cir.cancel();
         cir.setReturnValue(Optional.ofNullable(biomeEffectsEvent.getParticleConfig()));
      }
   }
}
