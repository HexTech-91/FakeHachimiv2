package net.hachimi.client.util.player;

import net.hachimi.client.util.Globals;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtil implements Globals {
   public static float[] getRotationsTo(Vec3d src, Vec3d dest) {
      float yaw = (float)(Math.toDegrees(Math.atan2(dest.subtract(src).z, dest.subtract(src).x)) - 90.0);
      float pitch = (float)Math.toDegrees(-Math.atan2(dest.subtract(src).y, Math.hypot(dest.subtract(src).x, dest.subtract(src).z)));
      return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
   }

   public static float[] smooth(float[] target, float[] previous, float rotationSpeed) {
      float speed = (1.0F - MathHelper.clamp(rotationSpeed / 100.0F, 0.1F, 0.9F)) * 10.0F;
      float[] rotations = new float[]{
         previous[0] + (float)(-getAngleDifference(previous[0], target[0]) / speed), previous[1] + -(previous[1] - target[1]) / speed
      };
      rotations[1] = MathHelper.clamp(rotations[1], -90.0F, 90.0F);
      return rotations;
   }

   public static double getAngleDifference(float client, float yaw) {
      return ((client - yaw) % 360.0 + 540.0) % 360.0 - 180.0;
   }

   public static double getAnglePitchDifference(float client, float pitch) {
      return ((client - pitch) % 180.0 + 270.0) % 180.0 - 90.0;
   }

   public static Vec3d getRotationVector(float pitch, float yaw) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }
}
