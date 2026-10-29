package net.hachimi.client.impl.event.particle;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

@Cancelable
public class ParticleEvent extends Event {
   private final ParticleEffect particle;

   public ParticleEvent(ParticleEffect particle) {
      this.particle = particle;
   }

   public ParticleEffect getParticle() {
      return this.particle;
   }

   public ParticleType<?> getParticleType() {
      return this.particle.getType();
   }

   @Cancelable
   public static class Emitter extends ParticleEvent {
      public Emitter(ParticleEffect particle) {
         super(particle);
      }
   }
}
