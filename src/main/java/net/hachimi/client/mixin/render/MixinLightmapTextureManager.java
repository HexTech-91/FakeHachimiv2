package net.hachimi.client.mixin.render;

import java.awt.Color;
import net.hachimi.client.impl.event.render.AmbientColorEvent;
import net.hachimi.client.impl.event.render.LightmapGammaEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({LightmapTextureManager.class})
public class MixinLightmapTextureManager {
   @Shadow
   @Final
   private NativeImage image;

   @ModifyArgs(
      method = {"update"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/texture/NativeImage;setColor(III)V"
      )
   )
   private void hookUpdate(Args args) {
      LightmapGammaEvent lightmapGammaEvent = new LightmapGammaEvent(args.<Integer>get(2));
      EventBus.INSTANCE.dispatch(lightmapGammaEvent);
      if (lightmapGammaEvent.isCanceled()) {
         args.set(2, lightmapGammaEvent.getGamma());
      }
   }

   @Inject(
      method = {"update"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/texture/NativeImageBackedTexture;upload()V",
         shift = Shift.BEFORE
      )}
   )
   private void hookUpdate(float delta, CallbackInfo ci) {
      AmbientColorEvent ambientColorEvent = new AmbientColorEvent();
      EventBus.INSTANCE.dispatch(ambientColorEvent);
      Color c = ambientColorEvent.getColor();
      if (ambientColorEvent.isCanceled()) {
         for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
               int r = c.getRed();
               int g = c.getGreen();
               int b = c.getBlue();
               this.image.setColor(i, j, 0xFF000000 | b << 16 | g << 8 | r);
            }
         }
      }
   }
}
