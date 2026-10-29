package net.hachimi.client.impl.manager.anticheat;

import net.minecraft.util.math.Vec3d;

public record SetbackData(Vec3d position, long timeMS, int teleportID) {
   public long timeSince() {
      return System.currentTimeMillis() - this.timeMS;
   }
}
