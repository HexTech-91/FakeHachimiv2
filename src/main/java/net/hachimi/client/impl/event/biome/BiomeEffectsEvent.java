package net.hachimi.client.impl.event.biome;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.world.biome.BiomeParticleConfig;

@Cancelable
public class BiomeEffectsEvent extends Event {
   private BiomeParticleConfig particleConfig;

   public BiomeParticleConfig getParticleConfig() {
      return this.particleConfig;
   }

   public void setParticleConfig(BiomeParticleConfig particleConfig) {
      this.particleConfig = particleConfig;
   }
}
