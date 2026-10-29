package net.hachimi.client.impl.event;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.client.render.Camera;

@Cancelable
public class PerspectiveEvent extends Event {
   public Camera camera;

   public PerspectiveEvent(Camera camera) {
      this.camera = camera;
   }

   public Camera getCamera() {
      return this.camera;
   }
}
