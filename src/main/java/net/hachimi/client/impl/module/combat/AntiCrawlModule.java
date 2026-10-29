package net.hachimi.client.impl.module.combat;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.module.player.SpeedmineModule;
import net.hachimi.client.util.world.BlockUtilPlus;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class AntiCrawlModule extends ToggleModule {
   private static AntiCrawlModule INSTANCE;
   private static final double[] XZ_OFFSETS = new double[]{0.0, 0.3, -0.3};
   Config<AntiCrawlModule.While> whileConfig = this.register(
      new EnumConfig<>("While", "When to break the block above you", AntiCrawlModule.While.CRAWLING, AntiCrawlModule.While.values())
   );
   Config<Boolean> webConfig = this.register(new BooleanConfig("Web", "Also breaks cobwebs at your feet", true));
   private boolean working;

   public AntiCrawlModule() {
      super("AntiCrawl", "Breaks the block above you to stand back up from crawling", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AntiCrawlModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.working = false;
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && event.getStage() == StageEvent.EventStage.PRE) {
         this.working = false;
         if (!mc.player.isFallFlying()) {
            if (switch ((AntiCrawlModule.While)(Object)this.whileConfig.getValue()) {
               case CRAWLING -> mc.player.isCrawling();
               case MINING -> mc.player.isCrawling() || SpeedmineModule.getInstance().isMining();
               case ALWAYS -> mc.world.getBlockState(mc.player.getBlockPos()).getBlock() != Blocks.BEDROCK || mc.player.isCrawling();
               default -> false;
            }) {
               for (double offsetX : XZ_OFFSETS) {
                  for (double offsetZ : XZ_OFFSETS) {
                     BlockPos pos = BlockPos.ofFloored(mc.player.getX() + offsetX, mc.player.getY() + 1.2, mc.player.getZ() + offsetZ);
                     if (this.canBreak(pos)) {
                        this.mineBlock(pos);
                        this.working = true;
                        return;
                     }

                     if (this.webConfig.getValue()) {
                        BlockPos webPos = BlockPos.ofFloored(mc.player.getX() + offsetX, mc.player.getY(), mc.player.getZ() + offsetZ);
                        if (mc.world.getBlockState(webPos).getBlock() == Blocks.COBWEB && this.canBreak(webPos)) {
                           this.mineBlock(webPos);
                           this.working = true;
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void mineBlock(BlockPos pos) {
      Direction side = BlockUtilPlus.getClickSideStrict(pos);
      if (side == null) {
         side = Direction.UP;
      }

      SpeedmineModule.getInstance().clickMine(new SpeedmineModule.MiningData(pos, side));
   }

   private boolean canBreak(BlockPos pos) {
      if (mc.world.isAir(pos)) {
         return false;
      } else {
         return mc.world.getBlockState(pos).getHardness(mc.world, pos) == -1.0F ? false : BlockUtilPlus.getClickSideStrict(pos) != null;
      }
   }

   public boolean isWorking() {
      return this.working;
   }

   public static enum While {
      CRAWLING,
      MINING,
      ALWAYS;
   }
}
