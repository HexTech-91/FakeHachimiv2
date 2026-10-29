package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class FovEvent extends Event {
   private double fov;

   public double getFov() {
      return this.fov;
   }

   public void setFov(double fov) {
      this.fov = fov;
   }
}
