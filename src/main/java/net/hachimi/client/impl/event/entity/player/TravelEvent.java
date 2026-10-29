package net.hachimi.client.impl.event.entity.player;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.math.Vec3d;

@Cancelable
public class TravelEvent extends StageEvent {
   private final Vec3d movementInput;

   public TravelEvent(Vec3d movementInput) {
      this.movementInput = movementInput;
   }

   public Vec3d getMovementInput() {
      return this.movementInput;
   }
}
