package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.LivingEntity;

@Cancelable
public class ElytraUpdateEvent extends Event {
   private final LivingEntity entity;
   private final boolean fallFlying;

   public ElytraUpdateEvent(LivingEntity entity, boolean fallFlying) {
      this.entity = entity;
      this.fallFlying = fallFlying;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public boolean isFallFlying() {
      return this.fallFlying;
   }
}
