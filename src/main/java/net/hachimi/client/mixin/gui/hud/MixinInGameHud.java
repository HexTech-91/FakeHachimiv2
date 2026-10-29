package net.hachimi.client.mixin.gui.hud;

import net.hachimi.client.impl.event.gui.hud.RenderCrosshairEvent;
import net.hachimi.client.impl.event.gui.hud.RenderOverlayEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public class MixinInGameHud implements Globals {
   @Shadow
   @Final
   private static Identifier PUMPKIN_BLUR;
   @Shadow
   @Final
   private static Identifier POWDER_SNOW_OUTLINE;

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void hookRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      RenderOverlayEvent.Post renderOverlayEvent = new RenderOverlayEvent.Post(context, tickCounter.getTickDelta(true));
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
   }

   @Inject(
      method = {"renderStatusEffectOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      RenderOverlayEvent.StatusEffect renderOverlayEvent = new RenderOverlayEvent.StatusEffect(context);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderPortalOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      RenderOverlayEvent.Portal renderOverlayEvent = new RenderOverlayEvent.Portal(context);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSpyglassOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderSpyglassOverlay(DrawContext context, float scale, CallbackInfo ci) {
      RenderOverlayEvent.Spyglass renderOverlayEvent = new RenderOverlayEvent.Spyglass(context);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
      if (texture.getPath().equals(PUMPKIN_BLUR.getPath())) {
         RenderOverlayEvent.Pumpkin renderOverlayEvent = new RenderOverlayEvent.Pumpkin(context);
         EventBus.INSTANCE.dispatch(renderOverlayEvent);
         if (renderOverlayEvent.isCanceled()) {
            ci.cancel();
         }
      } else if (texture.getPath().equals(POWDER_SNOW_OUTLINE.getPath())) {
         RenderOverlayEvent.Frostbite renderOverlayEvent = new RenderOverlayEvent.Frostbite(context);
         EventBus.INSTANCE.dispatch(renderOverlayEvent);
         if (renderOverlayEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Redirect(
      method = {"renderHeldItemTooltip"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithBackground(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIII)I"
      )
   )
   private int hookRenderHeldItemTooltip(DrawContext instance, TextRenderer textRenderer, Text text, int x, int y, int width, int color) {
      RenderOverlayEvent.ItemName renderOverlayEvent = new RenderOverlayEvent.ItemName(instance);
      EventBus.INSTANCE.dispatch(renderOverlayEvent);
      if (renderOverlayEvent.isCanceled()) {
         return renderOverlayEvent.isUpdateXY()
            ? instance.drawText(mc.textRenderer, text, renderOverlayEvent.getX(), renderOverlayEvent.getY(), color, true)
            : 0;
      } else {
         return instance.drawText(mc.textRenderer, text, x, y, color, true);
      }
   }

   @Inject(
      method = {"renderMainHud"},
      at = {@At("TAIL")}
   )
   private void hookRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      RenderOverlayEvent.Hotbar hotbar = new RenderOverlayEvent.Hotbar(context);
      EventBus.INSTANCE.dispatch(hotbar);
   }

   @Inject(
      method = {"renderCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      RenderCrosshairEvent renderCrosshairEvent = new RenderCrosshairEvent(context);
      EventBus.INSTANCE.dispatch(renderCrosshairEvent);
      if (renderCrosshairEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
