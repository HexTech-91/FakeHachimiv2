package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.module.misc.KitModule;
import net.hachimi.client.impl.module.player.SpeedmineModule;
import net.hachimi.client.util.world.BlockUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class AntiRegearModule extends ToggleModule {
   private static AntiRegearModule INSTANCE;
   Config<Float> safeRangeConfig = this.register(new NumberConfig<>("SafeRange", "Shulker boxes within this range of you are ignored", 0.0F, 2.0F, 8.0F));
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "Maximum range to destroy enemy shulker boxes", 0.0F, 5.0F, 8.0F));
   Config<Boolean> checkSelfConfig = this.register(new BooleanConfig("CheckSelf", "Ignore shulker boxes placed by your own KitModule", true));

   public AntiRegearModule() {
      super("AntiRegear", "Destroys nearby enemy shulker boxes using Speedmine", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AntiRegearModule getInstance() {
      return INSTANCE;
   }

   @Override
   public String getModuleData() {
      SpeedmineModule sm = SpeedmineModule.getInstance();
      return sm != null && sm.isEnabled() ? "Mining" : "Ready";
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (event.getStage() == StageEvent.EventStage.PRE) {
            ShulkerBoxBlockEntity target = this.findTarget();
            if (target == null) {
               this.disableSpeedmine();
            } else {
               BlockPos pos = target.getPos();
               SpeedmineModule sm = SpeedmineModule.getInstance();
               if (sm != null && !sm.isEnabled()) {
                  sm.enable();
               }

               Direction side = this.getBreakSide(pos);
               mc.interactionManager.attackBlock(pos, side);
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      this.disableSpeedmine();
   }

   private ShulkerBoxBlockEntity findTarget() {
      float safeRange = this.safeRangeConfig.getValue();
      float range = this.rangeConfig.getValue();

      for (BlockEntity entity : BlockUtil.blockEntities()) {
         if (entity instanceof ShulkerBoxBlockEntity shulker && mc.world.getBlockState(shulker.getPos()).getBlock() instanceof ShulkerBoxBlock) {
            float dist = MathHelper.sqrt((float)mc.player.squaredDistanceTo(shulker.getPos().toCenterPos()));
            if (!(dist <= safeRange)) {
               if (this.checkSelfConfig.getValue()) {
                  KitModule kit = KitModule.getInstance();
                  if (kit != null && kit.isOwnShulker(shulker.getPos())) {
                     continue;
                  }
               }

               if (dist <= range) {
                  return shulker;
               }
            }
         }
      }

      return null;
   }

   private Direction getBreakSide(BlockPos pos) {
      Direction best = Direction.UP;
      double bestDist = Double.MAX_VALUE;

      for (Direction dir : Direction.values()) {
         double d = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add(dir.getOffsetX() * 0.5, dir.getOffsetY() * 0.5, dir.getOffsetZ() * 0.5));
         if (d < bestDist) {
            bestDist = d;
            best = dir;
         }
      }

      return best;
   }

   private void disableSpeedmine() {
      SpeedmineModule sm = SpeedmineModule.getInstance();
      if (sm != null && sm.isEnabled()) {
         sm.disable();
      }
   }
}
