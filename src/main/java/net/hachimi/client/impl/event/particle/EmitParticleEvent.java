package net.hachimi.client.impl.event.particle;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.particle.ParticleEffect;

@Cancelable
public class EmitParticleEvent extends Event {
   private final ParticleEffect effect;
   private int particleCount;
   private int particleTime;

   public EmitParticleEvent(ParticleEffect effect, int particleCount, int particleTime) {
      this.effect = effect;
      this.particleCount = particleCount;
      this.particleTime = particleTime;
   }

   public ParticleEffect getParticleType() {
      return this.effect;
   }

   public int getParticleCount() {
      return this.particleCount;
   }

   public void setParticleCount(int particleCount) {
      this.particleCount = particleCount;
   }

   public int getParticleTime() {
      return this.particleTime;
   }

   public void setParticleTime(int particleTime) {
      this.particleTime = particleTime;
   }
}
