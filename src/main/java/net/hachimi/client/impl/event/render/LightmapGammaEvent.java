package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class LightmapGammaEvent extends Event {
   private int gamma;

   public LightmapGammaEvent(int gamma) {
      this.gamma = gamma;
   }

   public int getGamma() {
      return this.gamma;
   }

   public void setGamma(int gamma) {
      this.gamma = gamma;
   }
}
