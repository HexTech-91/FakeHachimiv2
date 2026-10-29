package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;
import net.minecraft.world.event.GameEvent;

public class EntityGameEvent extends Event {
   private final GameEvent gameEvent;
   private final Entity entity;

   public EntityGameEvent(GameEvent gameEvent, Entity entity) {
      this.gameEvent = gameEvent;
      this.entity = entity;
   }

   public GameEvent getGameEvent() {
      return this.gameEvent;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
