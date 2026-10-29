package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

@Cancelable
public class VelocityMultiplierEvent extends Event {
   private final BlockState state;

   public VelocityMultiplierEvent(BlockState state) {
      this.state = state;
   }

   public Block getBlock() {
      return this.state.getBlock();
   }
}
