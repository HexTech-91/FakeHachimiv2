package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public final class SwingSpeedEvent extends Event {
   int swingSpeed;
   boolean selfOnly;

   public void setSwingSpeed(int swingSpeed) {
      this.swingSpeed = swingSpeed;
   }

   public int getSwingSpeed() {
      return this.swingSpeed;
   }

   public void setSelfOnly(boolean selfOnly) {
      this.selfOnly = selfOnly;
   }

   public boolean getSelfOnly() {
      return this.selfOnly;
   }
}
