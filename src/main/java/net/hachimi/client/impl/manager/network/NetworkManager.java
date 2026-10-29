package net.hachimi.client.impl.manager.network;

import java.util.HashSet;
import java.util.Set;
import net.hachimi.client.impl.event.gui.screen.ConnectScreenEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.imixin.IClientPlayNetworkHandler;
import net.hachimi.client.mixin.accessor.AccessorClientWorld;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.math.PerSecondCounter;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;

public class NetworkManager implements Globals {
   private static final Set<Packet<?>> PACKET_CACHE = new HashSet<>();
   private ServerAddress address;
   private ServerInfo info;
   private final PerSecondCounter outgoingCounter = new PerSecondCounter();
   private final PerSecondCounter incomingCounter = new PerSecondCounter();

   public NetworkManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onConnect(ConnectScreenEvent event) {
      this.address = event.getAddress();
      this.info = event.getInfo();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      PACKET_CACHE.clear();
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      this.outgoingCounter.updateCounter();
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Inbound event) {
      this.incomingCounter.updateCounter();
   }

   public void connect(ServerAddress address, ServerInfo info) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler()
            .getConnection()
            .connect(
               address.getAddress(),
               address.getPort(),
               new ClientLoginNetworkHandler(mc.getNetworkHandler().getConnection(), mc, info, null, false, null, null, null)
            );
      }
   }

   public void sendPacket(Packet<?> p) {
      if (mc.getNetworkHandler() != null) {
         PACKET_CACHE.add(p);
         mc.getNetworkHandler().sendPacket(p);
      }
   }

   public void sendQuietPacket(Packet<?> p) {
      if (mc.getNetworkHandler() != null) {
         PACKET_CACHE.add(p);
         ((IClientPlayNetworkHandler)mc.getNetworkHandler()).sendQuietPacket(p);
      }
   }

   public void sendSequencedPacket(SequencedPacketCreator p) {
      if (mc.world != null) {
         try (PendingUpdateManager updater = ((AccessorClientWorld)mc.world).hookGetPendingUpdateManager().incrementSequence()) {
            int i = updater.getSequence();
            Packet<ServerPlayPacketListener> packet = p.predict(i);
            this.sendPacket(packet);
         }
      }
   }

   public int getClientLatency() {
      if (mc.getNetworkHandler() != null) {
         PlayerListEntry playerEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getGameProfile().getId());
         if (playerEntry != null) {
            return playerEntry.getLatency();
         }
      }

      return 0;
   }

   public ServerAddress getAddress() {
      return this.address;
   }

   public void setAddress(ServerAddress address) {
      this.address = address;
   }

   public ServerInfo getInfo() {
      return this.info;
   }

   public void setInfo(ServerInfo info) {
      this.info = info;
   }

   public boolean isCrystalPvpCC() {
      return this.getServerIp().contains("crystalpvp.cc");
   }

   public boolean is2b2t() {
      return this.getServerIp().contains("2b2t.org");
   }

   public int getOutgoingPPS() {
      return this.outgoingCounter.getPerSecond();
   }

   public int getIncomingPPS() {
      return this.incomingCounter.getPerSecond();
   }

   public String getServerIp() {
      return this.info != null ? this.info.address : "Singleplayer";
   }

   public boolean isCached(Packet<?> p) {
      return PACKET_CACHE.contains(p);
   }
}
