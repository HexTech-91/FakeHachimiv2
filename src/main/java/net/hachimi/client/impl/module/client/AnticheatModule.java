package net.hachimi.client.impl.module.client;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ConcurrentModule;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.mixin.accessor.AccessorPlayerMoveC2SPacket;
import net.hachimi.client.util.math.position.DirectionUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.world.BlastResistantBlocks;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AnticheatModule extends ConcurrentModule {
   private static AnticheatModule INSTANCE;
   Config<AnticheatModule.Anticheats> modeConfig = this.register(
      new EnumConfig<>("Mode", "Applies anticheat optimizations", AnticheatModule.Anticheats.GRIM, AnticheatModule.Anticheats.values())
   );
   Config<AnticheatModule.PlacementMode> placementConfig = this.register(
      new EnumConfig<>("Placement", "Placement packet mode sent to server", AnticheatModule.PlacementMode.Grim, AnticheatModule.PlacementMode.values())
   );
   Config<AnticheatModule.SwingMode> entitySwingConfig = this.register(
      new EnumConfig<>("EntitySwing", "Swing mode when attacking entities", AnticheatModule.SwingMode.Normal, AnticheatModule.SwingMode.values())
   );
   Config<AnticheatModule.SwingMode> placeSwingConfig = this.register(
      new EnumConfig<>("PlaceSwing", "Swing mode when placing blocks", AnticheatModule.SwingMode.None, AnticheatModule.SwingMode.values())
   );
   Config<Float> breakDelayConfig = this.register(new NumberConfig<>("BreakDelay", "Delay in ticks between block breaks", 0.0F, 1.0F, 10.0F));
   Config<AnticheatModule.AttackMode> attackConfig = this.register(
      new EnumConfig<>("Attack", "How to send attack packets to the server", AnticheatModule.AttackMode.Normal, AnticheatModule.AttackMode.values())
   );
   Config<AnticheatModule.PlacePacketMode> placeConfig = this.register(
      new EnumConfig<>("Place", "How to send place packets to the server", AnticheatModule.PlacePacketMode.Packet, AnticheatModule.PlacePacketMode.values())
   );
   Config<Boolean> multiPlaceConfig = this.register(new BooleanConfig("MultiPlace", "Allows placing multiple blocks per tick", false));
   Config<Boolean> multiPointConfig = this.register(new BooleanConfig("MultiPoint", "Sends multiple hit vectors when placing to bypass raytrace", false));
   Config<Boolean> closestDirectionConfig = this.register(new BooleanConfig("ClosestDirection", "Uses closest valid direction when placing blocks", false));
   Config<Boolean> grimAirplaceConfig = this.register(new BooleanConfig("GrimAirplace", "Bypasses Grim airplace check using offhand swap trick", false));
   Config<Boolean> sneakingSpoofConfig = this.register(
      new BooleanConfig("SneakingSpoof", "Spoofs sneaking state when placing to bypass block interaction checks", false)
   );
   Config<Boolean> pickConfig = this.register(new BooleanConfig("Pick", "Cancels PickFromInventory packets to prevent Grim flags", false));
   Config<Float> damageConfig = this.register(new NumberConfig<>("Damage", "Damage multiplier sent to server for Grim consistency", 0.0F, 0.7F, 1.0F));
   Config<Float> entityPlaceConfig = this.register(
      new NumberConfig<>(
         "PlaceThreshold", "Max ticks to place on entities", 0.0F, 2.0F, 25.0F, () -> this.modeConfig.getValue() != AnticheatModule.Anticheats.VANILLA
      )
   );
   Config<Boolean> noScreenCloseConfig = this.register(
      new BooleanConfig(
         "NoScreenClose", "Prevents the server from closing your inventory screen", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM
      )
   );
   Config<Boolean> raytraceSpoofConfig = this.register(
      new BooleanConfig("RaytraceFix", "Allows you to spoof your raytrace", false, () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.N_C_P)
   );
   public Config<Boolean> grimRotationConfig = this.register(
      new BooleanConfig(
         "GrimRotation",
         "Sends Full movement packet when rotating to bypass GrimAC rotation check",
         true,
         () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM
      )
   );
   public Config<Boolean> snapBackConfig = this.register(
      new BooleanConfig(
         "SnapBack",
         "Sends a snap-back rotation packet after placing/attacking to restore real look direction",
         false,
         () -> this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM
      )
   );
   private final Timer raytraceTimer = new CacheTimer();
   private float pitch = Float.NaN;

   public AnticheatModule() {
      super("Anticheat", "Settings for anticheat configs", ModuleCategory.CLIENT);
      INSTANCE = this;
   }

   public static AnticheatModule getInstance() {
      return INSTANCE;
   }

   @EventListener(
      priority = 10000
   )
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (mc.player != null && mc.world != null) {
         if (this.isNCP()
            && this.raytraceSpoofConfig.getValue()
            && event.getPacket() instanceof PlayerInteractBlockC2SPacket packet
            && this.raytraceTimer.passed(250)) {
            BlockHitResult packetResult = packet.getBlockHitResult();
            BlockPos pos = packetResult.getBlockPos();
            BlockHitResult result = mc.world
               .raycast(
                  new RaycastContext(
                     mc.player.getEyePos(), DirectionUtil.getDirectionOffsetPos(pos, packetResult.getSide()), ShapeType.COLLIDER, FluidHandling.NONE, mc.player
                  )
               );
            if (mc.world.isSpaceEmpty(mc.player.getBoundingBox().stretch(0.0, 0.15, 0.0))
               && result != null
               && result.getType() == Type.BLOCK
               && !result.getBlockPos().equals(pos)) {
               this.pitch = -75.0F;
               this.raytraceTimer.reset();
            }
         }

         if (event.getPacket() instanceof PlayerMoveC2SPacket packetx && packetx.changesLook() && !Float.isNaN(this.pitch)) {
            ((AccessorPlayerMoveC2SPacket)packetx).hookSetPitch(this.pitch);
            this.pitch = Float.NaN;
         }

         if (this.isGrim()
            && event.getPacket() instanceof PlayerActionC2SPacket packetx
            && (
               packetx.getAction() == Action.START_DESTROY_BLOCK
                  || packetx.getAction() == Action.STOP_DESTROY_BLOCK
                  || packetx.getAction() == Action.ABORT_DESTROY_BLOCK
            )) {
            if (BlastResistantBlocks.isUnbreakable(packetx.getPos())) {
               event.cancel();
               return;
            }

            if (packetx.getAction() == Action.START_DESTROY_BLOCK) {
               Managers.NETWORK.sendQuietPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, packetx.getPos(), Direction.UP));
               Managers.NETWORK.sendQuietPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, packetx.getPos(), Direction.UP));
               Managers.NETWORK.sendQuietPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, packetx.getPos(), Direction.UP));
               event.cancel();
            }
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (this.isGrim() && this.noScreenCloseConfig.getValue() && event.getPacket() instanceof CloseScreenS2CPacket packet && packet.getSyncId() == 0) {
            event.cancel();
         }
      }
   }

   @EventListener
   public void onConfigUpdate(ConfigUpdateEvent event) {
      if (event.getStage() == StageEvent.EventStage.POST && event.getConfig() == this.raytraceSpoofConfig && !this.raytraceSpoofConfig.getValue()) {
         this.pitch = Float.NaN;
      }
   }

   public boolean isGrim() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.GRIM;
   }

   public boolean isNCP() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.N_C_P;
   }

   public boolean getMiningFix() {
      return this.isGrim();
   }

   public boolean getWebJumpFix() {
      return false;
   }

   public int getEntityPlaceThreshold() {
      return this.modeConfig.getValue() == AnticheatModule.Anticheats.VANILLA ? 0 : Math.round(this.entityPlaceConfig.getValue() * 10.0F);
   }

   public AnticheatModule.PlacementMode getPlacementMode() {
      return this.placementConfig.getValue();
   }

   public AnticheatModule.SwingMode getEntitySwing() {
      return this.entitySwingConfig.getValue();
   }

   public AnticheatModule.SwingMode getPlaceSwing() {
      return this.placeSwingConfig.getValue();
   }

   public float getBreakDelayMs() {
      return this.breakDelayConfig.getValue() * 50.0F;
   }

   public AnticheatModule.AttackMode getAttackMode() {
      return this.attackConfig.getValue();
   }

   public AnticheatModule.PlacePacketMode getPlacePacketMode() {
      return this.placeConfig.getValue();
   }

   public boolean isMultiPlace() {
      return this.multiPlaceConfig.getValue();
   }

   public boolean isMultiPoint() {
      return this.multiPointConfig.getValue();
   }

   public boolean isClosestDirection() {
      return this.closestDirectionConfig.getValue();
   }

   public boolean isGrimAirplace() {
      return this.grimAirplaceConfig.getValue();
   }

   public boolean isSneakingSpoof() {
      return this.sneakingSpoofConfig.getValue();
   }

   public boolean isPick() {
      return this.pickConfig.getValue();
   }

   public float getDamageMultiplier() {
      return this.damageConfig.getValue();
   }

   public boolean isGrimRotation() {
      return this.grimRotationConfig.getValue();
   }

   public boolean isSnapBack() {
      return this.snapBackConfig.getValue();
   }

   private static enum Anticheats {
      GRIM,
      N_C_P,
      VANILLA;
   }

   public static enum AttackMode {
      Normal,
      Packet;
   }

   public static enum PlacePacketMode {
      Normal,
      Packet;
   }

   public static enum PlacementMode {
      Grim,
      NCP,
      Normal;
   }

   public static enum SwingMode {
      Normal,
      None,
      Packet;
   }
}
