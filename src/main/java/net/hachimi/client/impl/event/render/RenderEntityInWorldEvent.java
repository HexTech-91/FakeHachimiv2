package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;

@Cancelable
public class RenderEntityInWorldEvent extends Event {
   private final Entity entityType;

   public RenderEntityInWorldEvent(Entity entityType) {
      this.entityType = entityType;
   }

   public Entity getEntity() {
      return this.entityType;
   }
}
