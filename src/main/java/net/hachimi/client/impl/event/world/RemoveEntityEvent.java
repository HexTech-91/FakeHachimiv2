package net.hachimi.client.impl.event.world;

import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;

public class RemoveEntityEvent extends Event implements Globals {
   private final Entity entity;
   private final RemovalReason removalReason;

   public RemoveEntityEvent(Entity entity, RemovalReason removalReason) {
      this.entity = entity;
      this.removalReason = removalReason;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public RemovalReason getRemovalReason() {
      return this.removalReason;
   }
}
