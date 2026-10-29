package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.projectile.FireworkRocketEntity;

@Cancelable
public class FireworkTickEvent extends Event {
   private final FireworkRocketEntity fireworkRocketEntity;

   public FireworkTickEvent(FireworkRocketEntity fireworkRocketEntity) {
      this.fireworkRocketEntity = fireworkRocketEntity;
   }

   public FireworkRocketEntity getFireworkRocketEntity() {
      return this.fireworkRocketEntity;
   }
}
