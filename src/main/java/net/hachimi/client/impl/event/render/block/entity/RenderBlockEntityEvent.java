package net.hachimi.client.impl.event.render.block.entity;

import net.hachimi.eventbus.event.Event;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class RenderBlockEntityEvent extends Event {
   private final BlockEntity blockEntity;

   public RenderBlockEntityEvent(BlockEntity blockEntity) {
      this.blockEntity = blockEntity;
   }

   public BlockPos getPos() {
      return this.blockEntity.getPos();
   }

   public BlockEntity getBlockEntity() {
      return this.blockEntity;
   }

   public BlockState getCachedState() {
      return this.blockEntity.getCachedState();
   }
}
