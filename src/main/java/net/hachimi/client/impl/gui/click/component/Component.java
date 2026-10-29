package net.hachimi.client.impl.gui.click.component;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.BiConsumer;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.gui.click.ClickGuiScreen;
import net.hachimi.client.impl.module.client.ClickGuiModule;
import net.hachimi.client.util.Globals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.ColorHelper.Argb;
import org.joml.Matrix4f;

public abstract class Component implements Drawable, Globals {
   protected float x;
   protected float y;
   protected float width;
   protected float height;

   @Override
   public abstract void render(DrawContext var1, float var2, float var3, float var4);

   protected void drawStringScaled(DrawContext context, String string, float x, float y, int color) {
      context.getMatrices().scale(ClickGuiModule.CLICK_GUI_SCALE, ClickGuiModule.CLICK_GUI_SCALE, 0.0F);
      float j = 1.0F / ClickGuiModule.CLICK_GUI_SCALE;
      RenderManager.renderText(context, string, x * j, y * j, color);
      context.getMatrices().scale(j, j, 0.0F);
   }

   protected void rect(DrawContext context, int color) {
      this.fill(context, this.x, this.y, this.width * ClickGuiModule.CLICK_GUI_SCALE, this.height * ClickGuiModule.CLICK_GUI_SCALE, color);
   }

   protected void rectGradient(DrawContext context, int color1, int color2) {
      this.fillGradient(context, this.x, this.y, this.width * ClickGuiModule.CLICK_GUI_SCALE, this.height * ClickGuiModule.CLICK_GUI_SCALE, color1, color2);
   }

   protected void scale(DrawContext context, float scale) {
   }

   protected void drawRoundedRect(DrawContext context, double x1, double y1, double x2, double y2, int color) {
      this.drawRoundedRect(context, x1, y1, x2, y2, 0.0, color);
   }

   protected void drawRoundedRect(DrawContext context, double x1, double y1, double x2, double y2, double z, int color) {
      this.fill(context, x1, y1, x2, y2, z, color);
   }

   protected void drawCircle(DrawContext context, double x, double y, double radius, int color) {
      this.drawCircle(context, x, y, 0.0, radius, color);
   }

   protected void drawCircle(DrawContext context, double x, double y, double z, double radius, int color) {
   }

   protected void drawHorizontalLine(DrawContext context, double x1, double x2, double y, int color) {
      if (x2 < x1) {
         double i = x1;
         x1 = x2;
         x2 = i;
      }

      this.fill(context, x1, y, x1 + x2 + 1.0, y + 1.0, color);
   }

   protected void drawVerticalLine(DrawContext context, double x, double y1, double y2, int color) {
      if (y2 < y1) {
         double i = y1;
         y1 = y2;
         y2 = i;
      }

      this.fill(context, x, y1 + 1.0, x + 1.0, y1 + y2, color);
   }

   public void fill(DrawContext context, double x1, double y1, double x2, double y2, int color) {
      this.fill(context, x1, y1, x2, y2, 0.0, color);
   }

   public void fill(DrawContext context, double x1, double y1, double x2, double y2, double z, int color) {
      x2 += x1;
      y2 += y1;
      Matrix4f matrix4f = context.getMatrices().peek().getPositionMatrix();
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
      if (RenderSystem.getShader() != GameRenderer.getPositionColorProgram()) {
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      }

      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix4f, (float)x1, (float)y1, (float)z).color(g, h, j, f);
      buffer.vertex(matrix4f, (float)x1, (float)y2, (float)z).color(g, h, j, f);
      buffer.vertex(matrix4f, (float)x2, (float)y2, (float)z).color(g, h, j, f);
      buffer.vertex(matrix4f, (float)x2, (float)y1, (float)z).color(g, h, j, f);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   protected void fillGradient(DrawContext context, double startX, double startY, double endX, double endY, int colorStart, int colorEnd) {
      this.fillGradient(context, startX, startY, endX, endY, colorStart, colorEnd, 0);
   }

