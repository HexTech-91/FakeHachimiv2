package net.hachimi.client.impl.event.world;

import net.hachimi.eventbus.event.Event;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public class UnloadChunkBlocksEvent extends Event {
   private final WorldChunk chunk;

   public UnloadChunkBlocksEvent(WorldChunk chunk) {
      this.chunk = chunk;
   }

   public boolean contains(BlockPos blockPos) {
      int chunkX = blockPos.getX() >> 4;
      int chunkZ = blockPos.getZ() >> 4;
      return chunkX == this.chunk.getPos().x && chunkZ == this.chunk.getPos().z;
   }
}
