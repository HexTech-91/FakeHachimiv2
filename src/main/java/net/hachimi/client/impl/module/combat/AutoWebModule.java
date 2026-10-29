package net.hachimi.client.impl.module.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.BlockPlacerModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class AutoWebModule extends BlockPlacerModule {
   private static AutoWebModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("PlaceRange", "The range to fill nearby holes", 0.1F, 4.0F, 6.0F));
   Config<Float> enemyRangeConfig = this.register(new NumberConfig<>("EnemyRange", "The maximum range of targets", 0.1F, 10.0F, 15.0F));
   Config<Boolean> rotateConfig = this.register(new BooleanConfig("Rotate", "Rotates to block before placing", false));
   Config<Boolean> coverHeadConfig = this.register(new BooleanConfig("CoverHead", "Places webs on the targets head", false));
   Config<Integer> shiftTicksConfig = this.register(new NumberConfig<>("ShiftTicks", "The number of blocks to place per tick", 1, 2, 5));
   Config<Integer> shiftDelayConfig = this.register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0, 1, 5));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders web placements", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private int shiftDelay;
   private List<BlockPos> webs = new ArrayList<>();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();

   public AutoWebModule() {
      super("AutoWeb", "Automatically traps nearby entities in webs", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static AutoWebModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onDisable() {
      this.fadeList.clear();
      this.webs.clear();
   }

   @EventListener
   public void onDisconnect(DisconnectEvent event) {
      this.disable();
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!this.multitaskConfig.getValue() && this.checkMultitask()) {
         this.webs.clear();
      } else {
         int blocksPlaced = 0;
         int slot = this.getBlockItemSlot(Blocks.COBWEB);
         if (slot == -1) {
            this.webs.clear();
         } else if (this.shiftDelay < this.shiftDelayConfig.getValue()) {
            this.shiftDelay++;
         } else {
            List<BlockPos> webPlacements = new ArrayList<>();

            for (PlayerEntity entity : mc.world.getPlayers()) {
               if (entity != mc.player && !Managers.SOCIAL.isFriend(entity.getName())) {
                  double d = mc.player.distanceTo(entity);
                  if (!(d > this.enemyRangeConfig.getValue().floatValue())) {
                     BlockPos feetPos = entity.getBlockPos();
                     double dist = mc.player.getEyePos().squaredDistanceTo(feetPos.toCenterPos());
                     if (mc.world.getBlockState(feetPos).isAir() && dist <= ((NumberConfig)(Object)this.rangeConfig).getValueSq()) {
                        webPlacements.add(feetPos);
                     }

                     if (this.coverHeadConfig.getValue()) {
                        BlockPos headPos = feetPos.up();
                        double dist2 = mc.player.getEyePos().squaredDistanceTo(headPos.toCenterPos());
                        if (mc.world.getBlockState(headPos).isAir() && dist2 <= ((NumberConfig)(Object)this.rangeConfig).getValueSq()) {
                           webPlacements.add(headPos);
                        }
                     }
                  }
               }
            }

            this.webs = webPlacements;
            if (!this.webs.isEmpty()) {
               while (blocksPlaced < this.shiftTicksConfig.getValue() && blocksPlaced < this.webs.size()) {
                  BlockPos targetPos = this.webs.get(blocksPlaced);
                  blocksPlaced++;
                  this.shiftDelay = 0;
                  this.placeWeb(targetPos, slot);
               }

               if (this.rotateConfig.getValue()) {
                  Managers.ROTATION.setRotationSilentSync();
               }
            }
         }
      }
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (this.renderConfig.getValue()) {
         RenderBuffers.preRender();

         for (Entry<BlockPos, Animation> set : this.fadeList.entrySet()) {
            set.getValue().setState(false);
            int lineAlpha = (int)(120.0 * set.getValue().getFactor());
            Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
            BlockPos blockPos = set.getKey();
            double x1 = blockPos.getX();
            double y1 = blockPos.getY();
            double z1 = blockPos.getZ();
            double x2 = blockPos.getX() + 1.0;
            double y2 = blockPos.getY() + 1.0;
            double z2 = blockPos.getZ() + 1.0;
            RenderManager.renderPlane(event.getMatrices(), x1, y1, z1, x2, y2, z2, lineColor.getRGB());
            RenderManager.renderPlane(event.getMatrices(), x2, y1, z1, x1, y2, z2, lineColor.getRGB());
         }

         RenderBuffers.postRender();
         if (this.webs.isEmpty()) {
            return;
         }

         for (BlockPos pos : this.webs) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(pos, animation);
         }
      }

      this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
   }

   private void placeWeb(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock(pos, slot, this.strictDirectionConfig.getValue(), false, (state, angles) -> {
         if (this.rotateConfig.getValue() && state) {
            Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
         }
      });
   }

   public boolean isPlacing() {
      return !this.webs.isEmpty();
   }
}
