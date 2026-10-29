package net.hachimi.client.impl.event;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class FramerateLimitEvent extends Event {
   private int framerateLimit;

   public int getFramerateLimit() {
      return this.framerateLimit;
   }

   public void setFramerateLimit(int framerateLimit) {
      this.framerateLimit = framerateLimit;
   }
}
