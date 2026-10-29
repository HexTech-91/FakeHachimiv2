package net.hachimi.client.util.player;

import net.hachimi.client.util.Globals;
import net.minecraft.client.input.Input;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public class MovementUtil implements Globals {
   public static boolean isInputtingMovement() {
      return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
   }

   public static boolean isMovingInput() {
      return mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F;
   }

   public static boolean isMoving() {
      double d = mc.player.getX() - mc.player.lastX;
      double e = mc.player.getY() - mc.player.lastBaseY;
      double f = mc.player.getZ() - mc.player.lastZ;
      return MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4);
   }

   public static void applySneak() {
      float modifier = MathHelper.clamp(
         0.3F + EnchantmentUtil.getLevel(mc.player.getEquippedStack(EquipmentSlot.FEET), Enchantments.SWIFT_SNEAK) * 0.15F, 0.0F, 1.0F
      );
      mc.player.input.movementForward *= modifier;
      mc.player.input.movementSideways *= modifier;
   }

   public static Vec2f applySafewalk(double motionX, double motionZ) {
      double offset = 0.05;
      double moveX = motionX;
      double moveZ = motionZ;
      float fallDist = -mc.player.getStepHeight();
      if (!mc.player.isOnGround()) {
         fallDist = -1.5F;
      }

      while (moveX != 0.0 && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(moveX, fallDist, 0.0))) {
         if (moveX < 0.05 && moveX >= -0.05) {
            moveX = 0.0;
         } else if (moveX > 0.0) {
            moveX -= 0.05;
         } else {
            moveX += 0.05;
         }
      }

      while (moveZ != 0.0 && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0, fallDist, moveZ))) {
         if (moveZ < 0.05 && moveZ >= -0.05) {
            moveZ = 0.0;
         } else if (moveZ > 0.0) {
            moveZ -= 0.05;
         } else {
            moveZ += 0.05;
         }
      }

      while (moveX != 0.0 && moveZ != 0.0 && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(moveX, fallDist, moveZ))) {
         if (moveX < 0.05 && moveX >= -0.05) {
            moveX = 0.0;
         } else if (moveX > 0.0) {
            moveX -= 0.05;
         } else {
            moveX += 0.05;
         }

         if (moveZ < 0.05 && moveZ >= -0.05) {
            moveZ = 0.0;
         } else if (moveZ > 0.0) {
            moveZ -= 0.05;
         } else {
            moveZ += 0.05;
         }
      }

      return new Vec2f((float)moveX, (float)moveZ);
   }

   public static float getYawOffset(Input input, float rotationYaw) {
      if (input.movementForward < 0.0F) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (input.movementForward < 0.0F) {
         forward = -0.5F;
      } else if (input.movementForward > 0.0F) {
         forward = 0.5F;
      }

      float strafe = input.movementSideways;
      if (strafe > 0.0F) {
         rotationYaw -= 90.0F * forward;
      }

      if (strafe < 0.0F) {
         rotationYaw += 90.0F * forward;
      }

      return rotationYaw;
   }
}
