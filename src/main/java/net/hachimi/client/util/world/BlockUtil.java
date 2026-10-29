package net.hachimi.client.util.world;

import java.util.ArrayList;
import java.util.List;
import net.hachimi.client.util.Globals;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;

public class BlockUtil implements Globals {
   public static List<BlockEntity> blockEntities() {
      List<BlockEntity> list = new ArrayList<>();

      for (WorldChunk chunk : loadedChunks()) {
         list.addAll(chunk.getBlockEntities().values());
      }

      return list;
   }

   public static List<WorldChunk> loadedChunks() {
      List<WorldChunk> chunks = new ArrayList<>();
      int viewDist = mc.options.getViewDistance().getValue();

      for (int x = -viewDist; x <= viewDist; x++) {
         for (int z = -viewDist; z <= viewDist; z++) {
            WorldChunk chunk = mc.world.getChunkManager().getWorldChunk((int)mc.player.getX() / 16 + x, (int)mc.player.getZ() / 16 + z);
            if (chunk != null) {
               chunks.add(chunk);
            }
         }
      }

      return chunks;
   }

   public static boolean isBlockAccessible(BlockPos pos) {
      return mc.world.isAir(pos) && !mc.world.isAir(pos.add(0, -1, 0)) && mc.world.isAir(pos.add(0, 1, 0)) && mc.world.isAir(pos.add(0, 2, 0));
   }

   public static boolean isBlockLoaded(double x, double z) {
      ChunkManager chunkManager = mc.world.getChunkManager();
      return chunkManager != null ? chunkManager.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z)) : false;
   }
}
