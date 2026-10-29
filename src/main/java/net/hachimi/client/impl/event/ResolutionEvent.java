package net.hachimi.client.impl.event;

import net.hachimi.eventbus.event.Event;
import net.minecraft.client.util.Window;

public class ResolutionEvent extends Event {
   private final Window window;

   public ResolutionEvent(Window window) {
      this.window = window;
   }

   public Window getWindow() {
      return this.window;
   }
}
