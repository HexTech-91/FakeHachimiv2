package net.hachimi.client.impl.event;

import net.hachimi.eventbus.event.Event;
import net.minecraft.client.gui.screen.Screen;

public class ScreenOpenEvent extends Event {
   private final Screen screen;

   public ScreenOpenEvent(Screen screen) {
      this.screen = screen;
   }

   public Screen getScreen() {
      return this.screen;
   }
}
