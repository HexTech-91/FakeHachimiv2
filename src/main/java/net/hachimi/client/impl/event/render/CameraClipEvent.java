package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class CameraClipEvent extends Event {
   private float distance;

   public CameraClipEvent(float distance) {
      this.distance = distance;
   }

   public float getDistance() {
      return this.distance;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }
}
