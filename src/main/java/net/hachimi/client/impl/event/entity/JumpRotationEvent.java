package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public final class JumpRotationEvent extends Event {
   private float yaw;

   public JumpRotationEvent(float yaw) {
      this.yaw = yaw;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }
}
