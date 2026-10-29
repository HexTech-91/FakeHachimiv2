package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.LivingEntity;

@Cancelable
public class StepEvent extends Event {
   private final LivingEntity entity;
   private float stepHeight;

   public StepEvent(LivingEntity entity, float stepHeight) {
      this.entity = entity;
      this.stepHeight = stepHeight;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }

   public void setStepHeight(float stepHeight) {
      this.stepHeight = stepHeight;
   }

   public float getStepHeight() {
      return this.stepHeight;
   }
}
