package net.hachimi.client.impl.event;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class MouseUpdateEvent extends Event {
   private final double cursorDeltaX;
   private final double cursorDeltaY;

   public MouseUpdateEvent(double cursorDeltaX, double cursorDeltaY) {
      this.cursorDeltaX = cursorDeltaX;
      this.cursorDeltaY = cursorDeltaY;
   }

   public double getCursorDeltaX() {
      return this.cursorDeltaX;
   }

   public double getCursorDeltaY() {
      return this.cursorDeltaY;
   }
}
