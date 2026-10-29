package net.hachimi.client.impl.event.render.item;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class EatTransformationEvent extends Event {
   private float factor;

   public void setFactor(float factor) {
      this.factor = factor;
   }

   public float getFactor() {
      return this.factor;
   }
}
