package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.LivingEntity;

public class EntityDeathEvent extends Event {
   private final LivingEntity entity;

   public EntityDeathEvent(LivingEntity entity) {
      this.entity = entity;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }
}
