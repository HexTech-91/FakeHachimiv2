package net.hachimi.client.impl.font;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.hachimi.client.mixin.accessor.AccessorTextRenderer;
import net.hachimi.client.util.Globals;
import net.minecraft.client.font.EmptyGlyphRenderer;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TrueTypeFontLoader;
import net.minecraft.client.font.Font.FontFilterPair;
import net.minecraft.client.font.FontFilterType.FilterMap;
import net.minecraft.client.font.FontLoader.Loadable;
import net.minecraft.client.font.GlyphRenderer.Rectangle;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.font.TrueTypeFontLoader.Shift;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@Deprecated
public class VanillaTextRenderer implements Globals {
   public static TextRenderer getTextRender(String fontName) throws IOException {
      List<FontFilterPair> list = Lists.newArrayList();
      TrueTypeFontLoader loader = new TrueTypeFontLoader(Identifier.of(String.format("hachimi:%s.ttf", fontName)), 11.0F, 20.0F, Shift.NONE, "");
      Loadable loadable = loader.build().orThrow();
      Font font = loadable.load(mc.getResourceManager());
      list.add(new FontFilterPair(font, FilterMap.NO_FILTER));
      FontStorage storage = new FontStorage(mc.getTextureManager(), Identifier.of("hachimi:tr"));
      storage.setFonts(list, Collections.emptySet());
      return new TextRenderer(id -> storage, true);
   }

   public void drawWithShadow(MatrixStack matrices, String text, float x, float y, int color) {
      this.draw(matrices, text, x + 1.0F, y + 1.0F, color, true);
      this.draw(matrices, text, x, y, color, false);
   }

   public void draw(MatrixStack matrices, String text, float x, float y, int color, boolean shadow) {
      this.draw(text, x, y, color, matrices.peek().getPositionMatrix(), shadow);
   }

   private void draw(String text, float x, float y, int color, Matrix4f matrix, boolean shadow) {
      if (text != null) {
         Immediate immediate = VertexConsumerProvider.immediate(new BufferAllocator(1536));
         this.draw(text, x, y, color, shadow, matrix, immediate, TextLayerType.NORMAL, 0, 15728880);
         immediate.draw();
      }
   }

   public void draw(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      TextLayerType layerType,
      int backgroundColor,
      int light
   ) {
      this.drawInternal(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light);
   }

   private void drawInternal(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      TextLayerType layerType,
      int backgroundColor,
      int light
   ) {
      Matrix4f matrix4f = new Matrix4f(matrix);
      this.drawLayer(text, x, y, color, shadow, matrix4f, vertexConsumers, layerType, backgroundColor, light);
   }

   private void drawLayer(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumerProvider,
      TextLayerType layerType,
      int underlineColor,
      int light
   ) {
      VanillaTextRenderer.Drawer drawer = new VanillaTextRenderer.Drawer(vertexConsumerProvider, x, y, color, shadow, matrix, layerType, light);
      TextVisitFactory.visitFormatted(text, Style.EMPTY, drawer);
      drawer.drawLayer();
   }

   public static class Drawer implements CharacterVisitor {
      final VertexConsumerProvider vertexConsumers;
      private final float brightnessMultiplier;
      private final float red;
      private final float green;
      private final float blue;
      private final float alpha;
      private final Matrix4f matrix;
      private final TextLayerType layerType;
      private final int light;
      float x;
      float y;
      @Nullable
      private List<Rectangle> rectangles;

      public Drawer(VertexConsumerProvider vertexConsumers, float x, float y, int color, boolean shadow, Matrix4f matrix, TextLayerType layerType, int light) {
         this.vertexConsumers = vertexConsumers;
         this.x = x;
         this.y = y;
         this.brightnessMultiplier = shadow ? 0.25F : 1.0F;
         this.red = (color >> 16 & 0xFF) / 255.0F * this.brightnessMultiplier;
         this.green = (color >> 8 & 0xFF) / 255.0F * this.brightnessMultiplier;
         this.blue = (color & 0xFF) / 255.0F * this.brightnessMultiplier;
         this.alpha = (color >> 24 & 0xFF) / 255.0F;
         this.matrix = matrix;
         this.layerType = layerType;
         this.light = light;
      }

      @Override
      public boolean accept(int i, Style style, int j) {
         FontStorage fontStorage = ((AccessorTextRenderer)Globals.mc.textRenderer).hookGetFontStorage(style.getFont());
         Glyph glyph = fontStorage.getGlyph(j, ((AccessorTextRenderer)Globals.mc.textRenderer).hookGetValidateAdvance());
         GlyphRenderer glyphRenderer = style.isObfuscated() && j != 32 ? fontStorage.getObfuscatedGlyphRenderer(glyph) : fontStorage.getGlyphRenderer(j);
         boolean bl = style.isBold();
         float f = this.alpha;
         TextColor textColor = style.getColor();
         float l;
         float h;
         float g;
         if (textColor != null) {
            int k = textColor.getRgb();
            g = (k >> 16 & 0xFF) / 255.0F * this.brightnessMultiplier;
            h = (k >> 8 & 0xFF) / 255.0F * this.brightnessMultiplier;
            l = (k & 0xFF) / 255.0F * this.brightnessMultiplier;
         } else {
            g = this.red;
            h = this.green;
            l = this.blue;
         }

         if (!(glyphRenderer instanceof EmptyGlyphRenderer)) {
            float m = bl ? glyph.getBoldOffset() : 0.0F;
            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(glyphRenderer.getLayer(this.layerType));
            ((AccessorTextRenderer)Globals.mc.textRenderer)
               .hookDrawGlyph(glyphRenderer, bl, style.isItalic(), m, this.x, this.y, this.matrix, vertexConsumer, g, h, l, f, this.light);
         }

         float m = glyph.getAdvance(bl);
         this.x += m;
         return true;
      }

      public void drawLayer() {
         if (this.rectangles != null) {
            GlyphRenderer glyphRenderer = ((AccessorTextRenderer)Globals.mc.textRenderer).hookGetFontStorage(Style.DEFAULT_FONT_ID).getRectangleRenderer();
            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(glyphRenderer.getLayer(this.layerType));

            for (Rectangle rectangle : this.rectangles) {
               glyphRenderer.drawRectangle(rectangle, this.matrix, vertexConsumer, this.light);
            }
         }
      }
   }
}
