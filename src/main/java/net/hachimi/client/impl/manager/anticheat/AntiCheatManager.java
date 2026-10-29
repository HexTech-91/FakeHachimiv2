package net.hachimi.client.impl.manager.anticheat;

import java.util.Arrays;
import net.hachimi.client.Hachimi;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public final class AntiCheatManager implements Globals {
   private SetbackData lastSetback;
   private final int[] transactions = new int[4];
   private int index;
   private boolean isGrim;

   public AntiCheatManager() {
      EventBus.INSTANCE.subscribe(this);
      Arrays.fill(this.transactions, -1);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof CommonPingS2CPacket packet) {
         if (this.index > 3) {
            return;
         }

         int uid = packet.getParameter();
         this.transactions[this.index] = uid;
         this.index++;
         if (this.index == 4) {
            this.grimCheck();
         }
      } else if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
         this.lastSetback = new SetbackData(new Vec3d(packet.getX(), packet.getY(), packet.getZ()), System.currentTimeMillis(), packet.getTeleportId());
      }
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      Arrays.fill(this.transactions, -1);
      this.index = 0;
      this.isGrim = false;
   }

   private void grimCheck() {
      int i = 0;

      while (i < 4 && this.transactions[i] == -i) {
         i++;
      }

      this.isGrim = true;
      Hachimi.info("Server is running GrimAC.");
   }

   public boolean isGrim() {
      return this.isGrim;
   }

   public boolean hasPassed(long timeMS) {
      return this.lastSetback != null && this.lastSetback.timeSince() >= timeMS;
   }
}
