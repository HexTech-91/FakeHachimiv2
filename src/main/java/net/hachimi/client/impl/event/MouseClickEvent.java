package net.hachimi.client.impl.event;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class MouseClickEvent extends Event {
   private final int button;
   private final int action;

   public MouseClickEvent(int button, int action) {
      this.button = button;
      this.action = action;
   }

   public int getButton() {
      return this.button;
   }

   public int getAction() {
      return this.action;
   }
}
