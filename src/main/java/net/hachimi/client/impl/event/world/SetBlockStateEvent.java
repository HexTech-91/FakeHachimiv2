package net.hachimi.client.impl.event.world;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

@Cancelable
public class SetBlockStateEvent extends Event {
   private final int flags;
   private final BlockPos pos;
   private final BlockState state;

   public SetBlockStateEvent(int flags, BlockPos pos, BlockState state) {
      this.flags = flags;
      this.pos = pos;
      this.state = state;
   }

   public int getFlags() {
      return this.flags;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }
}
