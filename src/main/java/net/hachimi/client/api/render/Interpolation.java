package net.hachimi.client.api.render;

import net.hachimi.client.util.Globals;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Interpolation implements Globals {
   public static Vec3d getRenderPosition(Vec3d pos, Vec3d lastPos, float tickDelta) {
      return new Vec3d(
         pos.x - MathHelper.lerp((double)tickDelta, lastPos.x, pos.x),
         pos.y - MathHelper.lerp((double)tickDelta, lastPos.y, pos.y),
         pos.z - MathHelper.lerp((double)tickDelta, lastPos.z, pos.z)
      );
   }

   public static Vec3d getRenderPosition(Entity entity, float tickDelta) {
      return new Vec3d(
         entity.getX() - MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX()),
         entity.getY() - MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY()),
         entity.getZ() - MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ())
      );
   }

   public static Vec3d getInterpolatedPosition(Entity entity, float tickDelta) {
      return new Vec3d(
         entity.prevX + (entity.getX() - entity.prevX) * tickDelta,
         entity.prevY + (entity.getY() - entity.prevY) * tickDelta,
         entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta
      );
   }

   public static float interpolateFloat(float prev, float value, float factor) {
      return prev + (value - prev) * factor;
   }

   public static double interpolateDouble(double prev, double value, double factor) {
      return prev + (value - prev) * factor;
   }

   public static Box getInterpolatedBox(Box prevBox, Box box) {
      double delta = mc.isPaused() ? 1.0 : mc.getRenderTickCounter().getTickDelta(true);
      return new Box(
         interpolateDouble(prevBox.minX, box.minX, delta),
         interpolateDouble(prevBox.minY, box.minY, delta),
         interpolateDouble(prevBox.minZ, box.minZ, delta),
         interpolateDouble(prevBox.maxX, box.maxX, delta),
         interpolateDouble(prevBox.maxY, box.maxY, delta),
         interpolateDouble(prevBox.maxZ, box.maxZ, delta)
      );
   }

   public static Box getInterpolatedEntityBox(Entity entity) {
      Box box = entity.getBoundingBox();
      Box prevBox = entity.getBoundingBox().offset(entity.prevX - entity.getX(), entity.prevY - entity.getY(), entity.prevZ - entity.getZ());
      return getInterpolatedBox(prevBox, box);
   }
}
