package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class TickCounterEvent extends Event {
   private float ticks;

   public float getTicks() {
      return this.ticks;
   }

   public void setTicks(float ticks) {
      this.ticks = ticks;
   }
}
