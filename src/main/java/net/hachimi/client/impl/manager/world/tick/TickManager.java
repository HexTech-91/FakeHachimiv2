package net.hachimi.client.impl.manager.world.tick;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Queue;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.render.TickCounterEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.collection.EvictingQueue;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

public class TickManager implements Globals {
   private final Deque<Float> ticks = new EvictingQueue<>(20);
   private long time;
   private float clientTick = 1.0F;

   public TickManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.ticks.clear();
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            float last = 20000.0F / (float)(System.currentTimeMillis() - this.time);
            this.ticks.addFirst(last);
            this.time = System.currentTimeMillis();
         }
      }
   }

   public void setClientTick(float ticks) {
      this.clientTick = ticks;
   }

   @EventListener
   public void onTickCounter(TickCounterEvent event) {
      if (this.clientTick != 1.0F) {
         event.cancel();
         event.setTicks(this.clientTick);
      }
   }

   public Queue<Float> getTicks() {
      return this.ticks;
   }

   public float getTpsAverage() {
      float avg = 0.0F;

      try {
         ArrayList<Float> ticksCopy = Lists.newArrayList(this.ticks);
         if (!ticksCopy.isEmpty()) {
            for (float t : ticksCopy) {
               avg += t;
            }

            avg /= Math.max((float)ticksCopy.size(), 1.0F);
         }
      } catch (NullPointerException var5) {
      }

      return Math.min(100.0F, avg);
   }

   public float getTpsCurrent() {
      try {
         if (!this.ticks.isEmpty()) {
            return Math.min(100.0F, this.ticks.getFirst());
         }
      } catch (NoSuchElementException var2) {
      }

      return 20.0F;
   }

   public float getTpsMin() {
      float min = 20.0F;

      try {
         for (float t : this.ticks) {
            if (t < min) {
               min = t;
            }
         }
      } catch (NullPointerException var4) {
      }

      return min;
   }

   public boolean isTicksFilled() {
      return this.ticks.size() >= 20;
   }

   public float getTickSync(TickSync tps) {
      return switch (tps) {
         case AVERAGE -> this.getTpsAverage();
         case CURRENT -> this.getTpsCurrent();
         case MINIMAL -> this.getTpsMin();
         case NONE -> 20.0F;
      };
   }
}
