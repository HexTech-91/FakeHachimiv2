package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.LivingEntity;

public class PlayerDamageEvent extends Event {
   private final LivingEntity damaged;

   public PlayerDamageEvent(LivingEntity damaged) {
      this.damaged = damaged;
   }

   public LivingEntity getDamaged() {
      return this.damaged;
   }
}
