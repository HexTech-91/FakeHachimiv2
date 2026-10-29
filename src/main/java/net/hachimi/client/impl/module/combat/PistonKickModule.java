package net.hachimi.client.impl.module.combat;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.client.AnticheatModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.player.SnapRotation;
import net.hachimi.client.util.world.BlockUtilPlus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;

public final class PistonKickModule extends ToggleModule {
   public static PistonKickModule INSTANCE;
   private final Config<Integer> delay = this.register(new NumberConfig<>("Delay", "Delay between pushes (ticks)", 0, 1, 10));
   private final Config<Boolean> oneTwelve = this.register(new BooleanConfig("1.12", "Place piston and power in the same tick", true));
   private final Config<Float> targetRange = this.register(new NumberConfig<>("TargetRange", "Target range", 1.0F, 5.0F, 6.0F));
   private final Config<Boolean> inAirTarget = this.register(new BooleanConfig("InAirTarget", "Target players in the air", true));
   private final Config<Boolean> rotate = this.register(new BooleanConfig("Rotate", "Silently rotate when placing", true));
   private final Config<Boolean> outOfBlock = this.register(new BooleanConfig("OutOfBlock", "Target players partially outside of blocks", true));
   private final Config<Boolean> breakCrystal = this.register(new BooleanConfig("BreakCrystal", "Break end crystals blocking piston path", false));
   private final Config<Float> placeRange = this.register(new NumberConfig<>("PlaceRange", "Block place range", 1.0F, 5.0F, 6.0F));
   private final Config<Float> breakRange = this.register(
      new NumberConfig<>("BreakRange", "Crystal break range", 1.0F, 4.5F, 6.0F, () -> this.breakCrystal.getValue())
   );
   private final Config<PistonKickModule.SwapAction> swapAction = this.register(
      new EnumConfig<>("SwapAction", "How to swap to required items", PistonKickModule.SwapAction.SCREEN, PistonKickModule.SwapAction.values())
   );
   private final Config<Boolean> airPlace = this.register(new BooleanConfig("AirPlace", "Allow placing blocks in the air", false));
   private final Config<Boolean> consecutive = this.register(new BooleanConfig("Consecutive", "Keep targeting the same player consecutively", true));
   private final Config<Boolean> merge = this.register(new BooleanConfig("Merge", "Burst piston and power packets together", false));
   private int tickCount = 0;
   private PlayerEntity lastTarget = null;

