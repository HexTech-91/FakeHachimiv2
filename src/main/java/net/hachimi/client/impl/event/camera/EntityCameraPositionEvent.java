package net.hachimi.client.impl.event.camera;

import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class EntityCameraPositionEvent extends Event {
   private Vec3d position;
   private final float tickDelta;
   private final Entity entity;

   public EntityCameraPositionEvent(Vec3d position, Entity entity, float tickDelta) {
      this.position = position;
      this.tickDelta = tickDelta;
      this.entity = entity;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public Vec3d getPosition() {
      return this.position;
   }

   public void setPosition(Vec3d position) {
      this.position = position;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
