package net.hachimi.client.impl.event.network;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class TickMovementEvent extends Event {
   private int iterations;

   public int getIterations() {
      return this.iterations;
   }

   public void setIterations(int iterations) {
      this.iterations = iterations;
   }
}
