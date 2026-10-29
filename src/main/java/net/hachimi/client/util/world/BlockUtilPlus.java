package net.hachimi.client.util.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.player.RotationUtil;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class BlockUtilPlus implements Globals {
   public static final CopyOnWriteArrayList<BlockPos> placeList = new CopyOnWriteArrayList<>();
   private static final double MIN_EYE_HEIGHT = 0.4;
   private static final double MAX_EYE_HEIGHT = 1.62;
   private static final double MOVEMENT_THRESHOLD = 2.0E-4;
   public static final List<Block> SHIFT_BLOCKS = Arrays.asList(
      Blocks.ENDER_CHEST,
      Blocks.CHEST,
      Blocks.TRAPPED_CHEST,
      Blocks.CRAFTING_TABLE,
      Blocks.ANVIL,
      Blocks.BREWING_STAND,
      Blocks.HOPPER,
      Blocks.DROPPER,
      Blocks.DISPENSER,
      Blocks.ENCHANTING_TABLE,
      Blocks.BIRCH_TRAPDOOR,
      Blocks.BAMBOO_TRAPDOOR,
      Blocks.DARK_OAK_TRAPDOOR,
      Blocks.CHERRY_TRAPDOOR,
      Blocks.ACACIA_TRAPDOOR,
      Blocks.WHITE_SHULKER_BOX,
      Blocks.ORANGE_SHULKER_BOX,
      Blocks.MAGENTA_SHULKER_BOX,
      Blocks.LIGHT_BLUE_SHULKER_BOX,
      Blocks.YELLOW_SHULKER_BOX,
      Blocks.LIME_SHULKER_BOX,
      Blocks.PINK_SHULKER_BOX,
      Blocks.GRAY_SHULKER_BOX,
      Blocks.CYAN_SHULKER_BOX,
      Blocks.PURPLE_SHULKER_BOX,
      Blocks.BLUE_SHULKER_BOX,
      Blocks.BROWN_SHULKER_BOX,
      Blocks.GREEN_SHULKER_BOX,
      Blocks.RED_SHULKER_BOX,
      Blocks.BLACK_SHULKER_BOX
   );

   public static boolean isGrimDirection(BlockPos pos, Direction direction) {
      Box combined = getCombinedBox(pos);
      Box eyeBox = new Box(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), mc.player.getX(), mc.player.getY() + 1.62, mc.player.getZ())
         .expand(2.0E-4);
      if (isIntersected(eyeBox, combined)) {
         return true;
      } else {
         switch (direction) {
            case NORTH:
               if (!(eyeBox.minZ > combined.minZ)) {
                  return true;
               }
               break;
            case SOUTH:
               if (!(eyeBox.maxZ < combined.maxZ)) {
                  return true;
               }
               break;
            case EAST:
               if (!(eyeBox.maxX < combined.maxX)) {
                  return true;
               }
               break;
            case WEST:
               if (!(eyeBox.minX > combined.minX)) {
                  return true;
               }
               break;
            case UP:
               if (!(eyeBox.maxY < combined.maxY)) {
                  return true;
               }
               break;
            case DOWN:
               if (!(eyeBox.minY > combined.minY)) {
                  return true;
               }
               break;
            default:
               throw new MatchException(null, null);
         }

         return false;
      }
   }

   public static Direction getClickSideStrict(BlockPos pos) {
      Direction best = null;
      double minDist = Double.MAX_VALUE;

      for (Direction dir : Direction.values()) {
         if (isGrimDirection(pos, dir)) {
            double dist = mc.player.getEyePos().squaredDistanceTo(pos.offset(dir).toCenterPos());
            if (dist < minDist) {
               minDist = dist;
               best = dir;
            }
         }
      }

      return best;
   }

   public static Direction getClickSide(BlockPos pos) {
      Direction best = null;
      double bestDist = Double.MAX_VALUE;

      for (Direction dir : Direction.values()) {
         if (canSee(pos, dir)) {
            double dist = mc.player.getEyePos().squaredDistanceTo(pos.offset(dir).toCenterPos());
            if (dist < bestDist) {
               bestDist = dist;
               best = dir;
            }
         }
      }

      if (best != null) {
         return best;
      } else {
         bestDist = Double.MAX_VALUE;
         best = Direction.UP;

         for (Direction dirx : Direction.values()) {
            if (isGrimDirection(pos, dirx)) {
               double dist = mc.player.getEyePos().squaredDistanceTo(pos.offset(dirx).toCenterPos());
               if (dist < bestDist) {
                  bestDist = dist;
                  best = dirx;
               }
            }
         }

         return best;
      }
   }

   public static Direction getPlaceSide(BlockPos pos, Predicate<Direction> filter) {
      return getPlaceSide(pos, filter, false);
   }

   public static Direction getPlaceSide(BlockPos pos, Predicate<Direction> filter, boolean ignoreSneak) {
      if (pos == null) {
         return null;
      } else {
         double bestDist = Double.MAX_VALUE;
         Direction best = null;

         for (Direction dir : Direction.values()) {
            if (filter == null || filter.test(dir)) {
               BlockPos neighbor = pos.offset(dir);
               if (canClick(neighbor, ignoreSneak) && !mc.world.getBlockState(neighbor).isReplaceable() && isGrimDirection(neighbor, dir.getOpposite())) {
                  double dist = mc.player
                     .getEyePos()
                     .squaredDistanceTo(pos.toCenterPos().add(dir.getOffsetX() * 0.5, dir.getOffsetY() * 0.5, dir.getOffsetZ() * 0.5));
                  if (dist < bestDist) {
                     bestDist = dist;
                     best = dir;
                  }
               }
            }
         }

         return best;
      }
   }

   public static List<Direction> getPlaceSides(BlockPos pos, Predicate<Direction> filter) {
      return getPlaceSides(pos, filter, false);
   }

   public static List<Direction> getPlaceSides(BlockPos pos, Predicate<Direction> filter, boolean ignoreSneak) {
      List<Direction> sides = new ArrayList<>();
      if (pos == null) {
         return sides;
      } else {
         for (Direction dir : Direction.values()) {
            if (filter == null || filter.test(dir)) {
               BlockPos neighbor = pos.offset(dir);
               BlockState neighborState = mc.world.getBlockState(neighbor);
               if (canClick(neighbor, ignoreSneak) && !neighborState.isReplaceable() && isGrimDirection(neighbor, dir.getOpposite())) {
                  sides.add(dir);
               }
            }
         }

         return sides;
      }
   }

   public static boolean canClick(BlockPos pos) {
      return canClick(pos, false);
   }

   public static boolean canClick(BlockPos pos, boolean ignoreSneak) {
      BlockState state = mc.world.getBlockState(pos);
      Block block = state.getBlock();
      boolean needsShift = SHIFT_BLOCKS.contains(block) || block instanceof BedBlock;
      return state.isSolid() && (!needsShift || mc.player.isSneaking() || ignoreSneak);
   }

   public static boolean canReplace(BlockPos pos) {
      return pos.getY() >= 320 ? false : mc.world.getBlockState(pos).isReplaceable();
   }

   public static boolean canPlace(BlockPos pos) {
      return canPlace(pos, null);
   }

   public static boolean canPlace(BlockPos pos, Predicate<Direction> filter) {
      if (getPlaceSide(pos, filter) == null) {
         return false;
      } else {
         return !canReplace(pos) ? false : !hasEntity(pos, false);
      }
   }

   public static boolean clientCanPlace(BlockPos pos, boolean ignoreCrystal) {
      return !canReplace(pos) ? false : !hasEntity(pos, ignoreCrystal);
   }

   public static boolean canPlaceCrystal(BlockPos pos) {
      if (!mc.world.isAir(pos)) {
         return false;
      } else {
         BlockPos basePos = pos.down();
         Block base = mc.world.getBlockState(basePos).getBlock();
         if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) {
            return false;
         } else if (getClickSideStrict(basePos) == null) {
            return false;
         } else {
            BlockPos abovePos = pos.up();
            return !mc.world.isAir(abovePos) ? false : !hasEntityBlockCrystal(pos, false) && !hasEntityBlockCrystal(abovePos, false);
         }
      }
   }

   public static boolean hasCrystalPlace(BlockPos pos) {
      for (EndCrystalEntity crystal : getEndCrystals(new Box(pos))) {
         if (crystal.isAlive() && crystal.getBlockPos().equals(pos)) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && (!ignoreCrystal || !(entity instanceof EndCrystalEntity))) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasEntity(BlockPos pos, boolean ignoreCrystal, boolean ignorePlayer) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive()
            && !(entity instanceof ItemEntity)
            && !(entity instanceof ExperienceOrbEntity)
            && !(entity instanceof ExperienceBottleEntity)
            && !(entity instanceof ArrowEntity)
            && (!ignoreCrystal || !(entity instanceof EndCrystalEntity))
            && (!ignorePlayer || !(entity instanceof PlayerEntity))) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive() && (!ignoreCrystal || !(entity instanceof EndCrystalEntity))) {
            return true;
         }
      }

      return false;
   }

   public static boolean noEntityBlockCrystal(BlockPos pos, boolean ignoreCrystal, boolean ignoreItem) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity.isAlive() && (!ignoreItem || !(entity instanceof ItemEntity)) && (!ignoreCrystal || !(entity instanceof EndCrystalEntity))) {
            return false;
         }
      }

      return true;
   }

   public static boolean hasPlayerEntity(BlockPos pos) {
      for (Entity entity : getEntities(new Box(pos))) {
         if (entity instanceof PlayerEntity) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasCrystal(BlockPos pos) {
      for (EndCrystalEntity crystal : getEndCrystals(new Box(pos))) {
         if (crystal.isAlive()) {
            return true;
         }
      }

      return false;
   }

   public static boolean hasCrystalAt(BlockPos pos) {
      for (EndCrystalEntity crystal : getEndCrystals(new Box(pos))) {
         if (crystal.isAlive() && crystal.getBlockPos().equals(pos)) {
            return true;
         }
      }

      return false;
   }

   public static List<Entity> getEntities(Box box) {
      List<Entity> list = new ArrayList<>();

      for (Entity entity : mc.world.getEntities()) {
         if (entity != null && entity.getBoundingBox().intersects(box)) {
            list.add(entity);
         }
      }

      return list;
   }

   public static List<EndCrystalEntity> getEndCrystals(Box box) {
      List<EndCrystalEntity> list = new ArrayList<>();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity crystal && crystal.getBoundingBox().intersects(box)) {
            list.add(crystal);
         }
      }

      return list;
   }

   public static List<BlockPos> getSphere(double range) {
      return getSphere(range, mc.player.getEyePos());
   }

   public static List<BlockPos> getSphere(double range, Vec3d origin) {
      List<BlockPos> list = new ArrayList<>();

      for (double x = origin.x - range; x < origin.x + range; x++) {
         for (double z = origin.z - range; z < origin.z + range; z++) {
            for (double y = origin.y - range; y < origin.y + range; y++) {
               BlockPos cur = BlockPos.ofFloored(x, y, z);
               if (cur.toCenterPos().distanceTo(origin) <= range && !list.contains(cur)) {
                  list.add(cur);
               }
            }
         }
      }

      return list;
   }

   public static void placeBlock(BlockPos pos, Direction side, boolean rotate) {
      clickBlock(pos.offset(side), side.getOpposite(), rotate);
   }

   public static void clickBlock(BlockPos pos, Direction face, boolean rotate) {
      Vec3d hitVec = new Vec3d(
         pos.getX() + 0.5 + face.getOffsetX() * 0.5, pos.getY() + 0.5 + face.getOffsetY() * 0.5, pos.getZ() + 0.5 + face.getOffsetZ() * 0.5
      );
      if (rotate) {
         float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitVec);
         Managers.ROTATION.setRotationSilent(rot[0], rot[1]);
      }

      BlockHitResult result = new BlockHitResult(hitVec, face, pos, false);
      Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
      Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      placeList.add(pos);
   }

   public static boolean placeAuto(BlockPos pos, boolean rotate) {
      return placeAuto(pos, null, rotate);
   }

   public static boolean placeAuto(BlockPos pos, Predicate<Direction> filter, boolean rotate) {
      Direction side = getPlaceSide(pos, filter);
      if (side == null) {
         return false;
      } else {
         placeBlock(pos, side, rotate);
         return true;
      }
   }

   public static void clickSlabBlock(BlockPos pos, Direction face, Direction slabSide, boolean rotate) {
      double yOffset = 0.5;
      if (slabSide == Direction.UP) {
         yOffset += 0.1;
      }

      if (slabSide == Direction.DOWN) {
         yOffset -= 0.1;
      }

      Vec3d hitVec = new Vec3d(
         pos.getX() + 0.5 + face.getOffsetX() * 0.5, pos.getY() + yOffset + face.getOffsetY() * 0.5, pos.getZ() + 0.5 + face.getOffsetZ() * 0.5
      );
      if (rotate) {
         float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitVec);
         Managers.ROTATION.setRotationSilent(rot[0], rot[1]);
      }

      BlockHitResult result = new BlockHitResult(hitVec, face, pos, false);
      Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
      Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      placeList.add(pos);
   }

   public static boolean canSee(BlockPos pos, Direction side) {
      Vec3d target = pos.toCenterPos().add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
      HitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), target, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
      return result == null || result.getType() == Type.MISS;
   }

   public static Block getBlock(BlockPos pos) {
      return mc.world.getBlockState(pos).getBlock();
   }

   public static boolean needSneak(Block block) {
      return SHIFT_BLOCKS.contains(block) || block instanceof BedBlock;
   }

   public static Vec3d getClosestPoint(Entity entity) {
      return getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
   }

   public static Vec3d getClosestPointToBox(Vec3d pos, Box box) {
      return new Vec3d(MathHelper.clamp(pos.x, box.minX, box.maxX), MathHelper.clamp(pos.y, box.minY, box.maxY), MathHelper.clamp(pos.z, box.minZ, box.maxZ));
   }

   private static Box getCombinedBox(BlockPos pos) {
      Box combined = new Box(pos);

      for (Box box : mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).offset(pos.getX(), pos.getY(), pos.getZ()).getBoundingBoxes()) {
         double minX = Math.max(box.minX, combined.minX);
         double minY = Math.max(box.minY, combined.minY);
         double minZ = Math.max(box.minZ, combined.minZ);
         double maxX = Math.min(box.maxX, combined.maxX);
         double maxY = Math.min(box.maxY, combined.maxY);
         double maxZ = Math.min(box.maxZ, combined.maxZ);
         combined = new Box(minX, minY, minZ, maxX, maxY, maxZ);
      }

      return combined;
   }

   private static boolean isIntersected(Box a, Box b) {
      return b.maxX - 1.0E-7 > a.minX
         && b.minX + 1.0E-7 < a.maxX
         && b.maxY - 1.0E-7 > a.minY
         && b.minY + 1.0E-7 < a.maxY
         && b.maxZ - 1.0E-7 > a.minZ
         && b.minZ + 1.0E-7 < a.maxZ;
   }
}
