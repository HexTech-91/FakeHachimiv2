package net.hachimi.client.api.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.hachimi.client.impl.gui.click.component.ScissorStack;
import net.hachimi.client.impl.module.client.FontModule;
import net.hachimi.client.impl.module.render.NametagsModule;
import net.hachimi.client.init.Fonts;
import net.hachimi.client.mixin.accessor.AccessorTextRenderer;
import net.hachimi.client.mixin.accessor.AccessorWorldRenderer;
import net.hachimi.client.util.Globals;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.ColorHelper.Argb;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class RenderManager implements Globals {
   public static final Tessellator TESSELLATOR = RenderSystem.renderThreadTesselator();
   public static final ScissorStack SCISSOR_STACK = new ScissorStack();

   public static void post(Runnable callback) {
      RenderBuffers.post(callback);
   }

   public static void renderBox(MatrixStack matrices, BlockPos p, int color) {
      renderBox(matrices, new Box(p), color);
   }

   public static void renderBox(MatrixStack matrices, Box box, int color) {
      if (isFrustumVisible(box)) {
         matrices.push();
         drawBox(matrices, box, color);
         matrices.pop();
      }
   }

   public static void drawBox(MatrixStack matrices, Box box, int color) {
      drawBox(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
   }

   public static void drawBox(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      RenderBuffers.QUADS.begin(matrices);
      RenderBuffers.QUADS.color(color);
      RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x2, y1, z1).vertex(x2, y1, z2).vertex(x1, y1, z2);
      RenderBuffers.QUADS.vertex(x1, y2, z1).vertex(x1, y2, z2).vertex(x2, y2, z2).vertex(x2, y2, z1);
      RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z1).vertex(x2, y1, z1);
      RenderBuffers.QUADS.vertex(x2, y1, z1).vertex(x2, y2, z1).vertex(x2, y2, z2).vertex(x2, y1, z2);
      RenderBuffers.QUADS.vertex(x1, y1, z2).vertex(x2, y1, z2).vertex(x2, y2, z2).vertex(x1, y2, z2);
      RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y1, z2).vertex(x1, y2, z2).vertex(x1, y2, z1);
      RenderBuffers.QUADS.end();
   }

   public static void renderSide(MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, Direction direction, int color) {
      matrices.push();
      drawSide(matrices, x1, y1, z1, x2, y2, z2, direction, color);
      matrices.pop();
   }

   public static void renderSide(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, Direction direction, int color) {
      matrices.push();
      drawSide(matrices, x1, y1, z1, x2, y2, z2, direction, color);
      matrices.pop();
   }

   public static void drawSide(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, Direction direction, int color) {
      RenderBuffers.QUADS.begin(matrices);
      RenderBuffers.QUADS.color(color);
      if (direction.getAxis().isVertical()) {
         RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x2, y1, z1).vertex(x2, y1, z2).vertex(x1, y1, z2);
      } else if (direction != Direction.NORTH && direction != Direction.SOUTH) {
         RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y1, z2).vertex(x1, y2, z2).vertex(x1, y2, z1);
      } else {
         RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z1).vertex(x2, y1, z1);
      }

      RenderBuffers.QUADS.end();
   }

   public static void renderPlane(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      matrices.push();
      drawPlane(matrices, x1, y1, z1, x2, y2, z2, color);
      matrices.pop();
   }

   public static void drawPlane(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      RenderBuffers.QUADS.begin(matrices);
      RenderBuffers.QUADS.color(color);
      RenderBuffers.QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z2).vertex(x2, y1, z2);
      RenderBuffers.QUADS.end();
   }

   public static void renderBoundingCross(MatrixStack matrices, BlockPos p, float width, int color) {
      renderBoundingCross(matrices, new Box(p), width, color);
   }

   public static void renderBoundingCross(MatrixStack matrices, Box box, float width, int color) {
      if (isFrustumVisible(box)) {
         matrices.push();
         RenderSystem.lineWidth(width);
         drawBoundingCross(matrices, box, color);
         matrices.pop();
      }
   }

   public static void drawBoundingCross(MatrixStack matrices, Box box, int color) {
      drawBoundingCross(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
   }

   public static void drawBoundingCross(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      RenderBuffers.LINES.begin(matrices);
      RenderBuffers.LINES.color(color);
      RenderBuffers.LINES.vertexLine(x1, y1, z1, x2, y1, z2);
      RenderBuffers.LINES.vertexLine(x2, y1, z1, x1, y1, z2);
      RenderBuffers.LINES.end();
   }

   public static void renderBoundingBox(MatrixStack matrices, BlockPos p, float width, int color) {
      renderBoundingBox(matrices, new Box(p), width, color);
   }

   public static void renderBoundingBox(MatrixStack matrices, Box box, float width, int color) {
      if (isFrustumVisible(box)) {
         matrices.push();
         RenderSystem.lineWidth(width);
         drawBoundingBox(matrices, box, color);
         matrices.pop();
      }
   }

   public static void drawBoundingBox(MatrixStack matrices, Box box, int color) {
      drawBoundingBox(matrices, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
   }

   public static void drawBoundingBox(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      RenderBuffers.LINES.begin(matrices);
      RenderBuffers.LINES.color(color);
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      if (dy == 0.0) {
         RenderBuffers.LINES.vertexLine(x1, y1, z1, x2, y1, z1);
         RenderBuffers.LINES.vertexLine(x1, y1, z1, x1, y1, z2);
         RenderBuffers.LINES.vertexLine(x2, y1, z2, x1, y1, z2);
         RenderBuffers.LINES.vertexLine(x2, y1, z2, x2, y1, z1);
         RenderBuffers.LINES.end();
      } else {
         VoxelShapes.cuboid(0.0, 0.0, 0.0, dx, dy, dz)
            .forEachEdge(
               (minX, minY, minZ, maxX, maxY, maxZ) -> RenderBuffers.LINES.vertexLine(minX + x1, minY + y1, minZ + z1, maxX + x1, maxY + y1, maxZ + z1)
            );
         RenderBuffers.LINES.end();
      }
   }

   public static void renderLine(MatrixStack matrices, Vec3d s, Vec3d d, float width, int color) {
      renderLine(matrices, s.x, s.y, s.z, d.x, d.y, d.z, width, color);
   }

   public static void renderLine(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, float width, int color) {
      matrices.push();
      RenderSystem.lineWidth(width);
      drawLine(matrices, x1, y1, z1, x2, y2, z2, color);
      matrices.pop();
   }

   public static void drawLine(MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
      matrices.push();
      RenderBuffers.LINES.begin(matrices);
      RenderBuffers.LINES.color(color);
      RenderBuffers.LINES.vertexLine(x1, y1, z1, x2, y2, z2);
      RenderBuffers.LINES.end();
      matrices.pop();
   }

   public static void renderSign(String text, Vec3d pos, int color) {
      renderSign(text, pos.getX(), pos.getY(), pos.getZ(), color);
   }

   public static void renderSign(String text, Vec3d pos, float scaling, int color) {
      renderSign(text, pos.getX(), pos.getY(), pos.getZ(), scaling, color);
   }

   public static void renderSign(String text, double x, double y, double z, int color) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d pos = camera.getPos();
      double dist = Math.sqrt(pos.squaredDistanceTo(x, y, z));
      float scaling = (float)(0.0018F + NametagsModule.getInstance().getScaling() * dist);
      if (dist <= 8.0) {
         scaling = 0.0245F;
      }

      renderSign(text, x, y, z, scaling, color);
   }

   public static void renderSign(String text, double x, double y, double z, float scaling, int color) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d pos = camera.getPos();
      MatrixStack matrices = new MatrixStack();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - pos.getX(), y - pos.getY(), z - pos.getZ());
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.scale(-scaling, -scaling, -1.0F);
      float hwidth = mc.textRenderer.getWidth(text) / 2.0F;
      post(
         () -> {
            GL11.glDepthFunc(519);
            if (FontModule.getInstance().isEnabled()) {
               Fonts.CLIENT_UNSCALED.drawStringWithShadow(matrices, text, -hwidth, 0.0, color);
            } else {
               Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
               ((AccessorTextRenderer)mc.textRenderer)
                  .hookDrawLayer(
                     text,
                     -hwidth,
                     0.0F,
                     TextRenderer.tweakTransparency(color),
                     true,
                     matrices.peek().getPositionMatrix(),
                     vertexConsumers,
                     TextLayerType.SEE_THROUGH,
                     0,
                     15728880
                  );
               vertexConsumers.draw();
               ((AccessorTextRenderer)mc.textRenderer)
                  .hookDrawLayer(
                     text,
                     -hwidth,
                     0.0F,
                     TextRenderer.tweakTransparency(color),
                     false,
                     matrices.peek().getPositionMatrix(),
                     vertexConsumers,
                     TextLayerType.SEE_THROUGH,
                     0,
                     15728880
                  );
               vertexConsumers.draw();
            }

            GL11.glDepthFunc(515);
         }
      );
   }

   public static boolean isFrustumVisible(Box box) {
      return ((AccessorWorldRenderer)mc.worldRenderer).getFrustum().isVisible(box);
   }

   public static void rect(MatrixStack matrices, double x1, double y1, double x2, double y2, int color) {
      rect(matrices, x1, y1, x2, y2, 0.0, color);
   }

   public static void borderedRect(MatrixStack matrices, double x1, double y1, double x2, double y2, int borderColor, double thickness) {
      rect(matrices, x1 - thickness, y1 - thickness, thickness, y2 + thickness * 2.0, borderColor);
      rect(matrices, x1 + x2, y1 - thickness, thickness, y2 + thickness * 2.0, borderColor);
      rect(matrices, (float)x1, (float)(y1 - thickness - 1.0), (float)x2, (float)thickness, borderColor);
      rect(matrices, (float)x1, (float)(y1 + y2 + 1.0), (float)x2, (float)thickness, borderColor);
   }

   public static void borderedRectLine(MatrixStack matrices, double x1, double y1, double x2, double y2, int borderColor) {
      rectLine(matrices, x1, y1, 0.0, y2, borderColor);
      rectLine(matrices, x1 + x2, y1, 0.0, y2, borderColor);
      rectLine(matrices, x1, y1, x2, 0.0, borderColor);
      rectLine(matrices, x1, y1 + y2, x2, 0.0, borderColor);
   }

   public static void rect(MatrixStack matrices, double x1, double y1, double x2, double y2, double z, int color) {
      x2 += x1;
      y2 += y1;
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      if (x1 < x2) {
         double i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         double i = y1;
         y1 = y2;
         y2 = i;
      }

      float f = Argb.getAlpha(color) / 255.0F;
      float g = Argb.getRed(color) / 255.0F;
      float h = Argb.getGreen(color) / 255.0F;
      float j = Argb.getBlue(color) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = TESSELLATOR.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix4f, (float)x1, (float)y1, (float)z).color(g, h, j, f);
      bufferBuilder.vertex(matrix4f, (float)x1, (float)y2, (float)z).color(g, h, j, f);
      bufferBuilder.vertex(matrix4f, (float)x2, (float)y2, (float)z).color(g, h, j, f);
      bufferBuilder.vertex(matrix4f, (float)x2, (float)y1, (float)z).color(g, h, j, f);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void rectLine(MatrixStack matrices, double x1, double y1, double x2, double y2, int color) {
      x2 += x1;
      y2 += y1;
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      if (x1 < x2) {
         double i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         double i = y1;
         y1 = y2;
         y2 = i;
      }

      float f = Argb.getAlpha(color) / 255.0F;
      float g = Argb.getRed(color) / 255.0F;
      float h = Argb.getGreen(color) / 255.0F;
      float j = Argb.getBlue(color) / 255.0F;
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = TESSELLATOR.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      bufferBuilder.vertex(matrix4f, (float)x1, (float)y1, 0.0F).color(g, h, j, f);
      bufferBuilder.vertex(matrix4f, (float)x2, (float)y2, 0.0F).color(g, h, j, f);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void rectGradient(MatrixStack matrices, double x1, double y1, double x2, double y2, int color, int color1) {
      x2 += x1;
      y2 += y1;
      if (x1 < x2) {
         double i = x1;
         x1 = x2;
         x2 = i;
      }

      if (y1 < y2) {
         double i = y1;
         y1 = y2;
         y2 = i;
      }

      fillGradientQuad(matrices, (float)x1, (float)y1, (float)x2, (float)y2, color1, color, true);
   }

   public static void fillGradientQuad(DrawContext context, float x1, float y1, float x2, float y2, int startColor, int endColor, boolean sideways) {
      fillGradientQuad(context.getMatrices(), x1, y1, x2, y2, startColor, endColor, sideways);
   }

   public static void fillGradientQuad(MatrixStack matrixStack, float x1, float y1, float x2, float y2, int startColor, int endColor, boolean sideways) {
      float f = (startColor >> 24 & 0xFF) / 255.0F;
      float f1 = (startColor >> 16 & 0xFF) / 255.0F;
      float f2 = (startColor >> 8 & 0xFF) / 255.0F;
      float f3 = (startColor & 0xFF) / 255.0F;
      float f4 = (endColor >> 24 & 0xFF) / 255.0F;
      float f5 = (endColor >> 16 & 0xFF) / 255.0F;
      float f6 = (endColor >> 8 & 0xFF) / 255.0F;
      float f7 = (endColor & 0xFF) / 255.0F;
      Matrix4f posMatrix = matrixStack.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      if (sideways) {
         bufferBuilder.vertex(posMatrix, x1, y1, 0.0F).color(f1, f2, f3, f);
         bufferBuilder.vertex(posMatrix, x1, y2, 0.0F).color(f1, f2, f3, f);
         bufferBuilder.vertex(posMatrix, x2, y2, 0.0F).color(f5, f6, f7, f4);
         bufferBuilder.vertex(posMatrix, x2, y1, 0.0F).color(f5, f6, f7, f4);
      } else {
         bufferBuilder.vertex(posMatrix, x2, y1, 0.0F).color(f1, f2, f3, f);
         bufferBuilder.vertex(posMatrix, x1, y1, 0.0F).color(f1, f2, f3, f);
         bufferBuilder.vertex(posMatrix, x1, y2, 0.0F).color(f5, f6, f7, f4);
         bufferBuilder.vertex(posMatrix, x2, y2, 0.0F).color(f5, f6, f7, f4);
      }

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      RenderSystem.disableBlend();
   }

   public static void rectTextured(
      MatrixStack matrix,
      Identifier identifier,
      float x0,
      float x1,
      float y0,
      float y1,
      float z,
      float u0,
      float u1,
      float v0,
      float v1,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      RenderSystem.setShaderTexture(0, identifier);
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      RenderSystem.enableBlend();
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      buffer.vertex(matrix4f, x0, y0, z).color(red, green, blue, alpha).texture(u0, v0);
      buffer.vertex(matrix4f, x0, y1, z).color(red, green, blue, alpha).texture(u0, v1);
      buffer.vertex(matrix4f, x1, y1, z).color(red, green, blue, alpha).texture(u1, v1);
      buffer.vertex(matrix4f, x1, y0, z).color(red, green, blue, alpha).texture(u1, v0);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   public static void enableScissor(double x1, double y1, double x2, double y2) {
      x1 = Math.floor(x1);
      y1 = Math.floor(y1);
      x2 = Math.ceil(x2);
      y2 = Math.ceil(y2);
      setScissor(SCISSOR_STACK.push(new ScreenRect((int)x1, (int)y1, (int)(x2 - x1), (int)(y2 - y1))));
   }

   public static void disableScissor() {
      setScissor(SCISSOR_STACK.pop());
   }

   private static void setScissor(ScreenRect rect) {
      if (rect != null) {
         Window window = mc.getWindow();
         int i = window.getFramebufferHeight();
         double d = window.getScaleFactor();
         double e = rect.getLeft() * d;
         double f = i - rect.getBottom() * d;
         double g = rect.width() * d;
         double h = rect.height() * d;
         RenderSystem.enableScissor((int)e, (int)f, Math.max(0, (int)g), Math.max(0, (int)h));
      } else {
         RenderSystem.disableScissor();
      }
   }

   public static void renderText(DrawContext context, String text, float x, float y, int color) {
      if (FontModule.getInstance().isEnabled() && Fonts.CLIENT != null) {
         Fonts.CLIENT.drawStringWithShadow(context.getMatrices(), text, x, y, color);
      } else {
         context.drawText(mc.textRenderer, text, (int)x, (int)y, color, true);
      }
   }

   public static int textWidth(String text) {
      return FontModule.getInstance().isEnabled() && Fonts.CLIENT != null ? (int)Fonts.CLIENT.getStringWidth(text) : mc.textRenderer.getWidth(text);
   }

   public static int textHeight() {
      return FontModule.getInstance().isEnabled() && Fonts.CLIENT != null ? (int)Fonts.CLIENT.getFontHeight() : 9;
   }
}