   public PistonKickModule() {
      super("PistonKick", "Pushes players out of bedrock holes using pistons", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static PistonKickModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      this.tickCount = 0;
      this.lastTarget = null;
      SnapRotation.INSTANCE.subscribe();
   }

   @Override
   public void onDisable() {
      this.lastTarget = null;
      SnapRotation.INSTANCE.unsubscribe();
      SnapRotation.reset();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         this.tickCount++;
         if (this.tickCount >= this.delay.getValue()) {
            this.tickCount = 0;
            int pistonSlot = this.findClass(PistonBlock.class);
            int powerSlot = this.findPower();
            if (pistonSlot != -1 && powerSlot != -1) {
               List<PlayerEntity> enemies = this.getEnemies();
               if (!enemies.isEmpty()) {
                  if (this.consecutive.getValue() && this.lastTarget != null && enemies.contains(this.lastTarget)) {
                     enemies.remove(this.lastTarget);
                     enemies.add(0, this.lastTarget);
                  }

                  for (PlayerEntity target : enemies) {
                     if (this.inAirTarget.getValue() || target.isOnGround()) {
                        if (this.breakCrystal.getValue()) {
                           this.breakNearbyCrystals(target);
                        }

                        if (this.tryKick(target, pistonSlot, powerSlot)) {
                           this.lastTarget = target;
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean tryKick(PlayerEntity target, int pistonSlot, int powerSlot) {
      float[] offsets = new float[]{-0.25F, 0.0F, 0.25F};

      for (float ox : offsets) {
         for (float oz : offsets) {
            BlockPos playerPos = BlockPos.ofFloored(target.getX() + ox, target.getY() + 0.5, target.getZ() + oz);

            for (Direction dir : Type.HORIZONTAL) {
               BlockPos wall = playerPos.offset(dir);
               if (this.isTargetHere(wall, target) && mc.world.canCollide(target, new Box(wall))) {
                  if (this.tryPush(playerPos.offset(dir.getOpposite()), dir, pistonSlot, powerSlot)) {
                     return true;
                  }

                  if (this.tryPush(playerPos.offset(dir.getOpposite()).up(), dir, pistonSlot, powerSlot)) {
                     return true;
                  }
               }
            }
         }
      }

      if (!mc.world.canCollide(target, new Box(BlockPos.ofFloored(target.getX(), target.getY() + 2.5, target.getZ())))) {
         BlockPos entityPos = target.getBlockPos();

         for (Direction dirx : Type.HORIZONTAL) {
            BlockPos wall = entityPos.offset(dirx);
            Box shifted = target.getBoundingBox().offset(dirx.getOffsetX(), dirx.getOffsetY(), dirx.getOffsetZ());
            if (mc.world.getBlockState(wall.up()).getBlock() != Blocks.PISTON_HEAD
               && !mc.world.canCollide(target, shifted.offset(0.0, 1.0, 0.0))
               && !this.isTargetHere(wall, target)) {
               if (this.tryPush(entityPos.offset(dirx.getOpposite()).up(), dirx, pistonSlot, powerSlot)) {
                  return true;
               }

               if (this.tryPush(entityPos.offset(dirx.getOpposite()), dirx, pistonSlot, powerSlot)) {
                  return true;
               }
            }
         }
      }

      for (float ox : offsets) {
         for (float oz : offsets) {
            BlockPos playerPos = BlockPos.ofFloored(target.getX() + ox, target.getY() + 0.5, target.getZ() + oz);

            for (Direction dirxx : Type.HORIZONTAL) {
               BlockPos wall = playerPos.offset(dirxx);
               if (this.isTargetHere(wall, target)) {
                  if (this.tryPush(playerPos.offset(dirxx.getOpposite()).up(), dirxx, pistonSlot, powerSlot)) {
                     return true;
                  }

                  if (this.tryPush(playerPos.offset(dirxx.getOpposite()), dirxx, pistonSlot, powerSlot)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private boolean tryPush(BlockPos pistonPos, Direction pushDir, int pistonSlot, int powerSlot) {
      if (!mc.world.isAir(pistonPos.offset(pushDir))) {
         return false;
      } else if (!this.canPlaceAt(pistonPos)) {
         return false;
      } else {
         Direction placeSide = this.getPlaceSide(pistonPos);
         if (placeSide != null) {
            boolean canPower = false;

            for (Direction d : Direction.values()) {
               if (this.isRedstone(pistonPos.offset(d)) || this.canPlaceAt(pistonPos.offset(d))) {
                  canPower = true;
                  break;
               }
            }

            if (!canPower) {
               return false;
            } else {
               this.doSwap(pistonSlot);
               if (this.rotate.getValue()) {
                  this.pistonFacingRotation(pushDir, placeSide);
               }

               this.placeBlockDirect(pistonPos, placeSide);
               this.restoreSlot(pistonSlot);
               if (this.oneTwelve.getValue() || this.merge.getValue() || mc.world.getBlockState(pistonPos).getBlock() instanceof PistonBlock) {
                  for (Direction dx : Direction.values()) {
                     if (this.isRedstone(pistonPos.offset(dx))) {
                        return true;
                     }
                  }

                  for (Direction dxx : Direction.values()) {
                     if (dxx != Direction.UP) {
                        BlockPos powerPos = pistonPos.offset(dxx);
                        if (this.canPlaceAt(powerPos) && this.inPlaceRange(powerPos)) {
                           Direction pSide = this.getPlaceSide(powerPos);
                           if (pSide != null) {
                              this.doSwap(powerSlot);
                              if (this.rotate.getValue()) {
                                 this.doRotate(powerPos, pSide);
                              }

                              this.placeBlockDirect(powerPos, pSide);
                              this.restoreSlot(powerSlot);
                              return true;
                           }
                        }
                     }
                  }
               }

               return true;
            }
         } else {
            Direction powerDir = null;

            for (Direction dxxx : Direction.values()) {
               if (dxxx != Direction.UP) {
                  BlockPos powerPos = pistonPos.offset(dxxx);
                  if (this.canPlaceAt(powerPos) && this.inPlaceRange(powerPos) && BlockUtilPlus.getPlaceSide(pistonPos, null) != null) {
                     powerDir = dxxx;
                     break;
                  }
               }
            }

            if (powerDir == null) {
               return false;
            } else {
               BlockPos powerPos = pistonPos.offset(powerDir);
               Direction pSide = this.getPlaceSide(powerPos);
               if (pSide == null) {
                  return false;
               } else {
                  this.doSwap(powerSlot);
                  if (this.rotate.getValue()) {
                     this.doRotate(powerPos, pSide);
                  }

                  this.placeBlockDirect(powerPos, pSide);
                  this.restoreSlot(powerSlot);
                  Direction newSide = this.getPlaceSide(pistonPos);
                  if (newSide != null) {
                     this.doSwap(pistonSlot);
                     if (this.rotate.getValue()) {
                        this.pistonFacingRotation(pushDir, newSide);
                     }

                     this.placeBlockDirect(pistonPos, newSide);
                     this.restoreSlot(pistonSlot);
                  }

                  return true;
               }
            }
         }
      }
   }

   private boolean isTargetHere(BlockPos pos, PlayerEntity target) {
      return new Box(pos).intersects(target.getBoundingBox());
   }

   private boolean isRedstone(BlockPos pos) {
      Block b = mc.world.getBlockState(pos).getBlock();
      return b == Blocks.REDSTONE_BLOCK || b == Blocks.REDSTONE_TORCH;
   }

   private boolean canPlaceAt(BlockPos pos) {
      return mc.world.getBlockState(pos).isReplaceable() || mc.world.isAir(pos);
   }

   private boolean inPlaceRange(BlockPos pos) {
      double r = this.placeRange.getValue().floatValue();
      return mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= r * r;
   }

   private Direction getPlaceSide(BlockPos pos) {
      return this.airPlace.getValue() ? BlockUtilPlus.getClickSideStrict(pos) : BlockUtilPlus.getPlaceSide(pos, null);
   }

   private void breakNearbyCrystals(PlayerEntity target) {
      double r = this.breakRange.getValue().floatValue();
      mc.world
         .getEntitiesByClass(EndCrystalEntity.class, target.getBoundingBox().expand(r), e -> e.isAlive() && mc.player.squaredDistanceTo(e) <= r * r)
         .stream()
         .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
         .ifPresent(crystal -> {
            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         });
   }

   private void placeBlockDirect(BlockPos pos, Direction side) {
      BlockPos neighbor = pos.offset(side);
      Vec3d hitVec = new Vec3d(
         neighbor.getX() + 0.5 + side.getOpposite().getOffsetX() * 0.5,
         neighbor.getY() + 0.5 + side.getOpposite().getOffsetY() * 0.5,
         neighbor.getZ() + 0.5 + side.getOpposite().getOffsetZ() * 0.5
      );
      BlockHitResult result = new BlockHitResult(hitVec, side.getOpposite(), neighbor, false);
      Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
      Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      BlockUtilPlus.placeList.add(pos);
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

   private void pistonFacingRotation(Direction pushDir, Direction placeSide) {
      this.applyAnticheatFlags();

      float yaw = switch (pushDir) {
         case SOUTH -> 0.0F;
         case WEST -> 90.0F;
         case NORTH -> 180.0F;
         case EAST -> -90.0F;
         default -> 0.0F;
      };
      SnapRotation.snapAt(yaw, 0.0F);
   }

   private void doSwap(int slot) {
      switch ((PistonKickModule.SwapAction)(Object)this.swapAction.getValue()) {
         case SCREEN:
            if (slot < 9) {
               Managers.INVENTORY.setSlot(slot);
            } else {
               mc.interactionManager
                  .clickSlot(mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            }
            break;
         case HOTBAR:
            if (slot < 9) {
               Managers.INVENTORY.setSlot(slot);
            }
         case NONE:
      }
   }

   private void restoreSlot(int slot) {
      if (this.swapAction.getValue() == PistonKickModule.SwapAction.SCREEN && slot >= 9) {
         mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
      }
   }

   private int findPower() {
      int slot = this.findBlock(Blocks.REDSTONE_BLOCK);
      return slot != -1 ? slot : this.findBlock(Blocks.REDSTONE_TORCH);
   }

   private int findBlock(Block block) {
      if (this.swapAction.getValue() != PistonKickModule.SwapAction.NONE) {
         int end = this.swapAction.getValue() == PistonKickModule.SwapAction.SCREEN ? 36 : 9;

         for (int i = 0; i < end; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() == block) {
               return i;
            }
         }

         return -1;
      } else {
         ItemStack held = mc.player.getMainHandStack();
         return !held.isEmpty() && held.getItem() instanceof BlockItem bi && bi.getBlock() == block ? mc.player.getInventory().selectedSlot : -1;
      }
   }

   private int findClass(Class<?> clazz) {
      if (this.swapAction.getValue() != PistonKickModule.SwapAction.NONE) {
         int end = this.swapAction.getValue() == PistonKickModule.SwapAction.SCREEN ? 36 : 9;

         for (int i = 0; i < end; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && clazz.isInstance(bi.getBlock())) {
               return i;
            }
         }

         return -1;
      } else {
         ItemStack held = mc.player.getMainHandStack();
         return !held.isEmpty() && held.getItem() instanceof BlockItem bi && clazz.isInstance(bi.getBlock()) ? mc.player.getInventory().selectedSlot : -1;
      }
   }

   private List<PlayerEntity> getEnemies() {
      double r = this.targetRange.getValue().floatValue();
      return mc.world
         .getPlayers()
         .stream()
         .filter(p -> p != mc.player)
         .filter(p -> !p.isSpectator() && !p.isDead())
         .filter(p -> !Managers.SOCIAL.isFriend(p.getName().getString()))
         .filter(p -> mc.player.squaredDistanceTo(p) <= r * r)
         .sorted(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
         .collect(Collectors.toList());
   }

   public static enum SwapAction {
      SCREEN,
      HOTBAR,
      NONE;
   }
}
