package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.util.math.Vec3d;

@Cancelable
public class VelocityEvent extends Event {
   private Vec3d velocity;

   public VelocityEvent(Vec3d velocity) {
      this.velocity = velocity;
   }

   public void setVelocity(Vec3d velocity) {
      this.velocity = velocity;
   }

   public void setXYZ(double x, double y, double z) {
      this.velocity = new Vec3d(x, y, z);
   }

   public Vec3d getVelocity() {
      return this.velocity;
   }
}
