package net.hachimi.client.impl.manager.world;

import java.util.concurrent.ConcurrentHashMap;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.player.SpeedmineModule;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.render.animation.EasingPlus;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class BreakManager implements Globals {
   public static BreakManager INSTANCE;
   public final ConcurrentHashMap<Integer, BreakManager.BreakData> breakMap = new ConcurrentHashMap<>();
   public final ConcurrentHashMap<Integer, BreakManager.BreakData> doubleMap = new ConcurrentHashMap<>();
   private static final double BREAK_TIMEOUT = 10.0;
   private static final double DOUBLE_TIMEOUT = 10.0;
   private static final long MIN_TIMEOUT_MS = 3000L;

   public BreakManager() {
      INSTANCE = this;
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.breakMap.clear();
      this.doubleMap.clear();
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         for (int i : this.doubleMap.keySet()) {
            BreakManager.BreakData d = this.doubleMap.get(i);
            if (d == null || d.getEntity() == null || mc.world.isAir(d.pos) || d.timer.passedMs(Math.max(3000L, (long)(d.breakTime * 10.0)))) {
               this.doubleMap.remove(i);
            }
         }

         for (BreakManager.BreakData d : this.breakMap.values()) {
            d.breakTime = Math.max(getBreakTime(d.pos), 50.0);
            if (isUnbreakable(d.pos)) {
               d.fade.setLength(0L);
               d.complete = false;
               d.failed = true;
            } else if (mc.world.isAir(d.pos)) {
               d.fade.setLength(0L);
               d.complete = true;
               d.failed = false;
            } else if (!d.complete && d.timer.passedMs((long)(d.breakTime * 10.0))) {
               d.fade.setLength(0L);
               d.failed = true;
            } else {
               d.fade.setLength((long)d.breakTime);
            }
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof BlockBreakingProgressS2CPacket packet) {
            if (packet.getPos() != null) {
               BlockPos pos = packet.getPos();
               int entityId = packet.getEntityId();
               int progress = packet.getProgress();
               Entity entity = mc.world.getEntityById(entityId);
               if (entity instanceof PlayerEntity) {
                  if (!(MathHelper.sqrt((float)entity.getEyePos().squaredDistanceTo(pos.toCenterPos())) > 8.0F)) {
                     if (progress != 255) {
                        if (progress != 0) {
                           BreakManager.BreakData doublePos = this.doubleMap.get(entityId);
                           if (doublePos != null) {
                              doublePos.pos = pos;
                              doublePos.timer.reset();
                           } else if (!isUnbreakable(pos)) {
                              this.doubleMap.put(entityId, new BreakManager.BreakData(pos, entityId, true));
                           }

                           return;
                        }

                        BreakManager.BreakData doublePos = this.doubleMap.get(entityId);
                        if (doublePos != null && doublePos.pos.equals(pos) && !doublePos.timer.passedMs(150L)) {
                           return;
                        }
                     }

                     BreakManager.BreakData current = this.breakMap.get(entityId);
                     if (current == null || current.failed || !current.pos.equals(pos)) {
                        if (progress != 255 || current != null) {
                           if (progress != 255) {
                              this.breakMap.put(entityId, new BreakManager.BreakData(pos, entityId, false));
                              if (!this.doubleMap.containsKey(entityId) && !isUnbreakable(pos)) {
                                 this.doubleMap.put(entityId, new BreakManager.BreakData(pos, entityId, true));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static double getBreakTime(BlockPos pos) {
      if (mc.world == null) {
         return Double.MAX_VALUE;
      } else {
         float delta = SpeedmineModule.getInstance().calcBlockBreakingDelta(mc.world.getBlockState(pos), mc.world, pos);
         return delta <= 0.0F ? Double.MAX_VALUE : 1.0 / delta / 20.0 * 1000.0;
      }
   }

   public static boolean isUnbreakable(BlockPos pos) {
      return mc.world == null ? false : mc.world.getBlockState(pos).getHardness(mc.world, pos) < 0.0F;
   }

   public boolean isMining(BlockPos pos) {
      return this.isMining(pos, false);
   }

   public boolean isMining(BlockPos pos, boolean self) {
      for (BreakManager.BreakData d : this.breakMap.values()) {
         if (d.getEntity() != null && !d.failed && !(d.getEntity().getEyePos().distanceTo(pos.toCenterPos()) > 7.0) && d.pos.equals(pos)) {
            return true;
         }
      }

      return false;
   }

   public static class BreakData {
      public BlockPos pos;
      private final int entityId;
      public final BreakManager.FadeTimer fade;
      public final BreakManager.SimpleTimer timer;
      public double breakTime;
      public boolean failed = false;
      public boolean complete = false;

      public BreakData(BlockPos pos, int entityId, boolean extraBreak) {
         this.pos = pos;
         this.entityId = entityId;
         this.breakTime = Math.max(BreakManager.getBreakTime(pos), 50.0);
         this.fade = new BreakManager.FadeTimer((long)this.breakTime);
         this.timer = new BreakManager.SimpleTimer();
      }

      public Entity getEntity() {
         if (Globals.mc.world == null) {
            return null;
         } else {
            Entity e = Globals.mc.world.getEntityById(this.entityId);
            return e instanceof PlayerEntity ? e : null;
         }
      }

      public int getEntityId() {
         return this.entityId;
      }
   }

   public static class FadeTimer {
      private long length;
      private long startMs;
      private boolean active;

      public FadeTimer(long length) {
         this.length = length;
         this.startMs = System.currentTimeMillis();
         this.active = length > 0L;
      }

      public void setLength(long length) {
         if (this.length != length) {
            this.length = length;
            this.startMs = System.currentTimeMillis();
            this.active = length > 0L;
         }
      }

      public double getFactor() {
         if (this.length <= 0L) {
            return 0.0;
         } else {
            double t = (double)(System.currentTimeMillis() - this.startMs) / this.length;
            return this.active ? Math.min(t, 1.0) : Math.max(1.0 - t, 0.0);
         }
      }

      public double ease(EasingPlus easing) {
         return easing.ease(this.getFactor());
      }
   }

   public static class SimpleTimer {
      private long start = System.currentTimeMillis();

      public void reset() {
         this.start = System.currentTimeMillis();
      }

      public long getMs() {
         return System.currentTimeMillis() - this.start;
      }

      public long getElapsedTime() {
         return this.getMs();
      }

      public boolean passedMs(long ms) {
         return this.getMs() > ms;
      }
   }
}
