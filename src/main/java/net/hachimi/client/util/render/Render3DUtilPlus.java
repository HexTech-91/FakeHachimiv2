package net.hachimi.client.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.util.Globals;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Render3DUtilPlus implements Globals {
   public static final long INIT_TIME = System.currentTimeMillis();

   public static void endBuilding(BufferBuilder bb) {
      BuiltBuffer built = bb.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }
   }

   public static MatrixStack matrixFrom(double x, double y, double z) {
      MatrixStack matrices = new MatrixStack();
      Camera camera = mc.gameRenderer.getCamera();
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);
      return matrices;
   }

   public static float getTickDelta() {
      return mc.getRenderTickCounter().getTickDelta(true);
   }

   public static void drawText3D(String text, Vec3d pos, Color color) {
      drawText3D(Text.of(text), pos.x, pos.y, pos.z, 0.0, 0.0, 1.0, color.getRGB());
   }

   public static void drawText3D(String text, Vec3d pos, int color) {
      drawText3D(Text.of(text), pos.x, pos.y, pos.z, 0.0, 0.0, 1.0, color);
   }

   public static void drawText3D(Text text, Vec3d pos, double offX, double offY, double scale, Color color) {
      drawText3D(text, pos.x, pos.y, pos.z, offX, offY, scale, color.getRGB());
   }

   public static void drawText3D(Text text, double x, double y, double z, double offX, double offY, double scale, int color) {
      RenderSystem.disableDepthTest();
      MatrixStack matrices = matrixFrom(x, y, z);
      Camera camera = mc.gameRenderer.getCamera();
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      RenderSystem.enableBlend();
      matrices.translate(offX, offY, 0.0);
      matrices.scale(-0.025F * (float)scale, -0.025F * (float)scale, 1.0F);
      int halfWidth = mc.textRenderer.getWidth(text) / 2;
      Immediate immediate = VertexConsumerProvider.immediate(new BufferAllocator(1536));
      mc.textRenderer
         .drawLayer(
            text.getString(), (float)(-halfWidth), 0.0F, -1, true, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880
         );
      immediate.draw();
      mc.textRenderer
         .draw(text.copy(), (float)(-halfWidth), 0.0F, color, false, matrices.peek().getPositionMatrix(), immediate, TextLayerType.SEE_THROUGH, 0, 15728880);
      immediate.draw();
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
   }

   public static void drawFill(MatrixStack matrices, Box box, Color color) {
      RenderManager.renderBox(matrices, box, color.getRGB());
   }

   public static void drawBox(MatrixStack matrices, Box box, Color color) {
      RenderManager.renderBoundingBox(matrices, box, 1.5F, color.getRGB());
   }

   public static void drawBox(MatrixStack matrices, Box box, Color color, float lineWidth) {
      RenderManager.renderBoundingBox(matrices, box, lineWidth, color.getRGB());
   }

   public static void drawFadeFill(MatrixStack stack, Box box, Color c, Color c1) {
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
      float minX = (float)(box.minX - cam.x);
      float maxX = (float)(box.maxX - cam.x);
      float minY = (float)(box.minY - cam.y);
      float maxY = (float)(box.maxY - cam.y);
      float minZ = (float)(box.minZ - cam.z);
      float maxZ = (float)(box.maxZ - cam.z);
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      Matrix4f m = stack.peek().getPositionMatrix();
      buf.vertex(m, minX, minY, minZ).color(c.getRGB());
      buf.vertex(m, maxX, minY, minZ).color(c.getRGB());
      buf.vertex(m, maxX, minY, maxZ).color(c.getRGB());
      buf.vertex(m, minX, minY, maxZ).color(c.getRGB());
      buf.vertex(m, minX, minY, minZ).color(c.getRGB());
      buf.vertex(m, minX, maxY, minZ).color(c1.getRGB());
      buf.vertex(m, maxX, maxY, minZ).color(c1.getRGB());
      buf.vertex(m, maxX, minY, minZ).color(c.getRGB());
      buf.vertex(m, maxX, minY, minZ).color(c.getRGB());
      buf.vertex(m, maxX, maxY, minZ).color(c1.getRGB());
      buf.vertex(m, maxX, maxY, maxZ).color(c1.getRGB());
      buf.vertex(m, maxX, minY, maxZ).color(c.getRGB());
      buf.vertex(m, minX, minY, maxZ).color(c.getRGB());
      buf.vertex(m, maxX, minY, maxZ).color(c.getRGB());
      buf.vertex(m, maxX, maxY, maxZ).color(c1.getRGB());
      buf.vertex(m, minX, maxY, maxZ).color(c1.getRGB());
      buf.vertex(m, minX, minY, minZ).color(c.getRGB());
      buf.vertex(m, minX, minY, maxZ).color(c.getRGB());
      buf.vertex(m, minX, maxY, maxZ).color(c1.getRGB());
      buf.vertex(m, minX, maxY, minZ).color(c1.getRGB());
      buf.vertex(m, minX, maxY, minZ).color(c1.getRGB());
      buf.vertex(m, minX, maxY, maxZ).color(c1.getRGB());
      buf.vertex(m, maxX, maxY, maxZ).color(c1.getRGB());
      buf.vertex(m, maxX, maxY, minZ).color(c1.getRGB());
      endBuilding(buf);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void drawLine(Vec3d start, Vec3d end, Color color) {
      RenderManager.renderLine(new MatrixStack(), start, end, 1.0F, color.getRGB());
   }

   public static void drawLine(Vec3d start, Vec3d end, Color color, float width) {
      RenderManager.renderLine(new MatrixStack(), start, end, width, color.getRGB());
   }

   public static void vertexLine(MatrixStack matrices, VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
      Matrix4f model = matrices.peek().getPositionMatrix();
      Entry entry = matrices.peek();
      Vector3f normal = getNormal((float)x1, (float)y1, (float)z1, (float)x2, (float)y2, (float)z2);
      buffer.vertex(model, (float)x1, (float)y1, (float)z1)
         .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
         .normal(entry, normal.x(), normal.y(), normal.z());
      buffer.vertex(model, (float)x2, (float)y2, (float)z2)
         .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
         .normal(entry, normal.x(), normal.y(), normal.z());
   }

   public static Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float dz = z2 - z1;
      float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
      return new Vector3f(dx / len, dy / len, dz / len);
   }

   public static void drawTargetEsp(MatrixStack stack, Entity target, Color color) {
      ArrayList<Vec3d> v0 = new ArrayList<>();
      ArrayList<Vec3d> v1 = new ArrayList<>();
      ArrayList<Vec3d> v2 = new ArrayList<>();
      double x = target.prevX + (target.getX() - target.prevX) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
      double y = target.prevY + (target.getY() - target.prevY) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY();
      double z = target.prevZ + (target.getZ() - target.prevZ) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();
      double height = target.getHeight();

      for (int i = 0; i <= 361; i++) {
         double sin = Math.sin(Math.toRadians(i));
         double cos = Math.cos(Math.toRadians(i));
         v0.add(new Vec3d(cos * 0.5, height, sin * 0.5));
         double sin1 = Math.sin(Math.toRadians((i + 120) % 360));
         double cos1 = Math.cos(Math.toRadians(i + 120) % 360.0);
         v1.add(new Vec3d(cos1 * 0.5, height, sin1 * 0.5));
         double sin2 = Math.sin(Math.toRadians((i + 240) % 360));
         double cos2 = Math.cos(Math.toRadians((i + 240) % 360));
         v2.add(new Vec3d(cos2 * 0.5, height, sin2 * 0.5));
         height -= 0.004F;
      }

      stack.push();
      stack.translate(x, y, z);
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      Matrix4f matrix = stack.peek().getPositionMatrix();
      float timeOffset = (float)(System.currentTimeMillis() - INIT_TIME) / 5.0F;
      BufferBuilder buf = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int j = 0; j < v0.size() - 1; j++) {
         float alpha = 1.0F - (j + timeOffset) % 360.0F / 60.0F;
         int pulsed = ColorUtilPlus.injectAlpha(ColorUtilPlus.pulseColor(color, (int)(j / 20.0F), 10, 1.0).getRGB(), (int)(alpha * 255.0F));
         buf.vertex(matrix, (float)v0.get(j).x, (float)v0.get(j).y, (float)v0.get(j).z).color(pulsed);
         buf.vertex(matrix, (float)v0.get(j + 1).x, (float)v0.get(j + 1).y + 0.1F, (float)v0.get(j + 1).z)
            .color(ColorUtilPlus.injectAlpha(ColorUtilPlus.pulseColor(color, (int)(j / 20.0F), 10, 1.0).getRGB(), (int)(alpha * 255.0F)));
      }

      endBuilding(buf);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      buf = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int j = 0; j < v1.size() - 1; j++) {
         float alpha = 1.0F - (j + timeOffset) % 360.0F / 60.0F;
         buf.vertex(matrix, (float)v1.get(j).x, (float)v1.get(j).y, (float)v1.get(j).z)
            .color(ColorUtilPlus.injectAlpha(ColorUtilPlus.pulseColor(color, (int)(j / 20.0F), 10, 1.0).getRGB(), (int)(alpha * 255.0F)));
         buf.vertex(matrix, (float)v1.get(j + 1).x, (float)v1.get(j + 1).y + 0.1F, (float)v1.get(j + 1).z)
            .color(ColorUtilPlus.injectAlpha(ColorUtilPlus.pulseColor(color, (int)(j / 20.0F), 10, 1.0).getRGB(), (int)(alpha * 255.0F)));
      }

      endBuilding(buf);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      buf = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

      for (int j = 0; j < v2.size() - 1; j++) {
         float alpha = 1.0F - (j + timeOffset) % 360.0F / 60.0F;
         buf.vertex(matrix, (float)v2.get(j).x, (float)v2.get(j).y, (float)v2.get(j).z)
            .color(ColorUtilPlus.injectAlpha(ColorUtilPlus.pulseColor(color, (int)(j / 20.0F), 10, 1.0).getRGB(), (int)(alpha * 255.0F)));
         buf.vertex(matrix, (float)v2.get(j + 1).x, (float)v2.get(j + 1).y + 0.1F, (float)v2.get(j + 1).z)
            .color(ColorUtilPlus.injectAlpha(ColorUtilPlus.pulseColor(color, (int)(j / 20.0F), 10, 1.0).getRGB(), (int)(alpha * 255.0F)));
      }

      endBuilding(buf);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      stack.pop();
   }
}
