package net.hachimi.client.impl.module.misc;

import java.time.Instant;
import java.util.BitSet;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ConcurrentModule;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.gui.screen.MenuDisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.encryption.NetworkEncryptionUtils.SecureRandomUtil;
import net.minecraft.network.message.LastSeenMessageList.Acknowledgment;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public final class ServerModule extends ConcurrentModule {
   Config<Boolean> demoConfig = this.register(new BooleanConfig("NoDemo", "Prevents servers from forcing you to a demo screen", true));
   Config<Boolean> resourcePackConfig = this.register(new BooleanConfig("NoResourcePack", "Prevents server from forcing resource pack", false));
   Config<Boolean> antiCrashConfig = this.register(new BooleanConfig("NoServerCrash", "Prevents server packets from crashing the client", false));
   Config<Boolean> illegalDisconnectConfig = this.register(new BooleanConfig("IllegalDisconnect", "Disconnects by getting kicked from server", false));

   public ServerModule() {
      super("Server", "Prevents servers actions on player", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof GameStateChangeS2CPacket packet
         && packet.getReason() == GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN
         && !mc.isDemo()
         && this.demoConfig.getValue()) {
         Hachimi.info("Server attempted to use Demo mode features on you!");
         event.cancel();
      }

      if (event.getPacket() instanceof ResourcePackSendS2CPacket && this.resourcePackConfig.getValue()) {
         event.cancel();
         Managers.NETWORK.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), Status.DECLINED));
      }

      if (mc.world != null) {
         if (this.antiCrashConfig.getValue()) {
            if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet
               && (
                  packet.getX() > 3.0E7
                     || packet.getY() > mc.world.getTopY()
                     || packet.getZ() > 3.0E7
                     || packet.getX() < -3.0E7
                     || packet.getY() < mc.world.getBottomY()
                     || packet.getZ() < -3.0E7
               )) {
               event.cancel();
            } else if (event.getPacket() instanceof ExplosionS2CPacket packet
               && (
                  packet.getX() > 3.0E7
                     || packet.getY() > mc.world.getTopY()
                     || packet.getZ() > 3.0E7
                     || packet.getX() < -3.0E7
                     || packet.getY() < mc.world.getBottomY()
                     || packet.getZ() < -3.0E7
                     || packet.getRadius() > 1000.0F
                     || packet.getAffectedBlocks().size() > 1000
                     || packet.getPlayerVelocityX() > 1000.0F
                     || packet.getPlayerVelocityY() > 1000.0F
                     || packet.getPlayerVelocityZ() > 1000.0F
                     || packet.getPlayerVelocityX() < -1000.0F
                     || packet.getPlayerVelocityY() < -1000.0F
                     || packet.getPlayerVelocityZ() < -1000.0F
               )) {
               event.cancel();
            } else if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet
               && (
                  packet.getVelocityX() > 1000.0
                     || packet.getVelocityY() > 1000.0
                     || packet.getVelocityZ() > 1000.0
                     || packet.getVelocityX() < -1000.0
                     || packet.getVelocityY() < -1000.0
                     || packet.getVelocityZ() < -1000.0
               )) {
               event.cancel();
            } else if (event.getPacket() instanceof ParticleS2CPacket packet && packet.getCount() > 500) {
               event.cancel();
            }
         }
      }
   }

   @EventListener
   public void onMenuDisconnect(MenuDisconnectEvent event) {
      if (this.illegalDisconnectConfig.getValue()) {
         Managers.NETWORK.sendPacket(new ChatMessageC2SPacket("§", Instant.now(), SecureRandomUtil.nextLong(), null, new Acknowledgment(1, new BitSet(2))));
      }
   }
}
