package net.hachimi.client.api.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.ColorHelper.Argb;
import org.joml.Matrix3f;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL11;

public class RenderBuffers {
   public static final RenderBuffers.Buffer QUADS = new RenderBuffers.Buffer(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
   public static final RenderBuffers.Buffer TEXTURE_QUADS = new RenderBuffers.Buffer(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
   public static final RenderBuffers.Buffer LINES = new RenderBuffers.Buffer(DrawMode.LINES, VertexFormats.LINES);
   private static final List<Runnable> postRenderCallbacks = new ArrayList<>();
   private static boolean isSetup = false;

   public static void preRender() {
      GL11.glEnable(2848);
      GL11.glHint(3154, 4354);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.disableDepthTest();
      isSetup = true;
   }

   public static void postRender() {
      isSetup = false;
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      GL11.glDisable(2848);

      for (Runnable callback : postRenderCallbacks) {
         callback.run();
      }

      postRenderCallbacks.clear();
   }

   public static void post(Runnable callback) {
      if (isSetup) {
         postRenderCallbacks.add(callback);
      } else {
         callback.run();
      }
   }

   private static Matrix4d toMatrix4d(Matrix4f matrix4f) {
      return new Matrix4d(
         matrix4f.m00(),
         matrix4f.m01(),
         matrix4f.m02(),
         matrix4f.m03(),
         matrix4f.m10(),
         matrix4f.m11(),
         matrix4f.m12(),
         matrix4f.m13(),
         matrix4f.m20(),
         matrix4f.m21(),
         matrix4f.m22(),
         matrix4f.m23(),
         matrix4f.m30(),
         matrix4f.m31(),
         matrix4f.m32(),
         matrix4f.m33()
      );
   }

   public static class Buffer {
      public BufferBuilder buffer;
      private final DrawMode drawMode;
      private final VertexFormat vertexFormat;
      private Matrix4d positionMatrix;
      private Matrix3f normalMatrix;
      private int color;

      public Buffer(DrawMode drawMode, VertexFormat vertexFormat) {
         this.drawMode = drawMode;
         this.vertexFormat = vertexFormat;
         this.color = -1;
      }

      public void begin(MatrixStack stack) {
         this.updateMatrices(stack);
         this.buffer = Tessellator.getInstance().begin(this.drawMode, this.vertexFormat);
      }

      public void updateMatrices(MatrixStack stack) {
         this.positionMatrix = RenderBuffers.toMatrix4d(stack.peek().getPositionMatrix());
         this.normalMatrix = stack.peek().getNormalMatrix();
         Vec3d pos = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().camera.getPos();
         this.positionMatrix.translate(-pos.x, -pos.y, -pos.z);
      }

      public void end() {
         this.draw();
      }

      public RenderBuffers.Buffer vertex(double x, double y, double z) {
         if (this.buffer == null) {
            return this;
         } else {
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x, y, z, 1.0));
            this.buffer
               .vertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z())
               .color(Argb.getRed(this.color), Argb.getGreen(this.color), Argb.getBlue(this.color), Argb.getAlpha(this.color));
            return this;
         }
      }

      public RenderBuffers.Buffer vertexTex(double x, double y, double z, float u, float v) {
         if (this.buffer == null) {
            return this;
         } else {
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x, y, z, 1.0));
            this.buffer
               .vertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z())
               .texture(u, v)
               .color(Argb.getRed(this.color), Argb.getGreen(this.color), Argb.getBlue(this.color), Argb.getAlpha(this.color));
            return this;
         }
      }

      public RenderBuffers.Buffer vertexLine(double x1, double y1, double z1, double x2, double y2, double z2) {
         if (this.buffer == null) {
            return this;
         } else {
            float k = (float)(x2 - x1);
            float l = (float)(y2 - y1);
            float m = (float)(z2 - z1);
            float n = MathHelper.sqrt(k * k + l * l + m * m);
            k /= n;
            l /= n;
            m /= n;
            Vector3f vector3f = this.normalMatrix.transform(k, l, m, new Vector3f()).normalize();
            Vector4d vector4d = this.positionMatrix.transform(new Vector4d(x1, y1, z1, 1.0));
            this.buffer
               .vertex((float)vector4d.x(), (float)vector4d.y(), (float)vector4d.z())
               .normal(vector3f.x, vector3f.y, vector3f.z)
               .color(Argb.getRed(this.color), Argb.getGreen(this.color), Argb.getBlue(this.color), Argb.getAlpha(this.color));
            Vector4d vector4d2 = this.positionMatrix.transform(new Vector4d(x2, y2, z2, 1.0));
            this.buffer
               .vertex((float)vector4d2.x(), (float)vector4d2.y(), (float)vector4d2.z())
               .normal(vector3f.x, vector3f.y, vector3f.z)
               .color(Argb.getRed(this.color), Argb.getGreen(this.color), Argb.getBlue(this.color), Argb.getAlpha(this.color));
            return this;
         }
      }

      public void color(int color) {
         this.color = color;
      }

      public void draw() {
         if (this.buffer != null) {
            if (this.vertexFormat == VertexFormats.LINES) {
               RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
            } else if (this.vertexFormat == VertexFormats.POSITION_COLOR) {
               RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            } else if (this.vertexFormat == VertexFormats.POSITION_TEXTURE) {
               RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            }

            BuiltBuffer builtBuffer = this.buffer.endNullable();
            if (builtBuffer != null) {
               BufferRenderer.drawWithGlobalProgram(builtBuffer);
               this.buffer = null;
            }
         }
      }
   }
}
