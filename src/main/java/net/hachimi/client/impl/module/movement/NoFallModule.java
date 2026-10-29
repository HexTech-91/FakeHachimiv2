package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerUpdateEvent;
import net.hachimi.client.impl.module.exploit.PacketFlyModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.hachimi.client.util.string.EnumFormatter;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class NoFallModule extends ToggleModule {
   Config<NoFallModule.NoFallMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode to prevent fall damage", NoFallModule.NoFallMode.ANTI, NoFallModule.NoFallMode.values())
   );
   private double groundedY;

   public NoFallModule() {
      super("NoFall", "Prevents all fall damage", ModuleCategory.MOVEMENT);
   }

   @Override
   public String getModuleData() {
      return EnumFormatter.formatEnum(this.modeConfig.getValue());
   }

   @EventListener
   public void onPlayerUpdate(PlayerUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && this.checkFalling()) {
         if (this.modeConfig.getValue() == NoFallModule.NoFallMode.LIMIT) {
            if (mc.world.getRegistryKey() == World.NETHER) {
               Managers.NETWORK.sendPacket(new PositionAndOnGround(mc.player.getX(), 0.0, mc.player.getZ(), true));
            } else {
               Managers.NETWORK.sendPacket(new PositionAndOnGround(0.0, 64.0, 0.0, true));
            }

            mc.player.fallDistance = 0.0F;
         } else if (this.modeConfig.getValue() == NoFallModule.NoFallMode.GRIM) {
            Managers.NETWORK
               .sendPacket(new Full(mc.player.getX(), mc.player.getY() + 1.0E-9, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true));
            mc.player.onLanding();
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && this.checkFalling()) {
         if (event.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (this.modeConfig.getValue() == NoFallModule.NoFallMode.PACKET) {
               ((AccessorPlayerMoveC2SPacket)packet).hookSetOnGround(true);
            } else if (this.modeConfig.getValue() == NoFallModule.NoFallMode.ANTI) {
               double y = packet.getY(mc.player.getY());
               ((AccessorPlayerMoveC2SPacket)packet).hookSetY(y + 0.1F);
            } else if (this.modeConfig.getValue() == NoFallModule.NoFallMode.STRICT && this.isFallingLagback() && mc.player.fallDistance >= 3.0F) {
               Managers.MOVEMENT.setMotionY(0.0);
               ((AccessorPlayerMoveC2SPacket)packet).hookSetY(this.groundedY);
               mc.player.fallDistance = 0.0F;
            }
         }
      }
   }

   public double getGroundY() {
      for (int i = (int)Math.round(mc.player.getY()); i > 0; i--) {
         Box bb = mc.player.getBoundingBox();
         Box box = new Box(bb.minX, i - 1.0, bb.minZ, bb.maxX, i, bb.maxZ);
         if (mc.world.isSpaceEmpty(box) && !(box.minY > mc.player.getY())) {
            return i;
         }
      }

      return 0.0;
   }

   public boolean isFallingLagback() {
      this.groundedY = this.getGroundY() - 0.1;
      return mc.player.getY() - this.groundedY < 3.0;
   }

   private boolean checkFalling() {
      return mc.player.fallDistance > mc.player.getSafeFallDistance()
         && !mc.player.isOnGround()
         && !mc.player.isFallFlying()
         && !FlightModule.getInstance().isEnabled()
         && !PacketFlyModule.getInstance().isEnabled();
   }

   public static enum NoFallMode {
      ANTI,
      LIMIT,
      PACKET,
      STRICT,
      GRIM;
   }
}
