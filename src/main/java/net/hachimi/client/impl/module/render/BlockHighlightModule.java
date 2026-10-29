package net.hachimi.client.impl.module.render;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.BoxRender;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.render.RenderBlockOutlineEvent;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public class BlockHighlightModule extends ToggleModule {
   Config<BoxRender> boxModeConfig = this.register(new EnumConfig<>("BoxMode", "Box rendering mode", BoxRender.OUTLINE, BoxRender.values()));
   Config<Boolean> entitiesConfig = this.register(new BooleanConfig("Debug-Entities", "Highlights entity bounding boxes for debug purposes", false));
   Config<Float> widthConfig = this.register(new NumberConfig<>("Width", "The line width of the highlight", 1.0F, 1.0F, 5.0F));
   Config<Boolean> fadeConfig = this.register(new BooleanConfig("Fade", "Fades the block highlight", false));
   Config<Integer> fadeTimeConfig = this.register(new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 200, 1000, () -> false));
   private double distance;
   private final Map<Box, Animation> fadeList = new HashMap<>();

   public BlockHighlightModule() {
      super("BlockHighlight", "Highlights the block the player is facing", ModuleCategory.RENDER);
   }

   @Override
   public String getModuleData() {
      if (mc.world == null) {
         return super.getModuleData();
      } else if (mc.crosshairTarget instanceof BlockHitResult result && mc.world.getBlockState(result.getBlockPos()).isAir()) {
         return super.getModuleData();
      } else {
         DecimalFormat decimal = new DecimalFormat("0.0");
         return decimal.format(this.distance);
      }
   }

   @Override
   public void onDisable() {
      this.fadeList.clear();
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.world != null) {
         if (!this.fadeConfig.getValue() && !this.fadeList.isEmpty()) {
            this.fadeList.clear();
         }

         Box render = null;
         HitResult result = mc.crosshairTarget;
         if (result != null) {
            Vec3d pos = Managers.POSITION.getEyePos();
            if (this.entitiesConfig.getValue() && result.getType() == Type.ENTITY) {
               Entity entity = ((EntityHitResult)result).getEntity();
               render = entity.getBoundingBox();
               this.distance = pos.distanceTo(entity.getPos());
               if (this.fadeConfig.getValue()) {
                  this.fadeList.put(render, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
               }
            } else if (result.getType() == Type.BLOCK) {
               BlockPos hpos = ((BlockHitResult)result).getBlockPos();
               BlockState state = mc.world.getBlockState(hpos);
               VoxelShape outlineShape = state.getOutlineShape(mc.world, hpos);
               if (outlineShape.isEmpty()) {
                  return;
               }

               Box render1 = outlineShape.getBoundingBox();
               render = new Box(
                  hpos.getX() + render1.minX,
                  hpos.getY() + render1.minY,
                  hpos.getZ() + render1.minZ,
                  hpos.getX() + render1.maxX,
                  hpos.getY() + render1.maxY,
                  hpos.getZ() + render1.maxZ
               );
               this.distance = pos.distanceTo(hpos.toCenterPos());
               if (this.fadeConfig.getValue()) {
                  this.fadeList.put(render, new Animation(true, this.fadeTimeConfig.getValue().intValue()));
               }
            }
         }

         RenderBuffers.preRender();
         if (this.fadeConfig.getValue()) {
            for (Entry<Box, Animation> set : this.fadeList.entrySet()) {
               Box box = set.getKey();
               set.getValue().setState(false);
               if (!(set.getValue().getFactor() < 0.01F)) {
                  int boxAlpha = (int)(40.0 * set.getValue().getFactor());
                  int lineAlpha = (int)(145.0 * set.getValue().getFactor());
                  this.renderBb(event.getMatrices(), box, ColorsModule.getInstance().getRGB(boxAlpha), ColorsModule.getInstance().getRGB(lineAlpha));
               }
            }
         } else if (render != null) {
            this.renderBb(event.getMatrices(), render, ColorsModule.getInstance().getRGB(40), ColorsModule.getInstance().getRGB(145));
         }

         RenderBuffers.postRender();
      }
   }

   private void renderBb(MatrixStack matrixStack, Box render, int color, int lineColor) {
      switch ((BoxRender)(Object)this.boxModeConfig.getValue()) {
         case FILL:
            RenderManager.renderBox(matrixStack, render, color);
            RenderManager.renderBoundingBox(matrixStack, render, this.widthConfig.getValue(), lineColor);
            break;
         case OUTLINE:
            RenderManager.renderBoundingBox(matrixStack, render, this.widthConfig.getValue(), lineColor);
      }
   }

   @EventListener
   public void onRenderBlockOutline(RenderBlockOutlineEvent event) {
      event.cancel();
   }
}
