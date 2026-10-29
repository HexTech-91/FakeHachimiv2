package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.block.BlockState;

@Cancelable
public class SlowMovementEvent extends Event {
   private final BlockState state;
   private float multiplier = 1.0F;

   public SlowMovementEvent(BlockState state) {
      this.state = state;
   }

   public BlockState getState() {
      return this.state;
   }

   public float getMultiplier() {
      return this.multiplier;
   }

   public void setMultiplier(float multiplier) {
      this.multiplier = multiplier;
   }
}
