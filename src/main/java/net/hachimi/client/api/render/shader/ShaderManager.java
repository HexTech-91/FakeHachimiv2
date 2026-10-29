package net.hachimi.client.api.render.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.hachimi.client.api.render.satin.ManagedShaderEffect;
import net.hachimi.client.api.render.satin.ShaderEffectManager;
import net.hachimi.client.impl.imixin.IPostEffectProcessor;
import net.hachimi.client.mixin.accessor.AccessorMultiPhase;
import net.hachimi.client.mixin.accessor.AccessorMultiPhaseParameters;
import net.hachimi.client.mixin.accessor.AccessorTextureBase;
import net.hachimi.client.util.Globals;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhase;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderLayer.OutlineMode;
import net.minecraft.client.render.RenderPhase.Target;
import net.minecraft.client.render.RenderPhase.TextureBase;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;

public class ShaderManager implements Globals {
   private final OutlineVertexConsumerProvider vertexConsumerProvider = new OutlineVertexConsumerProvider(
      VertexConsumerProvider.immediate(new BufferAllocator(256))
   );
   private final Function<TextureBase, RenderLayer> layerCreator;
   private final Target target = new Target("shader_target", () -> {}, () -> {});
   private ShaderFramebuffer framebuffer;
   public ManagedShaderEffect filledShaderEffect;
   public ManagedShaderEffect gradientShaderEffect;
   public ManagedShaderEffect imageShaderEffect;
   public ManagedShaderEffect glowingShaderEffect;
   public ManagedShaderEffect rainbowShaderEffect;
   public ManagedShaderEffect bloomShaderEffect;
   public ManagedShaderEffect menuShaderEffect;
   public ManagedShaderEffect blurShaderEffect;

   public ShaderManager() {
      this.layerCreator = this.memoizeTexture(
         texture -> RenderLayer.of(
            "hachimi_overlay",
            VertexFormats.POSITION_TEXTURE_COLOR,
            DrawMode.QUADS,
            1536,
            MultiPhaseParameters.builder()
               .program(RenderPhase.OUTLINE_PROGRAM)
               .cull(RenderPhase.DISABLE_CULLING)
               .texture(texture)
               .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
               .target(this.target)
               .build(OutlineMode.IS_OUTLINE)
         )
      );
   }

   public void reloadShaders() {
      if (this.framebuffer == null) {
         this.framebuffer = new ShaderFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
      }

      if (this.filledShaderEffect == null) {
         this.filledShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/outline.json"));
      }

      if (this.gradientShaderEffect == null) {
         this.gradientShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/gradient.json"));
      }

      if (this.imageShaderEffect == null) {
         this.imageShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/image.json"));
      }

      if (this.glowingShaderEffect == null) {
         this.glowingShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/glowing.json"));
      }

      if (this.rainbowShaderEffect == null) {
         this.rainbowShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/rainbow.json"));
      }

      if (this.bloomShaderEffect == null) {
         this.bloomShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/bloom.json"));
      }

      if (this.menuShaderEffect == null) {
         this.menuShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/menu.json"));
      }

      if (this.blurShaderEffect == null) {
         this.blurShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/blur.json"));
      }
   }

   public void reloadShadersInternal() {
      this.framebuffer = new ShaderFramebuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
      this.filledShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/outline.json"));
      this.gradientShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/gradient.json"));
      this.imageShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/image.json"));
      this.glowingShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/glowing.json"));
      this.rainbowShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/rainbow.json"));
      this.bloomShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/bloom.json"));
      this.menuShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/menu.json"));
      this.blurShaderEffect = ShaderEffectManager.getInstance().manage(Identifier.of("hachimiclient", "shaders/post/blur.json"));
   }

   public void applyShader(ManagedShaderEffect shaderEffect, Runnable setup, Runnable runnable) {
      Framebuffer mcFramebuffer = mc.getFramebuffer();
      RenderSystem.assertOnRenderThreadOrInit();
      if (this.framebuffer.textureWidth != mcFramebuffer.textureWidth || this.framebuffer.textureHeight != mcFramebuffer.textureHeight) {
         this.framebuffer.resize(mcFramebuffer.textureWidth, mcFramebuffer.textureHeight, false);
      }

      GlStateManager._glBindFramebuffer(36009, this.framebuffer.fbo);
      this.framebuffer.beginWrite(false);
      runnable.run();
      this.framebuffer.endWrite();
      GlStateManager._glBindFramebuffer(36009, mcFramebuffer.fbo);
      mcFramebuffer.beginWrite(false);
      PostEffectProcessor effect = shaderEffect.getShaderEffect();
      if (effect != null) {
         ((IPostEffectProcessor)effect).overwriteBuffer("bufIn", this.framebuffer);
         Framebuffer bufOut = effect.getSecondaryTarget("bufOut");
         setup.run();
         this.framebuffer.clear(false);
         mcFramebuffer.beginWrite(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ZERO, DstFactor.ONE);
         RenderSystem.backupProjectionMatrix();
         bufOut.draw(bufOut.textureWidth, bufOut.textureHeight, false);
         RenderSystem.restoreProjectionMatrix();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   public VertexConsumerProvider createVertexConsumers(VertexConsumerProvider parent, Color color) {
      return layer -> {
         VertexConsumer parentBuffer = parent.getBuffer(layer);
         if (layer instanceof MultiPhase
            && ((AccessorMultiPhaseParameters)(Object)((AccessorMultiPhase)layer).hookGetPhases()).hookGetOutlineMode() != OutlineMode.NONE) {
            this.vertexConsumerProvider.setColor(color.getRed(), color.getGreen(), color.getBlue(), 255);
            VertexConsumer outlineBuffer = this.vertexConsumerProvider
               .getBuffer(this.layerCreator.apply(((AccessorMultiPhaseParameters)(Object)((AccessorMultiPhase)layer).hookGetPhases()).hookGetTexture()));
            return outlineBuffer != null ? outlineBuffer : parentBuffer;
         } else {
            return parentBuffer;
         }
      };
   }

   private Function<TextureBase, RenderLayer> memoizeTexture(Function<TextureBase, RenderLayer> function) {
      return new Function<TextureBase, RenderLayer>() {
         private final Map<Identifier, RenderLayer> cache = new HashMap<>();

         public RenderLayer apply(TextureBase texture) {
            return this.cache.computeIfAbsent(((AccessorTextureBase)texture).hookGetId().get(), id -> function.apply(texture));
         }
      };
   }

   public ManagedShaderEffect getFilledShaderEffect() {
      return this.filledShaderEffect;
   }

   public ManagedShaderEffect getGradientShaderEffect() {
      return this.gradientShaderEffect;
   }

   public ManagedShaderEffect getImageShaderEffect() {
      return this.imageShaderEffect;
   }

   public ManagedShaderEffect getGlowingShaderEffect() {
      return this.glowingShaderEffect;
   }

   public ManagedShaderEffect getRainbowShaderEffect() {
      return this.rainbowShaderEffect;
   }

   public ManagedShaderEffect getBloomShaderEffect() {
      return this.bloomShaderEffect;
   }

   public ManagedShaderEffect getMenuShaderEffect() {
      return this.menuShaderEffect;
   }

   public ManagedShaderEffect getBlurShaderEffect() {
      return this.blurShaderEffect;
   }
}
