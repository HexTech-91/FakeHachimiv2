package net.hachimi.client.impl.event.entity.player;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class LedgeClipEvent extends Event {
   private boolean clip;

   public void setClipped(boolean clip) {
      this.clip = clip;
   }

   public boolean isClipped() {
      return this.clip;
   }
}
