package net.hachimi.client.impl.module.combat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.ObsidianPlacerModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.position.PositionUtil;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.world.BlockUtilPlus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public final class BlockerModule extends ObsidianPlacerModule {
   private static BlockerModule INSTANCE;
   Config<BlockerModule.Page> pageConfig = this.register(new EnumConfig<>("Page", "Settings page", BlockerModule.Page.General, BlockerModule.Page.values()));
   Config<Integer> delayConfig = this.register(
      new NumberConfig<>("PlaceDelay", "Milliseconds between place batches", 0, 50, 500, () -> this.pageConfig.getValue() == BlockerModule.Page.General)
   );
   Config<Integer> blocksPerConfig = this.register(
      new NumberConfig<>("BlocksPer", "Maximum blocks placed per batch", 1, 1, 8, () -> this.pageConfig.getValue() == BlockerModule.Page.General)
   );
   Config<Boolean> rotatePlaceConfig = this.register(
      new BooleanConfig("Rotate", "Silently rotates to the block before placing", true, () -> this.pageConfig.getValue() == BlockerModule.Page.General)
   );
   Config<Boolean> breakCrystalConfig = this.register(
      new BooleanConfig("Break", "Attacks crystals blocking a placement before placing", true, () -> this.pageConfig.getValue() == BlockerModule.Page.General)
   );
   Config<Boolean> bevelCevConfig = this.register(
      new BooleanConfig("BevelCev", "Blocks diagonal-above corners next to your head", true, () -> this.pageConfig.getValue() == BlockerModule.Page.Target)
   );
   Config<Boolean> faceConfig = this.register(
      new BooleanConfig("Face", "Blocks crystal placements around your body", true, () -> this.pageConfig.getValue() == BlockerModule.Page.Target)
   );
   Config<Boolean> faceUpConfig = this.register(
      new BooleanConfig(
         "FaceUp", "Also blocks straight above your head", false, () -> this.pageConfig.getValue() == BlockerModule.Page.Target && this.faceConfig.getValue()
      )
   );
   Config<Boolean> feetConfig = this.register(
      new BooleanConfig(
         "Feet", "Watches blocks around your feet for mining and pre-places blockers", true, () -> this.pageConfig.getValue() == BlockerModule.Page.Target
      )
   );
   Config<Boolean> extendConfig = this.register(
      new BooleanConfig(
         "Extend",
         "Extends the blocker one extra layer out from the mined block",
         false,
         () -> this.pageConfig.getValue() == BlockerModule.Page.Target && this.feetConfig.getValue()
      )
   );
   Config<Boolean> burrowConfig = this.register(
      new BooleanConfig(
         "Burrow", "Also reacts to blocks being mined from directly below/above", true, () -> this.pageConfig.getValue() == BlockerModule.Page.Target
      )
   );
   Config<Boolean> onlySurroundConfig = this.register(
      new BooleanConfig(
         "OnlySurround",
         "Only run Face/Feet detection while Surround or SelfTrap is enabled",
         true,
         () -> this.pageConfig.getValue() == BlockerModule.Page.Target
      )
   );
   Config<Boolean> inAirPauseConfig = this.register(
      new BooleanConfig("InAirPause", "Pauses placement while airborne", false, () -> this.pageConfig.getValue() == BlockerModule.Page.Check)
   );
   Config<Boolean> detectMiningConfig = this.register(
      new BooleanConfig(
         "DetectMining", "Don't place into a block that is still being mined", true, () -> this.pageConfig.getValue() == BlockerModule.Page.Check
      )
   );
   Config<Boolean> eatingPauseConfig = this.register(
      new BooleanConfig("EatingPause", "Pauses placement while eating/using an item", true, () -> this.pageConfig.getValue() == BlockerModule.Page.Check)
   );
   private static final double SCAN_OFFSET = 0.3;
   private final Timer placeTimer = new CacheTimer();
   private final List<BlockPos> placePos = new ArrayList<>();
   private final List<BlockPos> blockerPos = new ArrayList<>();
   private final Set<BlockPos> placedThisBatch = new LinkedHashSet<>();
   private BlockPos lastPlayerPos;

   public BlockerModule() {
      super("Blocker", "Blocks crystal placements and mining spots around you", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static BlockerModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.placePos.clear();
      this.blockerPos.clear();
      this.placedThisBatch.clear();
      this.lastPlayerPos = null;
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!this.eatingPauseConfig.getValue() || !mc.player.isUsingItem()) {
            if (this.placeTimer.passed(this.delayConfig.getValue())) {
               this.placedThisBatch.clear();
               BlockPos playerPos = PositionUtil.getRoundedBlockPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
               if (this.lastPlayerPos != null && !this.lastPlayerPos.equals(playerPos)) {
                  this.placePos.clear();
                  this.blockerPos.clear();
               }

               this.lastPlayerPos = playerPos;
               boolean surroundActive = !this.onlySurroundConfig.getValue()
                  || SurroundModule.getInstance() != null && SurroundModule.getInstance().isEnabled()
                  || SelfTrapModule.getInstance() != null && SelfTrapModule.getInstance().isEnabled();
               double[] offsets = new double[]{0.3, -0.3, 0.0};
               if (this.bevelCevConfig.getValue()) {
                  for (Direction dir : Direction.values()) {
                     if (dir != Direction.DOWN && !this.isBedrock(playerPos.offset(dir).up())) {
                        BlockPos pos = playerPos.offset(dir).up(2);
                        if (BlockUtilPlus.hasCrystal(pos) && !this.placePos.contains(pos)) {
                           this.placePos.add(pos);
                        }
                     }
                  }
               }

               if (this.faceConfig.getValue() && surroundActive) {
                  for (double x : offsets) {
                     for (double z : offsets) {
                        for (Direction dirx : Direction.values()) {
                           for (int d = 0; d < 3; d++) {
                              BlockPos pos = PositionUtil.getRoundedBlockPos(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + z)
                                 .offset(dirx, d)
                                 .up();
                              if (BlockUtilPlus.hasCrystal(pos) && !this.placePos.contains(pos)) {
                                 this.placePos.add(pos);
                              }
                           }
                        }
                     }
                  }

                  if (this.faceUpConfig.getValue()) {
                     for (Direction dirx : Direction.values()) {
                        if (dirx != Direction.DOWN && !this.isBedrock(playerPos.offset(dirx).up())) {
                           BlockPos pos = playerPos.offset(dirx).up(2);
                           if (BlockUtilPlus.hasCrystal(pos) && !this.placePos.contains(pos)) {
                              this.placePos.add(pos);
                           }
                        }
                     }
                  }
               }

               ObsidianPlacerModule.BlockSlot obsidianSlot = this.getResistantBlockItem();
               if (obsidianSlot != null) {
                  this.placePos.removeIf(posx -> !BlockUtilPlus.clientCanPlace(posx, true));
                  if (this.burrowConfig.getValue()) {
                     for (double x : offsets) {
                        for (double z : offsets) {
                           BlockPos surroundPos = PositionUtil.getRoundedBlockPos(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);
                           if (!this.isBedrock(surroundPos) && Managers.BLOCK.isBreaking(surroundPos)) {
                              for (Direction dirxx : Direction.values()) {
                                 if (dirxx != Direction.UP && dirxx != Direction.DOWN) {
                                    BlockPos defensePos = surroundPos.offset(dirxx);
                                    if (!this.detectMiningConfig.getValue() || !Managers.BLOCK.isBreaking(defensePos)) {
                                       if (this.breakCrystalConfig.getValue()) {
                                          this.attackCrystalAt(defensePos);
                                       }

                                       if (BlockUtilPlus.canPlace(defensePos) && !this.blockerPos.contains(defensePos)) {
                                          this.blockerPos.add(defensePos);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }

                  if (this.feetConfig.getValue() && surroundActive) {
                     for (double x : offsets) {
                        for (double zx : offsets) {
                           for (Direction i : Direction.values()) {
                              BlockPos surroundPos = PositionUtil.getRoundedBlockPos(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + zx)
                                 .offset(i);
                              if (!this.isBedrock(surroundPos) && Managers.BLOCK.isBreaking(surroundPos)) {
                                 for (Direction dirxxx : Direction.values()) {
                                    BlockPos defensePos = surroundPos.offset(dirxxx);
                                    if (!this.detectMiningConfig.getValue() || !Managers.BLOCK.isBreaking(defensePos)) {
                                       if (this.breakCrystalConfig.getValue()) {
                                          this.attackCrystalAt(defensePos);
                                       }

                                       if (BlockUtilPlus.canPlace(defensePos) && !this.blockerPos.contains(defensePos)) {
                                          this.blockerPos.add(defensePos);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     if (this.extendConfig.getValue()) {
                        for (double x : offsets) {
                           for (double zx : offsets) {
                              for (Direction ix : Direction.values()) {
                                 if (ix != Direction.UP && ix != Direction.DOWN) {
                                    BlockPos surroundPos = PositionUtil.getRoundedBlockPos(mc.player.getX() + x, mc.player.getY() + 0.5, mc.player.getZ() + zx)
                                       .offset(ix);
                                    if (!this.isBedrock(surroundPos)) {
                                       for (Direction dirxxxx : Direction.values()) {
                                          if (dirxxxx != Direction.UP && dirxxxx != Direction.DOWN) {
                                             BlockPos blockPos = surroundPos.offset(dirxxxx);
                                             if (BlockUtilPlus.canPlaceCrystal(blockPos)
                                                && (!this.detectMiningConfig.getValue() || !Managers.BLOCK.isBreaking(blockPos))) {
                                                if (this.breakCrystalConfig.getValue()) {
                                                   this.attackCrystalAt(blockPos);
                                                }

                                                if (BlockUtilPlus.canPlace(blockPos) && !this.blockerPos.contains(blockPos)) {
                                                   this.blockerPos.add(blockPos);
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }

                  this.blockerPos.removeIf(posx -> !BlockUtilPlus.clientCanPlace(posx, true));
                  if (!this.inAirPauseConfig.getValue() || mc.player.isOnGround()) {
                     if (!this.blockerPos.isEmpty() || !this.placePos.isEmpty()) {
                        int placed = 0;

                        for (BlockPos pos : this.placePos) {
                           if (placed >= this.blocksPerConfig.getValue()) {
                              break;
                           }

                           if (this.tryPlace(pos, obsidianSlot)) {
                              placed++;
                           }
                        }

                        for (BlockPos pos : this.blockerPos) {
                           if (placed >= this.blocksPerConfig.getValue()) {
                              break;
                           }

                           if (this.tryPlace(pos, obsidianSlot)) {
                              placed++;
                           }
                        }

                        if (placed > 0) {
                           this.placeTimer.reset();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryPlace(BlockPos pos, ObsidianPlacerModule.BlockSlot slot) {
      if (this.placedThisBatch.contains(pos)) {
         return false;
      } else if (!mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, pos, ShapeContext.absent())) {
         return false;
      } else {
         Managers.INTERACT.placeBlock(pos, slot.slot(), this.strictDirectionConfig.getValue(), false, true, (state, angles) -> {
            if (this.rotatePlaceConfig.getValue() && state) {
               Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
         });
         this.placedThisBatch.add(pos);
         return true;
      }
   }

   private void attackCrystalAt(BlockPos pos) {
      Entity crystalEntity = mc.world.getOtherEntities(null, new Box(pos)).stream().filter(e -> e instanceof EndCrystalEntity).findFirst().orElse(null);
      if (crystalEntity != null) {
         Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
         Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      }
   }

   private boolean isBedrock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
   }

   public static enum Page {
      General,
      Target,
      Check;
   }
}
