package net.hachimi.client.impl.event.network;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class ReachEvent extends Event {
   private double reach;

   public double getReach() {
      return this.reach;
   }

   public void setReach(double reach) {
      this.reach = reach;
   }

   @Cancelable
   public static class Block extends ReachEvent {
   }

   @Cancelable
   public static class Entity extends ReachEvent {
   }
}
