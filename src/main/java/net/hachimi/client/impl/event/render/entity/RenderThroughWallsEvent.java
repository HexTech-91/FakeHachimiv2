package net.hachimi.client.impl.event.render.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.LivingEntity;

@Cancelable
public class RenderThroughWallsEvent extends Event {
   private LivingEntity entity;

   public RenderThroughWallsEvent(LivingEntity entity) {
      this.entity = entity;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }
}
