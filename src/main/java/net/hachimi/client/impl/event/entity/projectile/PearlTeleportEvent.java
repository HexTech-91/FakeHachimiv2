package net.hachimi.client.impl.event.entity.projectile;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.world.TeleportTarget;

@Cancelable
public class PearlTeleportEvent extends Event {
   private final TeleportTarget teleportTarget;

   public PearlTeleportEvent(TeleportTarget teleportTarget) {
      this.teleportTarget = teleportTarget;
   }

   public TeleportTarget getTeleportTarget() {
      return this.teleportTarget;
   }
}
