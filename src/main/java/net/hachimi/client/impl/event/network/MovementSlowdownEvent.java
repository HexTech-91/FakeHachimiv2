package net.hachimi.client.impl.event.network;

import net.hachimi.eventbus.event.Event;
import net.minecraft.client.input.Input;

public class MovementSlowdownEvent extends Event {
   public final Input input;

   public MovementSlowdownEvent(Input input) {
      this.input = input;
   }

   public Input getInput() {
      return this.input;
   }
}
