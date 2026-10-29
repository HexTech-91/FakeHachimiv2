package net.hachimi.client.impl.manager.player;

import net.hachimi.client.impl.event.network.PacketSneakingEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class MovementManager implements Globals {
   private boolean packetSneaking;

   public MovementManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   public void setMotionY(double y) {
      mc.player.setVelocity(mc.player.getVelocity().getX(), y, mc.player.getVelocity().getZ());
   }

   public void setMotionXZ(double x, double z) {
      mc.player.setVelocity(x, mc.player.getVelocity().y, z);
   }

   public void setMotionX(double x) {
      mc.player.setVelocity(x, mc.player.getVelocity().y, mc.player.getVelocity().z);
   }

   public void setMotionZ(double z) {
      mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, z);
   }

   public void setPacketSneaking(boolean packetSneaking) {
      this.packetSneaking = packetSneaking;
      if (packetSneaking) {
         Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.PRESS_SHIFT_KEY));
      } else {
         Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
      }
   }

   @EventListener
   public void onPacketSneak(PacketSneakingEvent event) {
      event.setCanceled(this.packetSneaking);
   }
}
