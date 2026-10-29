package net.hachimi.client.util.player;

import net.hachimi.client.impl.event.keyboard.KeyboardTickEvent;
import net.hachimi.client.impl.event.network.MovementPacketsEvent;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SnapRotation implements Globals {
   public static final SnapRotation INSTANCE = new SnapRotation();
   public static boolean moveFix = false;
   public static boolean grimFull = true;
   public static boolean doSnapBack = false;
   public static float rotationYaw = 0.0F;
   public static float rotationPitch = 0.0F;
   public static float targetYaw = 0.0F;
   public static float targetPitch = 0.0F;
   public static boolean rotation = false;
   public static boolean holdActive = false;
   public static float holdYaw = 0.0F;
   public static float holdPitch = 0.0F;
   public static boolean lastGround = true;
   private boolean subscribed = false;

   private SnapRotation() {
   }

   public void subscribe() {
      if (!this.subscribed) {
         EventBus.INSTANCE.subscribe(this);
         this.subscribed = true;
      }
   }

   public void unsubscribe() {
      if (this.subscribed) {
         EventBus.INSTANCE.unsubscribe(this);
         this.subscribed = false;
      }
   }

   public static void reset() {
      rotation = false;
      holdActive = false;
      targetYaw = 0.0F;
      targetPitch = 0.0F;
   }

   public static void snapAt(float yaw, float pitch) {
      if (moveFix) {
         rotation = true;
         targetYaw = yaw;
         targetPitch = pitch;
      } else {
         holdActive = true;
         holdYaw = yaw;
         holdPitch = pitch;
         boolean useGrimFull = grimFull;
         AnticheatModule ac = AnticheatModule.getInstance();
         if (ac != null) {
            useGrimFull = ac.isGrimRotation();
         }

         if (useGrimFull) {
            sendLookFull(yaw, pitch);
         } else {
            mc.getNetworkHandler().sendPacket(new LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
         }
      }
   }

   public static void snapAt(Vec3d target) {
      float[] angles = getRotationTo(mc.player.getEyePos(), target);
      snapAt(angles[0], angles[1]);
   }

   public static void snapAt(Box box) {
      snapAt(closestPointToEye(mc.player.getEyePos(), box));
   }

   public static void snapBack() {
      holdActive = false;
      boolean sb = doSnapBack;
      AnticheatModule ac = AnticheatModule.getInstance();
      if (ac != null) {
         sb = ac.isSnapBack();
      }

      if (sb && !moveFix) {
         sendLookFull(rotationYaw, rotationPitch);
      }
   }

   @EventListener
   public void onMovementPackets(MovementPacketsEvent event) {
      if (rotation) {
         event.setYaw(targetYaw);
         event.setPitch(targetPitch);
      } else {
         if (holdActive) {
            event.setYaw(holdYaw);
            event.setPitch(holdPitch);
         }
      }
   }

   @EventListener
   public void onKeyboardTick(KeyboardTickEvent event) {
      if (rotation && event.getStage() == StageEvent.EventStage.POST) {
         fixMovement(event);
      }
   }

   private static void fixMovement(KeyboardTickEvent event) {
      float forward = event.getInput().movementForward;
      float sideways = event.getInput().movementSideways;
      if (forward != 0.0F || sideways != 0.0F) {
         float directionFactor = Math.max(Math.abs(forward), Math.abs(sideways));
         double angleDiff = MathHelper.wrapDegrees(getMovementDirection(forward, sideways) - targetYaw);
         double angleDist = Math.abs(angleDiff);
         float newFwd = 0.0F;
         float newStrafe = 0.0F;
         if (angleDist <= 67.5) {
            newFwd++;
         } else if (angleDist >= 112.5) {
            newFwd--;
         }

         if (angleDiff >= 22.5 && angleDiff <= 157.5) {
            newStrafe--;
         } else if (angleDiff <= -22.5 && angleDiff >= -157.5) {
            newStrafe++;
         }

         event.getInput().movementForward = newFwd * directionFactor;
         event.getInput().movementSideways = newStrafe * directionFactor;
      }
   }

   private static float getMovementDirection(float forward, float sideways) {
      float yaw = mc.player.getYaw();
      if (forward == 0.0F && sideways == 0.0F) {
         return yaw;
      } else if (forward < 0.0F && sideways == 0.0F) {
         return yaw + 180.0F;
      } else if (forward > 0.0F && sideways < 0.0F) {
         return yaw + 45.0F;
      } else if (forward > 0.0F && sideways > 0.0F) {
         return yaw - 45.0F;
      } else if (forward == 0.0F && sideways < 0.0F) {
         return yaw + 90.0F;
      } else if (forward == 0.0F && sideways > 0.0F) {
         return yaw - 90.0F;
      } else if (forward < 0.0F && sideways < 0.0F) {
         return yaw + 135.0F;
      } else {
         return forward < 0.0F ? yaw - 135.0F : yaw;
      }
   }

   public static float[] getRotationTo(Vec3d eyePos, Vec3d target) {
      double dx = target.x - eyePos.x;
      double dy = target.y - eyePos.y;
      double dz = target.z - eyePos.z;
      double dxz = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dxz)));
      return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0F, 90.0F)};
   }

   public static Vec3d closestPointToEye(Vec3d eye, Box box) {
      double x = MathHelper.clamp(eye.x, box.minX, box.maxX);
      double y = MathHelper.clamp(eye.y, box.minY, box.maxY);
      double z = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
      return new Vec3d(x, y, z);
   }

   private static void sendLookFull(float yaw, float pitch) {
      mc.getNetworkHandler().sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
   }
}
