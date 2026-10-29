package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.ObsidianPlacerModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.player.AirPlaceModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.world.BlastResistantBlocks;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public class SurroundModule extends ObsidianPlacerModule {
   private static SurroundModule INSTANCE;
   Config<SurroundModule.Timing> timingConfig = this.register(
      new EnumConfig<>("Timing", "Timing for replacing blocks", SurroundModule.Timing.VANILLA, SurroundModule.Timing.values())
   );
   Config<Boolean> prePlaceExplosionConfig = this.register(
      new BooleanConfig("PrePlace-Explosions", "Pre places before explosions", false, () -> this.timingConfig.getValue() == SurroundModule.Timing.SEQUENTIAL)
   );
   Config<Boolean> prePlaceTickConfig = this.register(
      new BooleanConfig("PrePlace-Tick", "Pre places before ticks", false, () -> this.timingConfig.getValue() == SurroundModule.Timing.SEQUENTIAL)
   );
   Config<Float> placeRangeConfig = this.register(new NumberConfig<>("PlaceRange", "The placement range for surround", 0.0F, 4.0F, 6.0F));
   Config<Boolean> attackConfig = this.register(new BooleanConfig("Attack", "Attacks crystals in the way of surround", true));
   Config<Boolean> extendConfig = this.register(new BooleanConfig("Extend", "Extends surround if the player is not in the center of a block", true));
   Config<Boolean> headConfig = this.register(new BooleanConfig("CoverHead", "Place a block at your head", false));
   Config<Boolean> mineExtendConfig = this.register(new BooleanConfig("MineExtend", "Extends surround if the block is being mined", false));
   Config<Boolean> supportConfig = this.register(new BooleanConfig("Support", "Creates a floor for the surround if there is none", false));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig<>("ShiftTicks", "The number of blocks to place per tick", 1, 2, 10));
   Config<Float> shiftDelayConfig = this.register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0.0F, 1.0F, 5.0F));
   Config<Boolean> jumpDisableConfig = this.register(new BooleanConfig("AutoDisable", "Disables after moving out of the hole", true));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders where scaffold is placing blocks", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private int blocksPlaced;
   private List<BlockPos> surround = new ArrayList<>();
   private List<BlockPos> placements = new ArrayList<>();
   private final Map<BlockPos, Long> packets = new HashMap<>();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();
   private double prevY;

   public SurroundModule() {
      super("Surround", "Surrounds feet with obsidian", ModuleCategory.COMBAT, 950);
      INSTANCE = this;
   }

   public static SurroundModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.prevY = mc.player.getY();
      }
   }

   @Override
   public void onDisable() {
      this.surround.clear();
      this.placements.clear();
      this.packets.clear();
      this.fadeList.clear();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      this.blocksPlaced = 0;
      if (!SelfTrapModule.getInstance().isEnabled()) {
         if (!this.jumpDisableConfig.getValue() || !(mc.player.getY() - this.prevY > 0.5) && !(mc.player.fallDistance > 1.5F)) {
            if (!this.multitaskConfig.getValue() && this.checkMultitask()) {
               this.surround.clear();
               this.placements.clear();
            } else {
               ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
               if (blockSlot != null) {
                  int slot = blockSlot.slot();
                  if (slot == -1) {
                     this.surround.clear();
                     this.placements.clear();
                  } else {
                     this.surround = this.getSurround(mc.player);
                     if (!this.surround.isEmpty()) {
                        if (this.attackConfig.getValue()) {
                           this.attackBlockingCrystals(this.surround);
                        }

                        this.placements = this.getPlacementsFromSurround(this.surround);
                        if (!this.placements.isEmpty()) {
                           if (this.supportConfig.getValue()) {
                              for (BlockPos block : new ArrayList<>(this.placements)) {
                                 if (!(block.getY() > mc.player.getBlockY() + 1.0)) {
                                    Direction direction = Managers.INTERACT.getInteractDirectionInternal(block, this.strictDirectionConfig.getValue());
                                    if (direction == null) {
                                       this.placements.add(block.down());
                                    }
                                 }
                              }
                           }

                           this.placements.sort(Comparator.comparingInt(Vec3i::getY));

                           while (this.blocksPlaced < this.shiftTicksConfig.getValue() && this.blocksPlaced < this.placements.size()) {
                              BlockPos targetPos = this.placements.get(this.blocksPlaced);
                              this.placeBlock(targetPos, slot);
                           }

                           if (this.rotateConfig.getValue()) {
                              Managers.ROTATION.setRotationSilentSync();
                           }
                        }
                     }
                  }
               }
            }
         } else {
            this.disable();
         }
      }
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) {
               this.handlePackets(packet1);
            }
         } else {
            this.handlePackets(event.getPacket());
         }
      }
   }

   private void handlePackets(Packet<?> serverPacket) {
      if (this.timingConfig.getValue() == SurroundModule.Timing.SEQUENTIAL) {
         if (serverPacket instanceof BlockUpdateS2CPacket packet) {
            BlockState blockState = packet.getState();
            BlockPos targetPos = packet.getPos();
            if (this.surround.contains(targetPos)) {
               if (blockState.isReplaceable() && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, targetPos, ShapeContext.absent())) {
                  ObsidianPlacerModule.BlockSlot blockSlot = this.getResistantBlockItem();
                  if (blockSlot == null) {
                     return;
                  }

                  int slot = blockSlot.slot();
                  if (slot == -1) {
                     return;
                  }

                  this.placeBlock(targetPos, slot);
               } else if (BlastResistantBlocks.isBlastResistant(blockState)) {
                  this.packets.remove(targetPos);
               }
            }
         }

         if (this.blocksPlaced <= this.shiftTicksConfig.getValue() * 2) {
            if (serverPacket instanceof ExplosionS2CPacket packetx && this.prePlaceExplosionConfig.getValue()) {
               BlockPos pos = BlockPos.ofFloored(packetx.getX(), packetx.getY(), packetx.getZ());
               if (this.surround.contains(pos)) {
                  ObsidianPlacerModule.BlockSlot blockSlotx = this.getResistantBlockItem();
                  if (blockSlotx == null) {
                     return;
                  }

                  int slot = blockSlotx.slot();
                  if (slot == -1) {
                     return;
                  }

                  this.placeBlock(pos, slot);
               }
            }

            if (serverPacket instanceof EntitySpawnS2CPacket packetxx
               && packetxx.getEntityType().equals(EntityType.END_CRYSTAL)
               && this.prePlaceTickConfig.getValue()) {
               for (BlockPos pos : this.surround) {
                  if (pos.equals(BlockPos.ofFloored(packetxx.getX(), packetxx.getY(), packetxx.getZ()))) {
                     ObsidianPlacerModule.BlockSlot blockSlotxx = this.getResistantBlockItem();
                     if (blockSlotxx == null) {
                        return;
                     }

                     int slot = blockSlotxx.slot();
                     if (slot == -1) {
                        return;
                     }

                     this.placeBlock(pos, slot);
                     break;
                  }
               }
            }
         }
      }
   }

   private void placeBlock(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock(pos, slot, this.strictDirectionConfig.getValue(), false, true, (state, angles) -> {
         if (this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }
      });
      this.packets.put(pos, System.currentTimeMillis());
      this.blocksPlaced++;
   }

   public void attackBlockingCrystals(List<BlockPos> posList) {
      for (BlockPos pos : posList) {
         Entity crystalEntity = mc.world.getOtherEntities(null, new Box(pos)).stream().filter(e -> e instanceof EndCrystalEntity).findFirst().orElse(null);
         if (crystalEntity != null) {
            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return;
         }
      }
   }

   public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround) {
      List<BlockPos> placements = new ArrayList<>();

      for (BlockPos surroundPos : surround) {
         Long placed = this.packets.get(surroundPos);
         if ((
               !(this.shiftDelayConfig.getValue() > 0.0F)
                  || placed == null
                  || !((float)(System.currentTimeMillis() - placed) < this.shiftDelayConfig.getValue() * 50.0F)
            )
            && mc.world.getBlockState(surroundPos).isReplaceable()) {
            double dist = mc.player.squaredDistanceTo(surroundPos.toCenterPos());
            if (!(dist > ((NumberConfig)(Object)this.placeRangeConfig).getValueSq()) && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, surroundPos, ShapeContext.absent())) {
               placements.add(surroundPos);
            }
         }
      }

      return placements;
   }

   public List<BlockPos> getSurround(PlayerEntity player) {
      List<BlockPos> surroundBlocks = this.getSurroundNoDown(player);
      List<BlockPos> playerBlocks = this.getPlayerBlocks(player);

      for (BlockPos playerPos : playerBlocks) {
         if (!playerPos.equals(player.getBlockPos())) {
            surroundBlocks.add(playerPos.down());
         }
      }

      if (this.mineExtendConfig.getValue()) {
         for (BlockPos surroundPos : new ArrayList<>(surroundBlocks)) {
            if (Managers.BLOCK.isPassed(surroundPos, 0.7F)) {
               for (Direction direction : Direction.values()) {
                  if (direction != Direction.DOWN) {
                     BlockPos blockerPos = surroundPos.offset(direction);
                     if (!playerBlocks.contains(blockerPos) && AutoMineModule.getInstance().getMiningBlock() != blockerPos) {
                        surroundBlocks.add(blockerPos);
                     }
                  }
               }
            }
         }
      }

      if (AirPlaceModule.getInstance().isEnabled() && this.headConfig.getValue()) {
         surroundBlocks.add(mc.player.getBlockPos().up(2));
      }

      return surroundBlocks;
   }

   public List<BlockPos> getSurroundNoDown(PlayerEntity player) {
      return this.getSurroundNoDown(player, 0.0F);
   }

   public List<BlockPos> getSurroundNoDown(PlayerEntity player, float range) {
      List<BlockPos> surroundBlocks = new ArrayList<>();
      List<BlockPos> playerBlocks = this.getPlayerBlocks(player);

      for (BlockPos pos : playerBlocks) {
         if (!(range > 0.0F) || !(mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > range * range)) {
            for (Direction dir : Direction.values()) {
               if (dir.getAxis().isHorizontal()) {
                  BlockPos pos1 = pos.offset(dir);
                  if (!surroundBlocks.contains(pos1) && !playerBlocks.contains(pos1)) {
                     surroundBlocks.add(pos1);
                  }
               }
            }
         }
      }

      return surroundBlocks;
   }

   public List<BlockPos> getPlayerBlocks(PlayerEntity entity) {
      BlockPos playerPos = PositionUtil.getRoundedBlockPos(entity.getX(), entity.getY(), entity.getZ());
      List<BlockPos> playerBlocks = new ArrayList<>();
      if (this.extendConfig.getValue()) {
         playerBlocks.addAll(PositionUtil.getAllInBox(entity.getBoundingBox(), playerPos));
      } else {
         playerBlocks.add(playerPos);
      }

      return playerBlocks;
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (!SelfTrapModule.getInstance().isEnabled()) {
         if (this.renderConfig.getValue()) {
            RenderBuffers.preRender();

            for (Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
               set.getValue().setState(false);
               int boxAlpha = (int)(40.0 * set.getValue().getFactor());
               int lineAlpha = (int)(100.0 * set.getValue().getFactor());
               Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
               Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
               RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
               RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5F, lineColor.getRGB());
            }

            RenderBuffers.postRender();
            if (this.placements.isEmpty()) {
               return;
            }

            for (BlockPos pos : this.placements) {
               Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
               this.fadeList.put(pos, animation);
            }
         }

         this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
      }
   }

   public boolean isPlacing() {
      return !this.placements.isEmpty();
   }

   public static enum Timing {
      VANILLA,
      SEQUENTIAL;
   }
}
