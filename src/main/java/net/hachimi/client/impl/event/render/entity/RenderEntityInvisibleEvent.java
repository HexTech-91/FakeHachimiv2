package net.hachimi.client.impl.event.render.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.LivingEntity;

@Cancelable
public class RenderEntityInvisibleEvent extends Event {
   private final LivingEntity entity;

   public RenderEntityInvisibleEvent(LivingEntity entity) {
      this.entity = entity;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }
}
