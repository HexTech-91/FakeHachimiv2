package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.event.Event;
import net.minecraft.util.math.Box;

public class SetBBEvent extends Event {
   private final Box boundingBox;

   public SetBBEvent(Box boundingBox) {
      this.boundingBox = boundingBox;
   }

   public Box getBoundingBox() {
      return this.boundingBox;
   }
}
