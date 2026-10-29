package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.LivingEntity;

@Cancelable
public class EntityTravelEvent extends Event {
   private final LivingEntity entity;
   private final boolean pre;

   public EntityTravelEvent(LivingEntity entity, boolean pre) {
      this.entity = entity;
      this.pre = pre;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public boolean isPre() {
      return this.pre;
   }
}
