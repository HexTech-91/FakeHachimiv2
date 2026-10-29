package net.hachimi.client.util.world;

import net.hachimi.client.util.Globals;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class DimensionUtil implements Globals {
   public static int getDimension() {
      RegistryKey<World> world = mc.world.getRegistryKey();
      if (world == World.OVERWORLD) {
         return 1;
      } else if (world == World.NETHER) {
         return 0;
      } else {
         return world == World.END ? -1 : 1;
      }
   }
}
