package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public final class LimbAnimationEvent extends Event {
   float speed;

   public void setSpeed(float speed) {
      this.speed = speed;
   }

   public float getSpeed() {
      return this.speed;
   }
}
