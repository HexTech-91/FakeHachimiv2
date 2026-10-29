package net.hachimi.client.impl.event.network;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.util.math.BlockPos;

@Cancelable
public class BreakBlockEvent extends Event {
   private final BlockPos pos;

   public BreakBlockEvent(BlockPos pos) {
      this.pos = pos;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
