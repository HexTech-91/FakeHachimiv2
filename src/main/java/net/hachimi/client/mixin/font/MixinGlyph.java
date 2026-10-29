package net.hachimi.client.mixin.font;

import net.hachimi.client.impl.module.client.FontModule;
import net.minecraft.client.font.Glyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Glyph.class})
public interface MixinGlyph {
   @Inject(
      method = {"getShadowOffset"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookGetShadowOffset(CallbackInfoReturnable<Float> cir) {
      cir.cancel();
      cir.setReturnValue(FontModule.getInstance().getVanillaShadow());
   }
}