   protected void fillGradient(DrawContext context, double startX, double startY, double endX, double endY, int colorStart, int colorEnd, int z) {
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      this.fillGradient(context.getMatrices().peek().getPositionMatrix(), buffer, startX, startY, endX, endY, z, colorStart, colorEnd);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   protected void fillGradient(Matrix4f matrix, BufferBuilder builder, double x1, double y1, double x2, double y2, double z, int colorStart, int colorEnd) {
      x2 += x1;
      y2 += y1;
      if (x1 < x2) {
         double i2 = x1;
         x1 = x2;
         x2 = i2;
      }

      if (y1 < y2) {
         double i2 = y1;
         y1 = y2;
         y2 = i2;
      }

      float f = Argb.getAlpha(colorStart) / 255.0F;
      float g = Argb.getRed(colorStart) / 255.0F;
      float h = Argb.getGreen(colorStart) / 255.0F;
      float i = Argb.getBlue(colorStart) / 255.0F;
      float j = Argb.getAlpha(colorEnd) / 255.0F;
      float k = Argb.getRed(colorEnd) / 255.0F;
      float l = Argb.getGreen(colorEnd) / 255.0F;
      float m = Argb.getBlue(colorEnd) / 255.0F;
      builder.vertex(matrix, (float)x1, (float)y1, (float)z).color(k, l, m, j);
      builder.vertex(matrix, (float)x1, (float)y2, (float)z).color(k, l, m, j);
      builder.vertex(matrix, (float)x2, (float)y2, (float)z).color(g, h, i, f);
      builder.vertex(matrix, (float)x2, (float)y1, (float)z).color(g, h, i, f);
   }

   protected void fillGradientQuad(DrawContext context, float x1, float y1, float x2, float y2, int startColor, int endColor, boolean sideways) {
      float f = (startColor >> 24 & 0xFF) / 255.0F;
      float f1 = (startColor >> 16 & 0xFF) / 255.0F;
      float f2 = (startColor >> 8 & 0xFF) / 255.0F;
      float f3 = (startColor & 0xFF) / 255.0F;
      float f4 = (endColor >> 24 & 0xFF) / 255.0F;
      float f5 = (endColor >> 16 & 0xFF) / 255.0F;
      float f6 = (endColor >> 8 & 0xFF) / 255.0F;
      float f7 = (endColor & 0xFF) / 255.0F;
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      Matrix4f posMatrix = context.getMatrices().peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
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

   public void drawWithOutline(int x, int y, BiConsumer<Integer, Integer> renderAction) {
      RenderSystem.blendFuncSeparate(SrcFactor.ZERO, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
      renderAction.accept(x + 1, y);
      renderAction.accept(x - 1, y);
      renderAction.accept(x, y + 1);
      renderAction.accept(x, y - 1);
      RenderSystem.defaultBlendFunc();
      renderAction.accept(x, y);
   }

   public void drawSprite(DrawContext context, int x, int y, int z, int width, int height, Sprite sprite) {
      this.drawTexturedQuad(
         context.getMatrices().peek().getPositionMatrix(),
         x,
         x + width,
         y,
         y + height,
         z,
         sprite.getMinU(),
         sprite.getMaxU(),
         sprite.getMinV(),
         sprite.getMaxV()
      );
   }

   public void drawSprite(DrawContext context, int x, int y, int z, int width, int height, Sprite sprite, float red, float green, float blue, float alpha) {
      this.drawTexturedQuad(
         context.getMatrices().peek().getPositionMatrix(),
         x,
         x + width,
         y,
         y + height,
         z,
         sprite.getMinU(),
         sprite.getMaxU(),
         sprite.getMinV(),
         sprite.getMaxV(),
         red,
         green,
         blue,
         alpha
      );
   }

   public void drawBorder(DrawContext context, double x, double y, double width, double height, int color) {
      this.fill(context, x, y, x + width, y + 1.0, color);
      this.fill(context, x, y + height - 1.0, x + width, y + height, color);
      this.fill(context, x, y + 1.0, x + 1.0, y + height - 1.0, color);
      this.fill(context, x + width - 1.0, y + 1.0, x + width, y + height - 1.0, color);
   }

   public void drawTexture(DrawContext context, int x, int y, int u, int v, int width, int height) {
      this.drawTexture(context, x, y, 0, u, v, width, height, 256, 256);
   }

   public void drawTexture(DrawContext context, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
      this.drawTexture(context, x, x + width, y, y + height, z, width, height, u, v, textureWidth, textureHeight);
   }

   public void drawTexture(
      DrawContext context,
      float x,
      float y,
      float width,
      float height,
      float u,
      float v,
      float regionWidth,
      float regionHeight,
      float textureWidth,
      float textureHeight
   ) {
      this.drawTexture(context, x, x + width, y, y + height, 0.0F, regionWidth, regionHeight, u, v, textureWidth, textureHeight);
   }

   public void drawTexture(DrawContext context, float x, float y, float u, float v, float width, float height, float textureWidth, float textureHeight) {
      this.drawTexture(context, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
   }

   private void drawTexture(
      DrawContext context,
      float x0,
      float x1,
      float y0,
      float y1,
      float z,
      float regionWidth,
      float regionHeight,
      float u,
      float v,
      float textureWidth,
      float textureHeight
   ) {
      this.drawTexturedQuad(
         context.getMatrices().peek().getPositionMatrix(),
         x0,
         x1,
         y0,
         y1,
         z,
         (u + 0.0F) / textureWidth,
         (u + regionWidth) / textureWidth,
         (v + 0.0F) / textureHeight,
         (v + regionHeight) / textureHeight
      );
   }

   public void drawTexturedQuad(Matrix4f matrix, float x0, float x1, float y0, float y1, float z, float u0, float u1, float v0, float v1) {
      RenderSystem.setShader(GameRenderer::getPositionTexProgram);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      buffer.vertex(matrix, x0, y0, z).texture(u0, v0);
      buffer.vertex(matrix, x0, y1, z).texture(u0, v1);
      buffer.vertex(matrix, x1, y1, z).texture(u1, v1);
      buffer.vertex(matrix, x1, y0, z).texture(u1, v0);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }

   public void drawTexturedQuad(
      Matrix4f matrix, float x0, float x1, float y0, float y1, float z, float u0, float u1, float v0, float v1, float red, float green, float blue, float alpha
   ) {
      RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
      RenderSystem.enableBlend();
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      buffer.vertex(matrix, x0, y0, z).color(red, green, blue, alpha).texture(u0, v0);
      buffer.vertex(matrix, x0, y1, z).color(red, green, blue, alpha).texture(u0, v1);
      buffer.vertex(matrix, x1, y1, z).color(red, green, blue, alpha).texture(u1, v1);
      buffer.vertex(matrix, x1, y0, z).color(red, green, blue, alpha).texture(u1, v0);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.disableBlend();
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getHeight() {
      return this.height * ClickGuiModule.CLICK_GUI_SCALE;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public float getWidth() {
      return this.width * ClickGuiModule.CLICK_GUI_SCALE;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public boolean isWithin(double xval, double yval) {
      return this.isWithin((float)xval, (float)yval);
   }

   public boolean isWithin(float xval, float yval) {
      return this.isMouseOver(xval, yval, this.x, this.y, this.width * ClickGuiModule.CLICK_GUI_SCALE, this.height * ClickGuiModule.CLICK_GUI_SCALE);
   }

   public boolean isMouseOver(double mx, double my, double x1, double y1, double x2, double y2) {
      return mx >= x1 && mx <= x1 + x2 && my >= y1 && my <= y1 + y2;
   }

   public void setPos(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public void setDimensions(float width, float height) {
      this.setWidth(width);
      this.setHeight(height);
   }

   public void enableScissor(double x1, double y1, double x2, double y2) {
      x1 = Math.floor(x1);
      y1 = Math.floor(y1);
      x2 = Math.ceil(x2);
      y2 = Math.ceil(y2);
      this.setScissor(ClickGuiScreen.SCISSOR_STACK.push(new ScreenRect((int)x1, (int)y1, (int)(x2 - x1), (int)(y2 - y1))));
   }

   public void disableScissor() {
      this.setScissor(ClickGuiScreen.SCISSOR_STACK.pop());
   }

   private void setScissor(ScreenRect rect) {
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
}
