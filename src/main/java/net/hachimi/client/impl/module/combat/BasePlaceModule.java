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
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.ObsidianPlacerModule;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.impl.module.player.AirPlaceModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.entity.EntityUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.world.ExplosionUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class BasePlaceModule extends ObsidianPlacerModule {
   Config<Float> placeRangeConfig = this.register(new NumberConfig<>("PlaceRange", "The placement range for bases", 0.0F, 4.0F, 6.0F));
   Config<Float> enemyRangeConfig = this.register(new NumberConfig<>("EnemyRange", "The maximum range of targets", 0.1F, 10.0F, 15.0F));
   Config<Float> shiftDelayConfig = this.register(new NumberConfig<>("ShiftDelay", "The delay between each block placement interval", 0.0F, 1.0F, 5.0F));
   Config<Float> minDamageConfig = this.register(new NumberConfig<>("MinDamage", "Minimum damage required to place base", 1.0F, 4.0F, 10.0F));
   Config<Boolean> assumeArmorConfig = this.register(new BooleanConfig("AssumeBestArmor", "Assumes Prot 0 armor is max armor", false));
   Config<Boolean> renderConfig = this.register(new BooleanConfig("Render", "Renders where base blocks are being placed", true));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Time to fade", 0, 250, 1000, () -> false));
   private BlockPos crystalBase;
   private final Map<BlockPos, Long> packets = new HashMap<>();
   private final Map<BlockPos, Animation> fadeList = new HashMap<>();

   public BasePlaceModule() {
      super("BasePlace", "Places obsidian for crystal placements", ModuleCategory.COMBAT);
   }

   @Override
   public void onDisable() {
      this.crystalBase = null;
      this.packets.clear();
      this.fadeList.clear();
   }

   @EventListener
   public void onTick(PlayerTickEvent event) {
      if (AutoCrystalModule.getInstance().isEnabled() && !AutoCrystalModule.getInstance().isPlacing()) {
         PlayerEntity target = this.getClosestPlayer(this.enemyRangeConfig.getValue().floatValue());
         if (target != null) {
            this.crystalBase = this.getCrystalBase(target);
            if (this.crystalBase != null) {
               BlockState state = mc.world.getBlockState(this.crystalBase);
               int slot = this.getResistantBlockItem().slot();
               if (slot != -1 && state.isReplaceable()) {
                  this.placeBlock(this.crystalBase, slot);
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
            int boxAlpha = (int)(40.0 * set.getValue().getFactor());
            int lineAlpha = (int)(100.0 * set.getValue().getFactor());
            Color boxColor = ColorsModule.getInstance().getColor(boxAlpha);
            Color lineColor = ColorsModule.getInstance().getColor(lineAlpha);
            RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
            RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5F, lineColor.getRGB());
         }

         RenderBuffers.postRender();
         if (this.crystalBase != null && mc.world.isAir(this.crystalBase)) {
            Animation animation = new Animation(true, this.fadeTimeConfig.getValue().intValue());
            this.fadeList.put(this.crystalBase, animation);
         }
      }

      this.fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
   }

   private void placeBlock(BlockPos pos, int slot) {
      Managers.INTERACT.placeBlock(pos, slot, this.strictDirectionConfig.getValue(), false, true, (state, angles) -> {
         if (this.rotateConfig.getValue()) {
            if (state) {
               Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            } else {
               Managers.ROTATION.setRotationSilentSync();
            }
         }
      });
      this.packets.put(pos, System.currentTimeMillis());
   }

   private BlockPos getCrystalBase(PlayerEntity player) {
      List<BlockPos> targetBlocks = this.getSphere(this.placeRangeConfig.getValue().floatValue(), mc.player.getEyePos());
      double damage = 0.0;
      BlockPos crystalBase = null;

      for (BlockPos pos : targetBlocks) {
         BlockPos basePos = pos.down();
         if (basePos.getY() < EntityUtil.getRoundedBlockPos(player).getY()) {
            Long placed = this.packets.get(basePos);
            if ((
                  !(this.shiftDelayConfig.getValue() > 0.0F)
                     || placed == null
                     || !((float)(System.currentTimeMillis() - placed) < this.shiftDelayConfig.getValue() * 50.0F)
               )
               && AutoCrystalModule.getInstance().isCrystalHitboxClear(pos)) {
               double dist = mc.player.squaredDistanceTo(basePos.toCenterPos());
               if (!(dist > ((NumberConfig)(Object)this.placeRangeConfig).getValueSq())) {
                  double dmg1 = ExplosionUtil.getDamageTo(player, pos.toCenterPos(), this.assumeArmorConfig.getValue());
                  if (!(dmg1 < this.minDamageConfig.getValue().floatValue())
                     && (
                        AirPlaceModule.getInstance().isEnabled()
                           || Managers.INTERACT.getInteractDirectionInternal(basePos, this.strictDirectionConfig.getValue()) != null
                     )
                     && mc.world.canPlace(DEFAULT_OBSIDIAN_STATE, basePos, ShapeContext.absent())
                     && dmg1 > damage) {
                     crystalBase = basePos;
                     damage = dmg1;
                  }
               }
            }
         }
      }

      return crystalBase;
   }

   private List<BlockPos> getSphere(double rad, Vec3d origin) {
      List<BlockPos> sphere = new ArrayList<>();

      for (double x = -rad; x <= rad; x++) {
         for (double y = -rad; y <= rad; y++) {
            for (double z = -rad; z <= rad; z++) {
               Vec3i pos = new Vec3i((int)(origin.getX() + x), (int)(origin.getY() + y), (int)(origin.getZ() + z));
               BlockPos p = new BlockPos(pos);
               sphere.add(p);
            }
         }
      }

      return sphere;
   }
}
