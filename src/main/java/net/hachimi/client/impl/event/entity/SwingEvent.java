package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.util.Hand;

@Cancelable
public class SwingEvent extends Event {
   private final Hand hand;

   public SwingEvent(Hand hand) {
      this.hand = hand;
   }

   public Hand getHand() {
      return this.hand;
   }
}
