package net.hachimi.client.mixin.gl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hachimi.client.impl.imixin.IPostEffectProcessor;
import net.hachimi.client.mixin.accessor.AccessorPostEffectPass;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PostEffectProcessor.class})
public class MixinPostEffectProcessor implements IPostEffectProcessor {
   @Shadow
   @Final
   private List<PostEffectPass> passes;
   @Shadow
   @Final
   private Map<String, Framebuffer> targetsByName;
   @Unique
   private final List<String> bufferTargets = new CopyOnWriteArrayList<>();

   @Override
   public void overwriteBuffer(String name, Framebuffer buffer) {
      Framebuffer target = this.targetsByName.get(name);
      if (target != buffer) {
         if (target != null) {
            for (PostEffectPass pass : this.passes) {
               if (pass.input == target) {
                  ((AccessorPostEffectPass)pass).hookSetInput(buffer);
               }

               if (pass.output == target) {
                  ((AccessorPostEffectPass)pass).hookSetOutput(buffer);
               }
            }

            this.targetsByName.remove(name);
            this.bufferTargets.remove(name);
         }

         this.targetsByName.put(name, buffer);
         this.bufferTargets.add(name);
      }
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")}
   )
   private void hookClose(CallbackInfo ci) {
      for (String fakedBufferName : this.bufferTargets) {
         this.targetsByName.remove(fakedBufferName);
      }
   }
}
