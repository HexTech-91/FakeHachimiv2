package net.hachimi.client.impl.event.world;

import net.hachimi.eventbus.event.Event;
import net.minecraft.world.chunk.Chunk;

public class LoadChunkEvent extends Event {
   private final Chunk chunk;

   public LoadChunkEvent(Chunk chunk) {
      this.chunk = chunk;
   }

   public Chunk getChunk() {
      return this.chunk;
   }
}
