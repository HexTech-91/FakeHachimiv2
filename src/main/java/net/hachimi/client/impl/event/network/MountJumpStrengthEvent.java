package net.hachimi.client.impl.event.network;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

@Cancelable
public class MountJumpStrengthEvent extends Event {
   private float jumpStrength;

   public float getJumpStrength() {
      return this.jumpStrength;
   }

   public void setJumpStrength(float jumpStrength) {
      this.jumpStrength = jumpStrength;
   }
}
