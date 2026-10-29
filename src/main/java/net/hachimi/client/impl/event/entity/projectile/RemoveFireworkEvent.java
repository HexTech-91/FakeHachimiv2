package net.hachimi.client.impl.event.entity.projectile;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.projectile.FireworkRocketEntity;

@Cancelable
public class RemoveFireworkEvent extends Event {
   private final FireworkRocketEntity rocketEntity;

   public RemoveFireworkEvent(FireworkRocketEntity rocketEntity) {
      this.rocketEntity = rocketEntity;
   }

   public FireworkRocketEntity getRocketEntity() {
      return this.rocketEntity;
   }
}
