package net.hachimi.client.impl.event.entity.player;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;

@Cancelable
public class PushEntityEvent extends Event {
   private final Entity pushed;
   private final Entity pusher;

   public PushEntityEvent(Entity pushed, Entity pusher) {
      this.pushed = pushed;
      this.pusher = pusher;
   }

   public Entity getPushed() {
      return this.pushed;
   }

   public Entity getPusher() {
      return this.pusher;
   }
}
