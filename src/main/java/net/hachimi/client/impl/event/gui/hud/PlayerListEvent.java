package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class PlayerListEvent extends Event {
   private int size;

   public int getSize() {
      return this.size;
   }

   public void setSize(int size) {
      this.size = size;
   }
}
