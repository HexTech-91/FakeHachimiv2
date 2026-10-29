package net.hachimi.client.impl.event.keyboard;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.client.input.Input;

@Cancelable
public class KeyboardTickEvent extends StageEvent {
   private final Input input;

   public KeyboardTickEvent(Input input) {
      this.input = input;
   }

   public Input getInput() {
      return this.input;
   }
}
