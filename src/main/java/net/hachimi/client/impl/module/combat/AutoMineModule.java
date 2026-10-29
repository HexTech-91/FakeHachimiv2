package net.hachimi.client.impl.module.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BlockListConfig;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.AttackBlockEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.module.player.SpeedmineModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.math.timer.TickTimer;
import net.hachimi.client.util.math.timer.Timer;
import net.hachimi.client.util.player.RotationUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;

public final class AutoMineModule extends ToggleModule {
   public static AutoMineModule INSTANCE;
   private final Config<AutoMineModule.SelectionMode> selection = this.register(
      new EnumConfig<>("Selection", "Block filter mode", AutoMineModule.SelectionMode.BLACKLIST, AutoMineModule.SelectionMode.values())
   );
   private final Config<List<Block>> whitelist = this.register(
      new BlockListConfig<>("Whitelist", "Blocks to mine (whitelist mode)", Blocks.OBSIDIAN, Blocks.ENDER_CHEST)
   );
   private final Config<List<Block>> blacklist = this.register(
      new BlockListConfig<>(
         "Blacklist",
         "Blocks to skip (blacklist mode)",
         Blocks.BEDROCK,
         Blocks.LADDER,
         Blocks.SCAFFOLDING,
         Blocks.FIRE,
         Blocks.COBWEB,
         Blocks.WHITE_BED,
         Blocks.ORANGE_BED,
         Blocks.MAGENTA_BED,
         Blocks.LIGHT_BLUE_BED,
         Blocks.YELLOW_BED,
         Blocks.LIME_BED,
         Blocks.PINK_BED,
         Blocks.GRAY_BED,
         Blocks.LIGHT_GRAY_BED,
         Blocks.CYAN_BED,
         Blocks.PURPLE_BED,
         Blocks.BLUE_BED,
         Blocks.BROWN_BED,
         Blocks.GREEN_BED,
         Blocks.RED_BED,
         Blocks.BLACK_BED
      )
   );
   private final Config<Float> targetRange = this.register(new NumberConfig<>("TargetRange", "Range to find target", 1.0F, 6.0F, 12.0F));
   private final Config<Float> mineRange = this.register(new NumberConfig<>("MineRange", "Range to mine blocks", 1.0F, 6.0F, 6.0F));
   private final Config<AutoMineModule.SortMode> sort = this.register(
      new EnumConfig<>("Sort", "Target sorting", AutoMineModule.SortMode.LOWEST_DISTANCE, AutoMineModule.SortMode.values())
   );
   private final Config<Boolean> wait = this.register(new BooleanConfig("Wait", "Wait for block to break before mining next", true));
   private final Config<Boolean> burrow = this.register(new BooleanConfig("Burrow", "Mine block player is standing inside", true));
   private final Config<Boolean> surround = this.register(new BooleanConfig("Surround", "Mine surround blocks around enemy", true));
   private final Config<Boolean> under = this.register(new BooleanConfig("Under", "Mine block below target's feet", false));
   private final Config<Boolean> face = this.register(new BooleanConfig("Face", "Rotate to block before mining", false));
   private final Config<Integer> priority = this.register(new NumberConfig<>("Priority", "Mining priority (higher = mine first)", 0, 8, 10));
   private static final Set<Block> ALWAYS_SKIP = Set.of(
      Blocks.BEDROCK,
      Blocks.BARRIER,
      Blocks.COMMAND_BLOCK,
      Blocks.CHAIN_COMMAND_BLOCK,
      Blocks.REPEATING_COMMAND_BLOCK,
      Blocks.STRUCTURE_BLOCK,
      Blocks.JIGSAW,
      Blocks.FIRE,
      Blocks.AIR,
      Blocks.VOID_AIR,
      Blocks.CAVE_AIR
   );
   private final Timer tickTimer = new TickTimer();
   private final Map<BlockPos, Direction> miningMap = new LinkedHashMap<>();
   private PlayerEntity currentTarget;
   private BlockPos lastMinePos;
   private BlockPos manualMinePos;
   private boolean silentSwapping = false;

