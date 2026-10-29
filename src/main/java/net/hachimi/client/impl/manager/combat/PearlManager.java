package net.hachimi.client.impl.manager.combat;

import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.module.exploit.PhaseModule;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.player.RayCastUtil;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;

public class PearlManager implements Globals {
   private float[] lastThrownAngles;
   private Box pearlBB;

   public PearlManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && PhaseModule.getInstance().shouldRaytrace()) {
         if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet && this.lastThrownAngles != null) {
            BlockHitResult hitResult = (BlockHitResult)RayCastUtil.rayCast(3.0, this.lastThrownAngles);
            this.pearlBB = new Box(hitResult.getPos().subtract(0.4, 0.4, 0.4), hitResult.getPos().add(0.4, 0.4, 0.4));
            if (mc.world.getBlockState(hitResult.getBlockPos()).isAir()) {
               return;
            }

            if (!this.pearlBB.contains(packet.getX(), packet.getY(), packet.getZ())) {
               event.cancel();
               mc.getNetworkHandler().getConnection().send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
               mc.getNetworkHandler()
                  .getConnection()
                  .send(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false));
            }

            this.lastThrownAngles = null;
         }
      }
   }

   public void setLastThrownAngles(float[] lastThrownAngles) {
      this.lastThrownAngles = lastThrownAngles;
   }
}
