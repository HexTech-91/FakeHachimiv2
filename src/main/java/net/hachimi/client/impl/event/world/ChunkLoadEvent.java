package net.hachimi.client.impl.event.world;

import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.util.math.ChunkPos;

public class ChunkLoadEvent extends StageEvent {
   private final ChunkPos pos;

   public ChunkLoadEvent(ChunkPos pos) {
      this.pos = pos;
   }

   public ChunkPos getPos() {
      return this.pos;
   }
}
