package net.hachimi.client.impl.manager.combat;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.hachimi.client.impl.event.entity.EntityDeathEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class TotemManager implements Globals {
   private final ConcurrentMap<UUID, TotemManager.TotemData> totems = new ConcurrentHashMap<>();

   public TotemManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.world != null) {
         if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            Entity entity = packet.getEntity(mc.world);
            if (entity != null && entity.isAlive()) {
               if (this.totems.containsKey(entity.getUuid())) {
                  this.totems
                     .replace(entity.getUuid(), new TotemManager.TotemData(System.currentTimeMillis(), this.totems.get(entity.getUuid()).getPops() + 1));
               } else {
                  this.totems.put(entity.getUuid(), new TotemManager.TotemData(System.currentTimeMillis(), 1));
               }
            }
         }
      }
   }

   @EventListener(
      priority = Integer.MIN_VALUE
   )
   public void onRemoveEntity(EntityDeathEvent event) {
      this.totems.remove(event.getEntity().getUuid());
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.totems.clear();
   }

   public int getTotems(Entity entity) {
      return this.totems.getOrDefault(entity.getUuid(), new TotemManager.TotemData(0L, 0)).getPops();
   }

   public long getLastPopTime(Entity entity) {
      return this.totems.getOrDefault(entity.getUuid(), new TotemManager.TotemData(-1L, 0)).getLastPopTime();
   }

   public static class TotemData {
      private final long lastPopTime;
      private final int pops;

      public TotemData(long lastPopTime, int pops) {
         this.lastPopTime = lastPopTime;
         this.pops = pops;
      }

      public int getPops() {
         return this.pops;
      }

      public long getLastPopTime() {
         return this.lastPopTime;
      }
   }
}
