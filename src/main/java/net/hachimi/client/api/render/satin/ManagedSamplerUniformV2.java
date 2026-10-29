package net.hachimi.client.api.render.satin;

import java.util.function.IntSupplier;
import net.hachimi.client.api.render.satin.uniform.SamplerUniformV2;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.JsonEffectShaderProgram;
import net.minecraft.client.texture.AbstractTexture;

public final class ManagedSamplerUniformV2 extends ManagedSamplerUniformBase implements SamplerUniformV2 {
   public ManagedSamplerUniformV2(String name) {
      super(name);
   }

   @Override
   public void set(AbstractTexture texture) {
      this.set(texture::getGlId);
   }

   @Override
   public void set(Framebuffer textureFbo) {
      this.set(textureFbo::getColorAttachment);
   }

   @Override
   public void set(int textureName) {
      this.set(() -> textureName);
   }

   @Override
   protected void set(Object value) {
      this.set((IntSupplier)value);
   }

   @Override
   public void set(IntSupplier value) {
      SamplerAccess[] targets = this.targets;
      if (targets.length > 0 && this.cachedValue != value) {
         for (SamplerAccess target : targets) {
            ((JsonEffectShaderProgram)target).bindSampler(this.name, value);
         }

         this.cachedValue = value;
      }
   }
}
