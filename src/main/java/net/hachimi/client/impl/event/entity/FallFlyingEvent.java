package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class FallFlyingEvent extends Event {
   private boolean isFallFlying;

   public FallFlyingEvent(boolean isFallFlying) {
      this.isFallFlying = isFallFlying;
   }

   public void setFallFlying(boolean fallFlying) {
      this.isFallFlying = fallFlying;
   }

   public boolean isFallFlying() {
      return this.isFallFlying;
   }
}
