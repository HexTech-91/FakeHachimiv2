package net.hachimi.client.impl.event.gui.hud;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class PlayerListColumnsEvent extends Event {
   private int tabHeight;

   public void setTabHeight(int tabHeight) {
      this.tabHeight = tabHeight;
   }

   public int getTabHeight() {
      return this.tabHeight;
   }
}
