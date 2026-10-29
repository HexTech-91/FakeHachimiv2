package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class RenderFogEvent extends Event {
   private final float viewDistance;
   private float start;
   private float end;

   public RenderFogEvent(float viewDistance) {
      this.viewDistance = viewDistance;
   }

   public float getViewDistance() {
      return this.viewDistance;
   }

   public float getStart() {
      return this.start;
   }

   public void setStart(float start) {
      this.start = start;
   }

   public float getEnd() {
      return this.end;
   }

   public void setEnd(float end) {
      this.end = end;
   }
}
