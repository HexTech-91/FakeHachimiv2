package net.hachimi.client.impl.manager.combat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.DataTracker.Entry;
import net.minecraft.entity.data.DataTracker.SerializedEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.util.math.Box;

public class HitboxManager implements Globals {
   private final List<Entity> serverCrawling = new CopyOnWriteArrayList<>();

   public HitboxManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof EntityTrackerUpdateS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.id());
            if (!(entity instanceof PlayerEntity)) {
               return;
            }

            for (SerializedEntry<?> serializedEntry : packet.trackedValues()) {
               Entry<?> entry = entity.getDataTracker().entries[serializedEntry.id()];
               if (entry.getData().equals(Entity.POSE)) {
                  if (!serializedEntry.value().equals(EntityPose.SWIMMING)) {
                     this.serverCrawling.remove(entity);
                  } else if (!this.serverCrawling.contains(entity)) {
                     this.serverCrawling.add(entity);
                  }
               }
            }
         }
      }
   }

   public boolean isServerCrawling(Entity entity) {
      return this.serverCrawling.contains(entity);
   }

   public Box getCrawlingBoundingBox(Entity entity) {
      return entity.getDimensions(EntityPose.SWIMMING).getBoxAt(entity.getPos());
   }
}
