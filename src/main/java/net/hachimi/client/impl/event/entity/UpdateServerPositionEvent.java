package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.LivingEntity;

public class UpdateServerPositionEvent extends Event {
   private final LivingEntity livingEntity;
   private final double x;
   private final double y;
   private final double z;
   private final float yaw;
   private final float pitch;

   public UpdateServerPositionEvent(LivingEntity livingEntity, double x, double y, double z, float yaw, float pitch) {
      this.livingEntity = livingEntity;
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public LivingEntity getLivingEntity() {
      return this.livingEntity;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }
}
