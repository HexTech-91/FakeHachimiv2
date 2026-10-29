package net.hachimi.client.impl.module.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.CombatModule;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.SnapRotation;
import net.hachimi.client.util.world.BlockUtilPlus;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;

public class PistonCrystalModule extends CombatModule {
   private final Config<Float> minDamage = this.register(new NumberConfig<>("MinDamage", "Minimum damage to attempt a sequence", 1.0F, 6.0F, 20.0F));
   private final Config<Float> maxLocalDamage = this.register(new NumberConfig<>("MaxLocalDamage", "Max self-damage allowed", 1.0F, 12.0F, 20.0F));
   private final Config<Integer> delay = this.register(new NumberConfig<>("Delay", "Delay between place actions (ticks)", 0, 0, 10));
   private final Config<PistonCrystalModule.Sort> sort = this.register(
      new EnumConfig<>("Sort", "How to pick target", PistonCrystalModule.Sort.CLOSE_ANGLE, PistonCrystalModule.Sort.values())
   );
   private final Config<Float> targetRange = this.register(new NumberConfig<>("TargetRange", "Max target range", 1.0F, 6.0F, 20.0F));
   private final Config<Boolean> inAirTarget = this.register(new BooleanConfig("InAirTarget", "Target players in the air", true));
   private final Config<Boolean> breakCrystal = this.register(new BooleanConfig("BreakCrystal", "Attack crystal after placing it", true));
   private final Config<Float> placeRange = this.register(new NumberConfig<>("PlaceRange", "Range to place blocks", 0.1F, 4.5F, 6.0F));
   private final Config<Float> breakRange = this.register(new NumberConfig<>("BreakRange", "Range to break crystals", 0.1F, 4.5F, 6.0F));
   private final Config<Boolean> rotate = this.register(new BooleanConfig("Rotate", "Silently rotate to placed blocks", true));
   private final Config<Boolean> grim = this.register(
      new BooleanConfig("Grim", "YawDeceive — filter dir by player facing & override piston yaw", true, () -> this.rotate.getValue())
   );
   private final Config<PistonCrystalModule.SwapAction> swapAction = this.register(
      new EnumConfig<>("SwapAction", "How to swap to required items", PistonCrystalModule.SwapAction.SCREEN, PistonCrystalModule.SwapAction.values())
   );
   private final Config<Boolean> airPlace = this.register(new BooleanConfig("AirPlace", "Allow placing when not on ground", false));
   private final Config<Boolean> safety = this.register(new BooleanConfig("Safety", "Skip if crystal would kill self", true));
   private final Config<Boolean> blockDestruction = this.register(new BooleanConfig("BlockDestruction", "Account for explosion block destruction", false));
   private final Config<Boolean> selfExtrapolate = this.register(new BooleanConfig("SelfExtrapolate", "Extrapolate self position for self-damage calc", false));
   private final Config<Integer> extrapolationTicks = this.register(new NumberConfig<>("ExtrapolationTicks", "Ticks to extrapolate enemy position", 0, 0, 10));
   private final Config<Boolean> assumeBestArmor = this.register(new BooleanConfig("AssumeBestArmor", "Assume target has Prot 4 netherite", false));
   private BlockPos pistonPos;
   private BlockPos redstonePos;
   private BlockPos crystalPos;
   private BlockPos lastPiston;
   private Direction face;
   private boolean usingTorch;
   private PlayerEntity target;
   private long lastActionMs = 0L;
   private long breakTimerMs = 0L;
   private int preSwapSlot = -1;

