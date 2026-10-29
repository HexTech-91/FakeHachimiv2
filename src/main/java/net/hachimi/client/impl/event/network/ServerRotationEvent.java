package net.hachimi.client.impl.event.network;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class ServerRotationEvent extends Event {
   private float yaw = Float.NaN;
   private float pitch = Float.NaN;

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }
}
