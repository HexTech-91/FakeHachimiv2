package net.hachimi.client.mixin.gui.screen;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SplashTextRenderer.class})
public class MixinSplashTextRenderer {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hachimi$hideSplashText(CallbackInfo ci) {
      ci.cancel();
   }
}