   public PistonCrystalModule() {
      super("PistonCrystal", "Places piston + redstone to push a crystal into targets", ModuleCategory.COMBAT, 75);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.resetState();
      this.breakTimerMs = System.currentTimeMillis() + 99999999L;
      SnapRotation.INSTANCE.subscribe();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.resetState();
      SnapRotation.INSTANCE.unsubscribe();
      SnapRotation.reset();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (!mc.player.isUsingItem()) {
            long nowMs = System.currentTimeMillis();
            if (nowMs - this.lastActionMs >= this.delay.getValue().intValue() * 50L) {
               int crystalSlot = this.findItem(Items.END_CRYSTAL);
               int pistonSlot = this.findItemClass(PistonBlock.class);
               int redstoneSlot = this.findRedstone();
               if (crystalSlot != -1 && pistonSlot != -1 && redstoneSlot != -1) {
                  this.target = this.pickTarget();
                  if (this.target != null) {
                     if (this.crystalPos != null
                        && this.redstonePos != null
                        && this.pistonPos != null
                        && (
                           !BlockUtilPlus.canPlaceCrystal(this.crystalPos)
                              || !BlockUtilPlus.canPlace(this.redstonePos)
                              || !BlockUtilPlus.canPlace(this.pistonPos)
                        )) {
                        this.resetState();
                     }

                     if (this.pistonPos == null && this.crystalPos == null && this.redstonePos == null) {
                        this.doPistonCrystal(this.target);
                     }

                     if (this.lastPiston != null
                        && this.face != null
                        && mc.world.getBlockState(this.lastPiston.offset(this.face.getOpposite())).getBlock() instanceof PistonHeadBlock
                        && mc.world.getBlockState(this.lastPiston).getBlock() instanceof PistonBlock) {
                        Direction side = BlockUtilPlus.getClickSide(this.lastPiston);
                        if (side != null) {
                           mc.interactionManager.attackBlock(this.lastPiston, side);
                           this.resetState();
                           return;
                        }
                     }

                     if (this.pistonPos != null && BlockUtilPlus.canPlace(this.pistonPos)) {
                        this.placeBlock(pistonSlot, this.pistonPos, this.face);
                        this.lastPiston = this.pistonPos;
                     }

                     if (this.redstonePos != null && BlockUtilPlus.canPlace(this.redstonePos)) {
                        if (this.usingTorch) {
                           this.placeTorch(redstoneSlot, this.redstonePos);
                        } else {
                           this.placeBlock(redstoneSlot, this.redstonePos, null);
                        }
                     }

                     if (this.crystalPos != null && BlockUtilPlus.canPlaceCrystal(this.crystalPos)) {
                        this.placeCrystal(crystalSlot, this.crystalPos);
                        this.lastActionMs = nowMs;
                        this.breakTimerMs = nowMs;
                     } else {
                        if (this.breakCrystal.getValue() && nowMs - this.breakTimerMs >= this.delay.getValue().intValue() * 50L) {
                           for (int yOff = -1; yOff <= 2; yOff++) {
                              BlockPos check = this.target.getBlockPos().up(yOff);
                              if (BlockUtilPlus.hasCrystal(check)) {
                                 this.attackCrystalAt(check);
                                 this.lastActionMs = nowMs;
                                 this.breakTimerMs = nowMs;
                                 this.resetState();
                                 break;
                              }
                           }
                        }
                     }
                  }
               } else {
                  this.resetState();
               }
            }
         }
      }
   }

   private void doPistonCrystal(PlayerEntity tgt) {
      BlockPos base = tgt.getBlockPos();
      int ticks = this.extrapolationTicks.getValue();
      Vec3d vel = tgt.getVelocity();
      double ex = tgt.getX() + vel.x * ticks;
      double ey = tgt.getY() + vel.y * ticks;
      double ez = tgt.getZ() + vel.z * ticks;
      base = BlockPos.ofFloored(ex, ey, ez);

      for (int h : new int[]{0, 1, 2}) {
         BlockPos standPos = base.up(h);
         Vec3d vec = new Vec3d(standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5);
         float dmg = (float)ExplosionUtil.getDamageTo(tgt, vec, this.blockDestruction.getValue(), ticks, this.assumeBestArmor.getValue());
         float selfDmg = (float)ExplosionUtil.getDamageTo(
            mc.player, vec, this.blockDestruction.getValue(), this.selfExtrapolate.getValue() ? ticks : 0, this.assumeBestArmor.getValue()
         );
         if (!(dmg <= this.minDamage.getValue())
            && !(selfDmg > this.maxLocalDamage.getValue())
            && (!this.safety.getValue() || !(selfDmg >= mc.player.getHealth() + mc.player.getAbsorptionAmount()))
            && this.trySearch(base, h)) {
            return;
         }
      }
   }

   private boolean trySearch(BlockPos base, int height) {
      List<Direction> dirs = new ArrayList<>();
      Direction facing = mc.player.getHorizontalFacing();
      dirs.add(facing);

      for (Direction d : Type.HORIZONTAL) {
         if (d != facing) {
            dirs.add(d);
         }
      }

      for (Direction dir : dirs) {
         BlockPos temp1 = base.offset(dir).up(height);
         if (BlockUtilPlus.canPlaceCrystal(temp1) && this.inRange(temp1, this.placeRange.getValue().floatValue())) {
            BlockPos tempPiston = null;
            BlockPos tempRedstone = null;

            label129:
            for (Direction dir2 : Type.HORIZONTAL) {
               if (dir2 != dir.getOpposite()) {
                  BlockPos temp2 = temp1.offset(dir).offset(dir2);
                  BlockPos extCheck = temp2.offset(dir.getOpposite());
                  if (mc.world.isAir(extCheck) || mc.world.getBlockState(extCheck).isReplaceable()) {
                     if (!BlockUtilPlus.canPlace(temp2) && !(mc.world.getBlockState(temp2).getBlock() instanceof PistonBlock)) {
                        for (Direction help : Direction.values()) {
                           if (help != dir.getOpposite()) {
                              BlockPos helpPos = temp2.offset(help);
                              if (BlockUtilPlus.isGrimDirection(helpPos, help.getOpposite())
                                 && BlockUtilPlus.canPlace(helpPos)
                                 && this.inRange(helpPos, this.placeRange.getValue().floatValue())) {
                                 Direction side = BlockUtilPlus.getPlaceSide(helpPos, dx -> true);
                                 if (side != null) {
                                    int redSlot = this.findRedstone();
                                    if (redSlot != -1) {
                                       this.doSwap(redSlot);
                                       if (this.rotate.getValue()) {
                                          this.doRotate(helpPos, side);
                                       }

                                       BlockUtilPlus.placeBlock(helpPos, side, false);
                                       if (this.rotate.getValue()) {
                                          this.doSnapBack();
                                       }

                                       this.restoreSlot(redSlot);
                                       return true;
                                    }
                                 }
                              }
                           }
                        }
                     } else if (this.inRange(temp2, this.placeRange.getValue().floatValue())) {
                        tempPiston = temp2;

                        for (Direction dir3 : Direction.values()) {
                           if (dir3 != dir.getOpposite()) {
                              BlockPos temp3 = temp2.offset(dir3);
                              if (mc.world.getBlockState(temp3).getBlock() instanceof RedstoneBlock
                                 || mc.world.getBlockState(temp3).getBlock() instanceof RedstoneTorchBlock) {
                                 tempRedstone = temp2.offset(dir3);
                                 break label129;
                              }

                              if (BlockUtilPlus.canPlace(temp3) && this.inRange(temp3, this.placeRange.getValue().floatValue())) {
                                 tempRedstone = temp2.offset(dir3);
                                 break label129;
                              }
                           }
                        }
                        break;
                     }
                  }
               }
            }

            if (tempPiston != null && tempRedstone != null) {
               this.face = dir;
               this.crystalPos = temp1;
               this.pistonPos = tempPiston;
               this.redstonePos = tempRedstone;
               return true;
            }
         }
      }

      return false;
   }

   private void placeBlock(int slot, BlockPos pos, Direction pistonDir) {
      if (slot != -1) {
         Direction side = BlockUtilPlus.getPlaceSide(pos, d -> true);
         if (side != null) {
            this.doSwap(slot);
            if (this.rotate.getValue()) {
               this.doRotate(pos, side);
               if (pistonDir != null && this.grim.getValue()) {
                  this.pistonFacingSnap(pistonDir);
               }
            }

            BlockUtilPlus.placeBlock(pos, side, false);
            if (this.rotate.getValue()) {
               this.doSnapBack();
            }

            this.restoreSlot(slot);
         }
      }
   }

   private void placeCrystal(int slot, BlockPos cp) {
      BlockPos base = cp.down();
      Direction side = BlockUtilPlus.getClickSide(base);
      if (side == null) {
         side = Direction.UP;
      }

      if (slot != -1) {
         this.doSwap(slot);
         if (this.rotate.getValue()) {
            this.doRotate(base, side);
         }

         BlockUtilPlus.clickBlock(base, side, false);
         if (this.rotate.getValue()) {
            this.doSnapBack();
         }

         this.restoreSlot(slot);
      }
   }

   private void placeTorch(int slot, BlockPos pos) {
      if (BlockUtilPlus.canPlace(pos)) {
         if (slot != -1) {
            List<Direction> sides = BlockUtilPlus.getPlaceSides(pos, null);
            if (!sides.isEmpty()) {
               for (Direction side : sides) {
                  if (!(mc.world.getBlockState(pos.offset(side)).getBlock() instanceof PistonBlock) && side != Direction.UP) {
                     this.doSwap(slot);
                     if (this.rotate.getValue()) {
                        this.doRotate(pos, side);
                     }

                     BlockUtilPlus.placeBlock(pos, side, false);
                     if (this.rotate.getValue()) {
                        this.doSnapBack();
                     }

                     this.restoreSlot(slot);
                     return;
                  }
               }
            }
         }
      }
   }

   private void attackCrystalAt(BlockPos pos) {
      List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos).expand(1.0), e -> e.isAlive());
      if (!crystals.isEmpty()) {
         double brSq = this.breakRange.getValue() * this.breakRange.getValue();
         EndCrystalEntity crystal = crystals.stream()
            .filter(e -> mc.player.squaredDistanceTo(e) <= brSq)
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
            .orElse(null);
         if (crystal != null) {
            if (this.rotate.getValue()) {
               this.applyAnticheatFlags();
               SnapRotation.snapAt(crystal.getBoundingBox());
            }

            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            if (this.rotate.getValue()) {
               this.doSnapBack();
            }
         }
      }
   }

   private void applyAnticheatFlags() {
      AnticheatModule ac = AnticheatModule.getInstance();
      SnapRotation.grimFull = ac != null ? ac.isGrimRotation() : true;
      SnapRotation.doSnapBack = ac != null ? ac.isSnapBack() : false;
   }

   private void doRotate(BlockPos pos, Direction side) {
      this.applyAnticheatFlags();
      SnapRotation.snapAt(pos.toCenterPos().add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5));
   }

   private void pistonFacingSnap(Direction dir) {
      this.applyAnticheatFlags();
      if (dir == Direction.EAST) {
         SnapRotation.snapAt(-90.0F, 5.0F);
      } else if (dir == Direction.WEST) {
         SnapRotation.snapAt(90.0F, 5.0F);
      } else if (dir == Direction.NORTH) {
         SnapRotation.snapAt(180.0F, 5.0F);
      } else {
         SnapRotation.snapAt(0.0F, 5.0F);
      }
   }

   private void doSnapBack() {
      AnticheatModule ac = AnticheatModule.getInstance();
      boolean sb = ac != null ? ac.isSnapBack() : false;
      if (sb) {
         SnapRotation.snapBack();
      } else {
         SnapRotation.holdActive = false;
      }
   }

   private void doSwap(int slot) {
      if (slot >= 0) {
         this.preSwapSlot = mc.player.getInventory().selectedSlot;
         switch ((PistonCrystalModule.SwapAction)(Object)this.swapAction.getValue()) {
            case SCREEN:
               if (slot < 9) {
                  Managers.INVENTORY.setClientSlot(slot);
               } else {
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, this.preSwapSlot + 36, SlotActionType.SWAP, mc.player);
                  Managers.INVENTORY.setClientSlot(this.preSwapSlot);
               }
               break;
            case HOTBAR:
               if (slot < 9) {
                  Managers.INVENTORY.setClientSlot(slot);
               }
            case NONE:
         }
      }
   }

   private void restoreSlot(int slot) {
      if (this.preSwapSlot != -1) {
         if (this.swapAction.getValue() == PistonCrystalModule.SwapAction.SCREEN && slot >= 9) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, this.preSwapSlot + 36, SlotActionType.SWAP, mc.player);
         } else if (this.swapAction.getValue() != PistonCrystalModule.SwapAction.NONE) {
            Managers.INVENTORY.setClientSlot(this.preSwapSlot);
         }

         this.preSwapSlot = -1;
      }
   }

   private int findItem(Item item) {
      if (this.swapAction.getValue() != PistonCrystalModule.SwapAction.NONE) {
         int end = this.swapAction.getValue() == PistonCrystalModule.SwapAction.SCREEN ? 36 : 9;

         for (int i = 0; i < end; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() == item) {
               return i;
            }
         }

         return -1;
      } else {
         ItemStack h = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
         return !h.isEmpty() && h.getItem() == item ? mc.player.getInventory().selectedSlot : -1;
      }
   }

   private int findItemClass(Class<?> c) {
      if (this.swapAction.getValue() == PistonCrystalModule.SwapAction.NONE) {
         ItemStack h = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
         return !h.isEmpty() && h.getItem() instanceof BlockItem bi && c.isInstance(bi.getBlock()) ? mc.player.getInventory().selectedSlot : -1;
      } else {
         int end = this.swapAction.getValue() == PistonCrystalModule.SwapAction.SCREEN ? 36 : 9;

         for (int i = 0; i < end; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && c.isInstance(bi.getBlock())) {
               return i;
            }
         }

         return -1;
      }
   }

   private int findRedstone() {
      int slot = this.findItem(Items.REDSTONE_BLOCK);
      if (slot != -1) {
         this.usingTorch = false;
         return slot;
      } else {
         slot = this.findItem(Items.REDSTONE_TORCH);
         if (slot != -1) {
            this.usingTorch = true;
            return slot;
         } else {
            return -1;
         }
      }
   }

   private PlayerEntity pickTarget() {
      double r = this.targetRange.getValue().floatValue();
      return mc.world
         .getPlayers()
         .stream()
         .filter(e -> !(e instanceof ClientPlayerEntity))
         .filter(e -> !e.isSpectator() && !e.isDead())
         .filter(e -> mc.player.squaredDistanceTo(e) <= r * r)
         .filter(e -> !Managers.SOCIAL.isFriend(e.getName().getString()))
         .filter(e -> this.inAirTarget.getValue() || e.isOnGround())
         .min(this.getComparator())
         .orElse(null);
   }

   private Comparator<PlayerEntity> getComparator() {
      return switch ((PistonCrystalModule.Sort)(Object)this.sort.getValue()) {
         case CLOSE_ANGLE, LOWEST_DISTANCE -> Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e));
         case LOWEST_HEALTH -> Comparator.comparingDouble(e -> e.getHealth() + e.getAbsorptionAmount());
      };
   }

   private boolean inRange(BlockPos pos, double r) {
      return mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= r * r;
   }

   private void resetState() {
      this.pistonPos = null;
      this.redstonePos = null;
      this.crystalPos = null;
      this.lastPiston = null;
      this.face = null;
      this.usingTorch = false;
   }

   public static enum Sort {
      CLOSE_ANGLE,
      LOWEST_DISTANCE,
      LOWEST_HEALTH;
   }

   public static enum SwapAction {
      SCREEN,
      HOTBAR,
      NONE;
   }
}