   public AutoMineModule() {
      super("AutoMine", "Automatically mine blocks around enemies", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AutoMineModule getInstance() {
      return INSTANCE;
   }

   public BlockPos getMiningBlock() {
      return this.lastMinePos;
   }

   public boolean isSilentSwapping() {
      return this.silentSwapping;
   }

   @Override
   public void onEnable() {
      this.miningMap.clear();
      this.currentTarget = null;
      this.lastMinePos = null;
      this.manualMinePos = null;
      this.silentSwapping = false;
   }

   @Override
   public void onDisable() {
      for (Entry<BlockPos, Direction> e : this.miningMap.entrySet()) {
         Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, e.getKey(), e.getValue()));
      }

      this.miningMap.clear();
      this.currentTarget = null;
      this.lastMinePos = null;
      this.manualMinePos = null;
      this.silentSwapping = false;
   }

   @EventListener
   public void onAttackBlock(AttackBlockEvent event) {
      if (mc.player != null && mc.world != null) {
         BlockPos pos = event.getPos();
         if (this.lastMinePos == null || !pos.equals(this.lastMinePos)) {
            this.manualMinePos = pos;
            if (this.lastMinePos != null && this.miningMap.containsKey(this.lastMinePos) && !this.lastMinePos.equals(pos)) {
               Direction dir = this.miningMap.remove(this.lastMinePos);
               Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, this.lastMinePos, dir));
               this.lastMinePos = null;
            }
         }
      }
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (mc.player != null && mc.world != null) {
         this.miningMap.entrySet().removeIf(e -> mc.world.getBlockState(e.getKey()).isAir());
         if (this.manualMinePos != null) {
            BlockState manualState = mc.world.getBlockState(this.manualMinePos);
            boolean stillMining = !manualState.isAir()
               && mc.player.squaredDistanceTo(this.manualMinePos.toCenterPos()) <= this.mineRange.getValue() * this.mineRange.getValue();
            if (stillMining) {
               return;
            }

            this.manualMinePos = null;
         }

         this.currentTarget = this.getBestTarget();
         if (this.wait.getValue() && this.lastMinePos != null && this.miningMap.containsKey(this.lastMinePos) && this.isStillMinable(this.lastMinePos)) {
            this.doMine(this.lastMinePos);
         } else if (this.currentTarget == null) {
            if (!this.miningMap.isEmpty()) {
               this.abortAll();
            }

            this.lastMinePos = null;
         } else {
            List<BlockPos> toMine = this.getBlocksToMine(this.currentTarget);
            if (toMine.isEmpty()) {
               if (!this.miningMap.isEmpty()) {
                  this.abortAll();
               }

               this.lastMinePos = null;
            } else {
               if (this.wait.getValue()) {
                  if (this.lastMinePos != null && !mc.world.getBlockState(this.lastMinePos).isAir() && this.miningMap.containsKey(this.lastMinePos)) {
                     this.doMine(this.lastMinePos);
                     return;
                  }

                  BlockPos next = toMine.stream().filter(p -> !this.miningMap.containsKey(p)).findFirst().orElse(null);
                  if (next == null) {
                     return;
                  }

                  this.doMine(next);
               } else {
                  for (BlockPos pos : toMine) {
                     this.doMine(pos);
                  }
               }
            }
         }
      }
   }

   private void doMine(BlockPos pos) {
      if (!mc.world.getBlockState(pos).isAir()) {
         Direction bestFace = this.getBestFace(pos);
         if (this.face.getValue()) {
            float[] rot = RotationUtil.getRotationsTo(
               mc.player.getEyePos(), pos.toCenterPos().add(bestFace.getOffsetX() * 0.5, bestFace.getOffsetY() * 0.5, bestFace.getOffsetZ() * 0.5)
            );
            this.silentSwapping = true;
            Managers.ROTATION.setRotationSilent(rot[0], rot[1]);
            this.silentSwapping = false;
         }

         SpeedmineModule speedmine = SpeedmineModule.getInstance();
         if (speedmine != null && speedmine.isEnabled()) {
            speedmine.clickMine(new SpeedmineModule.MiningData(pos, bestFace));
            this.miningMap.putIfAbsent(pos, bestFace);
            this.lastMinePos = pos;
         } else {
            if (!this.miningMap.containsKey(pos)) {
               Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, pos, bestFace));
               this.miningMap.put(pos, bestFace);
            }

            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, pos, bestFace));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            this.lastMinePos = pos;
         }
      }
   }

   private void abortAll() {
      for (Entry<BlockPos, Direction> e : this.miningMap.entrySet()) {
         Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, e.getKey(), e.getValue()));
      }

      this.miningMap.clear();
   }

   private List<BlockPos> getBlocksToMine(PlayerEntity target) {
      List<BlockPos> candidates = new ArrayList<>();
      BlockPos feet = target.getBlockPos();
      if (this.burrow.getValue() && this.isBurrowed(target, feet) && this.isValid(feet)) {
         candidates.add(feet);
      }

      if (this.surround.getValue()) {
         List<BlockPos> sides = new ArrayList<>();

         for (Direction dir : Type.HORIZONTAL) {
            BlockPos side = feet.offset(dir);
            if (this.isValid(side)) {
               sides.add(side);
            }
         }

         sides.sort(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p.toCenterPos())));
         candidates.addAll(sides);
      }

      BlockPos under = feet.down();
      if (this.under.getValue() && this.isValid(under) && !candidates.contains(under)) {
         candidates.add(under);
      }

      double maxDistSq = this.mineRange.getValue() * this.mineRange.getValue();
      return candidates.stream().filter(p -> mc.player.squaredDistanceTo(p.toCenterPos()) <= maxDistSq).collect(Collectors.toList());
   }

   private boolean isStillMinable(BlockPos pos) {
      return mc.world.getBlockState(pos).isAir()
         ? false
         : mc.player.squaredDistanceTo(pos.toCenterPos()) <= this.mineRange.getValue() * this.mineRange.getValue();
   }

   private boolean isBurrowed(PlayerEntity player, BlockPos feet) {
      return !mc.world.getBlockState(feet).isAir() && player.getBoundingBox().intersects(new Box(feet));
   }

   private boolean isValid(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      if (state.isAir()) {
         return false;
      } else {
         Block block = state.getBlock();
         if (ALWAYS_SKIP.contains(block)) {
            return false;
         } else if (state.getHardness(mc.world, pos) < 0.0F) {
            return false;
         } else {
            return switch ((AutoMineModule.SelectionMode)(Object)this.selection.getValue()) {
               case ANY -> true;
               case WHITELIST -> this.whitelist.getValue().contains(block);
               case BLACKLIST -> !this.blacklist.getValue().contains(block);
            };
         }
      }
   }

   private Direction getBestFace(BlockPos pos) {
      Direction best = Direction.UP;
      double min = Double.MAX_VALUE;

      for (Direction d : Direction.values()) {
         double dist = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add(d.getOffsetX() * 0.5, d.getOffsetY() * 0.5, d.getOffsetZ() * 0.5));
         if (dist < min) {
            min = dist;
            best = d;
         }
      }

      return best;
   }

   private PlayerEntity getBestTarget() {
      double r = this.targetRange.getValue().floatValue();
      return mc.world
         .getPlayers()
         .stream()
         .filter(p -> p != mc.player)
         .filter(p -> !p.isSpectator() && !p.isDead())
         .filter(p -> !Managers.SOCIAL.isFriend(p.getName().getString()))
         .filter(p -> mc.player.squaredDistanceTo(p) <= r * r)
         .min(this.getComparator())
         .orElse(null);
   }

   private Comparator<PlayerEntity> getComparator() {
      return switch ((AutoMineModule.SortMode)(Object)this.sort.getValue()) {
         case LOWEST_HEALTH -> Comparator.comparingDouble(p -> p.getHealth() + p.getAbsorptionAmount());
         case CLOSEST_ANGLE -> Comparator.comparingDouble(this::getAngleTo);
         default -> Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p));
      };
   }

   private double getAngleTo(PlayerEntity target) {
      float[] rot = RotationUtil.getRotationsTo(mc.player.getEyePos(), target.getEyePos());
      float deltaYaw = Math.abs(rot[0] - mc.player.getYaw()) % 360.0F;
      if (deltaYaw > 180.0F) {
         deltaYaw = 360.0F - deltaYaw;
      }

      return deltaYaw + Math.abs(rot[1] - mc.player.getPitch());
   }

   public static enum SelectionMode {
      ANY,
      WHITELIST,
      BLACKLIST;
   }

   public static enum SortMode {
      LOWEST_DISTANCE,
      LOWEST_HEALTH,
      CLOSEST_ANGLE;
   }
}
