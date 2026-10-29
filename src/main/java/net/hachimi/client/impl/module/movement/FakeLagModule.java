package net.hachimi.client.impl.module.movement;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.FakePlayerEntity;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class FakeLagModule extends ToggleModule {
   Config<FakeLagModule.LagMode> modeConfig = this.register(
      new EnumConfig<>("Mode", "The mode for caching packets", FakeLagModule.LagMode.BLINK, FakeLagModule.LagMode.values())
   );
   Config<Boolean> pulseConfig = this.register(new BooleanConfig("Pulse", "Releases packets at intervals", false));
   Config<Float> factorConfig = this.register(
      new NumberConfig<>("Factor", "The factor for packet intervals", 0.0F, 1.0F, 10.0F, () -> this.pulseConfig.getValue())
   );
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders the serverside player position", true));
   private FakePlayerEntity serverModel;
   private boolean blinking;
   private final Queue<Packet<?>> packets = new LinkedBlockingQueue<>();

   public FakeLagModule() {
      super("FakeLag", "Withholds packets from the server, creating clientside lag", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && this.renderConfig.getValue()) {
         this.serverModel = new FakePlayerEntity(mc.player);
         this.serverModel.spawnPlayer();
         this.serverModel.setUuid(mc.player.getUuid());
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         if (!this.packets.isEmpty()) {
            for (Packet<?> p : this.packets) {
               Managers.NETWORK.sendPacket(p);
            }

            this.packets.clear();
         }

         if (this.serverModel != null) {
            this.serverModel.setUuid(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7"));
            this.serverModel.despawnPlayer();
         }
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && this.pulseConfig.getValue() && this.packets.size() > this.factorConfig.getValue() * 10.0F) {
         this.blinking = true;
         if (!this.packets.isEmpty()) {
            for (Packet<?> p : this.packets) {
               Managers.NETWORK.sendPacket(p);
            }
         }

         this.packets.clear();
         if (this.serverModel != null) {
            this.serverModel.copyPositionAndRotation(mc.player);
            this.serverModel.setHeadYaw(mc.player.headYaw);
         }

         this.blinking = false;
      }
   }

   @EventListener
   public void onDisconnectEvent(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && !mc.player.isRiding() && !this.blinking) {
         if (event.getPacket() instanceof PlayerActionC2SPacket
            || event.getPacket() instanceof PlayerMoveC2SPacket
            || event.getPacket() instanceof ClientCommandC2SPacket
            || event.getPacket() instanceof HandSwingC2SPacket
            || event.getPacket() instanceof PlayerInteractEntityC2SPacket
            || event.getPacket() instanceof PlayerInteractBlockC2SPacket
            || event.getPacket() instanceof PlayerInteractItemC2SPacket) {
            event.cancel();
            this.packets.add(event.getPacket());
         }
      }
   }

   public static enum LagMode {
      BLINK;
   }
}
