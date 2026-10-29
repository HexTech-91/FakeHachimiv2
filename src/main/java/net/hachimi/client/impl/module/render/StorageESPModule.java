package net.hachimi.client.impl.module.render;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderBuffers;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.util.world.BlockUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class StorageESPModule extends ToggleModule {
   private static StorageESPModule INSTANCE;
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The ESP render range", 10.0F, 50.0F, 200.0F));
   Config<Boolean> fillConfig = this.register(new BooleanConfig("Fill", "Fills in the highlight", false));
   Config<Float> widthConfig = this.register(new NumberConfig<>("Width", "The line width of the highlight", 1.0F, 1.5F, 5.0F));
   Config<Boolean> chestsConfig = this.register(new BooleanConfig("Chests", "Render players through walls", true));
   Config<Color> chestsColorConfig = this.register(
      new ColorConfig("ChestsColor", "The render color for chests", new Color(200, 200, 101), false, false, () -> this.chestsConfig.getValue())
   );
   Config<Boolean> echestsConfig = this.register(new BooleanConfig("EnderChests", "Render players through walls", true));
   Config<Color> echestsColorConfig = this.register(
      new ColorConfig("EnderChestsColor", "The render color for ender chests", new Color(155, 0, 200), false, false, () -> this.echestsConfig.getValue())
   );
   Config<Boolean> shulkersConfig = this.register(new BooleanConfig("Shulkers", "Render players through walls", true));
   Config<Color> shulkersColorConfig = this.register(
      new ColorConfig("ShulkersColor", "The render color for shulkers", new Color(200, 0, 106), false, false, () -> this.shulkersConfig.getValue())
   );
   Config<Boolean> hoppersConfig = this.register(new BooleanConfig("Hoppers", "Render players through walls", false));
   Config<Color> hoppersColorConfig = this.register(
      new ColorConfig("HoppersColor", "The render color for hoppers", new Color(100, 100, 100), false, false, () -> this.hoppersConfig.getValue())
   );
   Config<Boolean> furnacesConfig = this.register(new BooleanConfig("Furnaces", "Render players through walls", false));
   Config<Color> furnacesColorConfig = this.register(
      new ColorConfig("FurnacesColor", "The render color for furnaces", new Color(100, 100, 100), false, false, () -> this.furnacesConfig.getValue())
   );

   public StorageESPModule() {
      super("StorageESP", "Highlights containers in the world", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static StorageESPModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      RenderBuffers.preRender();

      for (BlockEntity blockEntity : BlockUtil.blockEntities()) {
         double dist = mc.player.squaredDistanceTo(blockEntity.getPos().toCenterPos());
         if (!(dist > ((NumberConfig)(Object)this.rangeConfig).getValueSq())) {
            Color color = this.getStorageESPColor(blockEntity);
            if (color != null) {
               Vec3d vec3d = blockEntity.getPos().toCenterPos();
               double alpha = (100.0 - mc.player.squaredDistanceTo(vec3d)) / 100.0;
               alpha = 1.0 - MathHelper.clamp(alpha, 0.0, 1.0);
               BlockPos blockPos = blockEntity.getPos();
               if (blockEntity instanceof ChestBlockEntity) {
                  double x1 = blockPos.getX() + 0.06;
                  double y1 = blockPos.getY();
                  double z1 = blockPos.getZ() + 0.06;
                  double x2 = blockPos.getX() + 0.94;
                  double y2 = blockPos.getY() + 0.875;
                  double z2 = blockPos.getZ() + 0.94;
                  BlockState state = blockEntity.getCachedState();
                  if (state.contains(ChestBlock.CHEST_TYPE)) {
                     Direction direction = state.get(ChestBlock.FACING);
                     ChestType type = state.get(ChestBlock.CHEST_TYPE);
                     if (type == ChestType.RIGHT) {
                        direction = direction.rotateYCounterclockwise();
                        if (direction.getOffsetX() < 0) {
                           x1 += direction.getOffsetX();
                        } else {
                           x2 += direction.getOffsetX();
                        }

                        if (direction.getOffsetZ() < 0) {
                           z1 += direction.getOffsetZ();
                        } else {
                           z2 += direction.getOffsetZ();
                        }

                        int fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(30.0 * alpha)).getRGB();
                        int outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(72.0 * alpha)).getRGB();
                        Box chestBox = new Box(x1, y1, z1, x2, y2, z2);
                        if (this.fillConfig.getValue()) {
                           RenderManager.renderBox(event.getMatrices(), chestBox, fillColor);
                        }

                        RenderManager.renderBoundingBox(event.getMatrices(), chestBox, this.widthConfig.getValue(), outlineColor);
                     } else if (type == ChestType.LEFT) {
                        direction = direction.rotateYClockwise();
                        if (direction.getOffsetX() < 0) {
                           x1 += direction.getOffsetX();
                        } else {
                           x2 += direction.getOffsetX();
                        }

                        if (direction.getOffsetZ() < 0) {
                           z1 += direction.getOffsetZ();
                        } else {
                           z2 += direction.getOffsetZ();
                        }

                        int fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(30.0 * alpha)).getRGB();
                        int outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(72.0 * alpha)).getRGB();
                        Box chestBox = new Box(x1, y1, z1, x2, y2, z2);
                        if (this.fillConfig.getValue()) {
                           RenderManager.renderBox(event.getMatrices(), chestBox, fillColor);
                        }

                        RenderManager.renderBoundingBox(event.getMatrices(), chestBox, this.widthConfig.getValue(), outlineColor);
                     } else if (type == ChestType.SINGLE) {
                        int fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(60.0 * alpha)).getRGB();
                        int outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(145.0 * alpha)).getRGB();
                        Box chestBox = new Box(x1, y1, z1, x2, y2, z2);
                        if (this.fillConfig.getValue()) {
                           RenderManager.renderBox(event.getMatrices(), chestBox, fillColor);
                        }

                        RenderManager.renderBoundingBox(event.getMatrices(), chestBox, this.widthConfig.getValue(), outlineColor);
                     }
                  }
               } else if (blockEntity instanceof EnderChestBlockEntity) {
                  double x1 = blockPos.getX() + 0.06;
                  double y1 = blockPos.getY();
                  double z1 = blockPos.getZ() + 0.06;
                  double x2 = blockPos.getX() + 0.94;
                  double y2 = blockPos.getY() + 0.875;
                  double z2 = blockPos.getZ() + 0.94;
                  int fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(60.0 * alpha)).getRGB();
                  int outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(145.0 * alpha)).getRGB();
                  Box chestBox = new Box(x1, y1, z1, x2, y2, z2);
                  if (this.fillConfig.getValue()) {
                     RenderManager.renderBox(event.getMatrices(), chestBox, fillColor);
                  }

                  RenderManager.renderBoundingBox(event.getMatrices(), chestBox, this.widthConfig.getValue(), outlineColor);
               } else {
                  int fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(60.0 * alpha)).getRGB();
                  int outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(145.0 * alpha)).getRGB();
                  if (this.fillConfig.getValue()) {
                     RenderManager.renderBox(event.getMatrices(), blockPos, fillColor);
                  }

                  RenderManager.renderBoundingBox(event.getMatrices(), blockPos, this.widthConfig.getValue(), outlineColor);
               }
            }
         }
      }

      RenderBuffers.postRender();
   }

   public Color getStorageESPColor(BlockEntity tileEntity) {
      if (tileEntity instanceof ChestBlockEntity chestBlockEntity) {
         return this.chestsColorConfig.getValue();
      } else if (tileEntity instanceof EnderChestBlockEntity) {
         return this.echestsColorConfig.getValue();
      } else if (tileEntity instanceof ShulkerBoxBlockEntity) {
         return this.shulkersColorConfig.getValue();
      } else if (tileEntity instanceof HopperBlockEntity) {
         return this.hoppersColorConfig.getValue();
      } else {
         return tileEntity instanceof FurnaceBlockEntity ? this.furnacesColorConfig.getValue() : null;
      }
   }
}
