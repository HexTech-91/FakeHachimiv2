package net.hachimi.client.impl.event;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;

@Cancelable
public class EntityOutlineEvent extends Event {
   private final Entity entity;

   public EntityOutlineEvent(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
