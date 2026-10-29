package net.hachimi.client.api.render.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;

public class ShaderFramebuffer extends Framebuffer {
   public ShaderFramebuffer(int width, int height) {
      super(false);
      RenderSystem.assertOnRenderThreadOrInit();
      this.resize(width, height, true);
      this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
   }
}
