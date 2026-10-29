package net.hachimi.client.impl.event.world;

import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;

public class AddEntityEvent extends Event {
   private final Entity entity;

   public AddEntityEvent(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
