package net.hachimi.client.mixin.gui.screen;

import java.lang.reflect.Field;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SplashOverlay.class})
public class MixinLoadingOverlay {
   @Unique
   private long startTimeMs = -1L;

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void hookRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      int sw = mc.getWindow().getScaledWidth();
      int sh = mc.getWindow().getScaledHeight();
      if (this.startTimeMs < 0L) {
         this.startTimeMs = System.currentTimeMillis();
      }

      float progress = this.getProgress();
      context.fill(0, 0, sw, sh, -16777216);
      this.drawProgressBar(context, sw, sh, progress);
   }

   @Unique
   private float getProgress() {
      try {
         for (Field f : SplashOverlay.class.getDeclaredFields()) {
            if (f.getType() == float.class) {
               f.setAccessible(true);
               float val = (Float)f.get(this);
               if (val >= 0.0F && val <= 1.0F) {
                  return val;
               }
            }
         }
      } catch (Exception var6) {
      }

      return Math.min(1.0F, (float)(System.currentTimeMillis() - this.startTimeMs) / 5000.0F);
   }

   @Unique
   private void drawProgressBar(DrawContext context, int sw, int sh, float progress) {
      float eased = 1.0F
         - (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F)) * (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F)) * (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F));
      int maxW = Math.max(24, Math.min(620, sw - 42));
      int minW = Math.min(150, maxW);
      int barW = MathHelper.clamp(Math.round(sw * 0.56F), minW, maxW);
      int barX = (sw - barW) / 2;
      int barY = Math.round(sh * 0.76F);
      int barH = Math.max(1, Math.round(sh * 0.003F));
      int fillW = MathHelper.clamp(Math.round(barW * eased), 0, barW);
      context.fill(barX, barY, barX + barW, barY + barH, withAlpha(1443880, 1.0F));
      if (fillW > 0) {
         context.fillGradient(barX, barY, barX + fillW, barY + barH, -9814112, -5210113);
         context.fill(barX, barY - Math.max(1, barH + 2), barX + fillW, barY, withAlpha(9847008, 0.16F));
         context.fill(barX, barY + barH, barX + fillW, barY + barH + Math.max(1, barH + 2), withAlpha(9847008, 0.13F));
      }
   }

   @Unique
   private static int withAlpha(int rgb, float opacity) {
      int alpha = MathHelper.clamp(Math.round(opacity * 255.0F), 0, 255);
      return alpha << 24 | rgb & 16777215;
   }

}
