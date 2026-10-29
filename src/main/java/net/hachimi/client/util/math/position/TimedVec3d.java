package net.hachimi.client.util.math.position;

import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;

public record TimedVec3d(Vec3d pos, long time) implements Position {
   @Override
   public double getX() {
      return this.pos.getX();
   }

   @Override
   public double getY() {
      return this.pos.getY();
   }

   @Override
   public double getZ() {
      return this.pos.getZ();
   }
}
