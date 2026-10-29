package net.hachimi.client.impl.event.render.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;

@Cancelable
public class ElytraTransformEvent extends Event {
   private final Entity entity;

   public ElytraTransformEvent(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
