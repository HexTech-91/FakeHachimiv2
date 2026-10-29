package net.hachimi.client.impl.module.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.math.timer.CacheTimer;
import net.hachimi.client.util.render.ColorUtilPlus;
import net.hachimi.client.util.render.Render3DUtilPlus;
import net.hachimi.client.util.render.animation.EasingPlus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class PlaceEchoModule extends ToggleModule {
   public static final Map<BlockPos, PlaceEchoModule.PlaceEchoPos> renderMap = new HashMap<>();
   private static PlaceEchoModule INSTANCE;
   private final Config<Double> fadeTime = this.register(new NumberConfig<>("Fade-Time", "Duration of the fade-out animation (ms)", 0.0, 250.0, 3000.0));
   private final Config<Boolean> noFail = this.register(new BooleanConfig("NoFail", "Skip try-place state; always start fading immediately", false));
   private final Config<Double> timeout = this.register(new NumberConfig<>("TimeOut", "Max wait time for block to appear (ms)", 0.0, 3000.0, 10000.0));
   private final Config<Boolean> boxEnabled = this.register(new BooleanConfig("Box", "Draw outline after block placed", false));
   private final Config<Boolean> fillEnabled = this.register(new BooleanConfig("Fill", "Draw fill after block placed", true));
   private final Config<Color> boxColor = this.register(
      new ColorConfig("BoxColor", "Outline color after block placed", new Color(245, 138, 231, 255), true, true, () -> this.boxEnabled.getValue())
   );
   private final Config<Color> fillColor = this.register(
      new ColorConfig("FillColor", "Fill color after block placed", new Color(245, 138, 231, 100), true, true, () -> this.fillEnabled.getValue())
   );
   private final Config<Color> waitingBoxColor = this.register(
      new ColorConfig("WaitingBoxColor", "Outline color while waiting for block", new Color(245, 138, 231, 28), true, true, () -> this.boxEnabled.getValue())
   );
   private final Config<Color> waitingFillColor = this.register(
      new ColorConfig("WaitingFillColor", "Fill color while waiting for block", new Color(245, 138, 231, 45), true, true, () -> this.fillEnabled.getValue())
   );
   private final Config<EasingPlus> easing = this.register(new EnumConfig<>("Easing", "Animation easing function", EasingPlus.Linear, EasingPlus.values()));
   private final Config<PlaceEchoModule.PlaceEchoMode> mode = this.register(
      new EnumConfig<>("Mode", "Animation mode", PlaceEchoModule.PlaceEchoMode.Shrink, PlaceEchoModule.PlaceEchoMode.values())
   );

   public PlaceEchoModule() {
      super("PlaceEcho", "Animated block-place highlight", ModuleCategory.RENDER);
      INSTANCE = this;
   }

   public static PlaceEchoModule getInstance() {
      return INSTANCE;
   }

   public void create(BlockPos pos) {
      renderMap.put(pos, new PlaceEchoModule.PlaceEchoPos(pos));
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.world != null) {
         renderMap.values().removeIf(p -> p.draw(event.getMatrices()));
      }
   }

   public static enum PlaceEchoMode {
      Fade,
      Shrink,
      All;
   }

   public class PlaceEchoPos {
      public final CacheTimer placeEchoFadeTimer = new CacheTimer();
      public final CacheTimer placeEchoWaitTimer = new CacheTimer();
      public final BlockPos placeEchoPos;
      public boolean placeEchoIsAir = true;

      public PlaceEchoPos(BlockPos pos) {
         this.placeEchoPos = pos;
      }

      public boolean draw(MatrixStack matrices) {
         if (this.placeEchoIsAir) {
            if (!PlaceEchoModule.this.noFail.getValue() && Globals.mc.world.isAir(this.placeEchoPos)) {
               if (this.placeEchoWaitTimer.passed(PlaceEchoModule.this.timeout.getValue().longValue())) {
                  return true;
               }

               this.placeEchoFadeTimer.reset();
               Box placeEchoBox = new Box(this.placeEchoPos);
               if (PlaceEchoModule.this.fillEnabled.getValue() && PlaceEchoModule.this.waitingFillColor.getValue().getAlpha() > 0) {
                  Render3DUtilPlus.drawFill(matrices, placeEchoBox, PlaceEchoModule.this.waitingFillColor.getValue());
               }

               if (PlaceEchoModule.this.boxEnabled.getValue() && PlaceEchoModule.this.waitingBoxColor.getValue().getAlpha() > 0) {
                  Render3DUtilPlus.drawBox(matrices, placeEchoBox, PlaceEchoModule.this.waitingBoxColor.getValue());
               }

               return false;
            }

            this.placeEchoIsAir = false;
            this.placeEchoFadeTimer.reset();
         }

         long elapsed = this.placeEchoFadeTimer.getElapsedTime();
         long placeEchoDuration = PlaceEchoModule.this.fadeTime.getValue().longValue();
         double raw = placeEchoDuration <= 0L ? 1.0 : Math.min(1.0, (double)elapsed / placeEchoDuration);
         double placeEchoQuads = PlaceEchoModule.this.easing.getValue().ease(raw);
         if (placeEchoQuads >= 1.0) {
            return true;
         } else {
            double placeEchoAlpha = PlaceEchoModule.this.mode.getValue() != PlaceEchoModule.PlaceEchoMode.Fade
                  && PlaceEchoModule.this.mode.getValue() != PlaceEchoModule.PlaceEchoMode.All
               ? 1.0
               : 1.0 - placeEchoQuads;
            double placeEchoShrink = PlaceEchoModule.this.mode.getValue() != PlaceEchoModule.PlaceEchoMode.Shrink
                  && PlaceEchoModule.this.mode.getValue() != PlaceEchoModule.PlaceEchoMode.All
               ? 0.0
               : placeEchoQuads * 0.5;
            Box placeEchoBoxx = new Box(this.placeEchoPos).expand(-placeEchoShrink, -placeEchoShrink, -placeEchoShrink);
            if (PlaceEchoModule.this.fillEnabled.getValue() && PlaceEchoModule.this.fillColor.getValue().getAlpha() > 0) {
               Color placeEchoFill = ColorUtilPlus.injectAlpha(
                  PlaceEchoModule.this.fillColor.getValue(), (int)(PlaceEchoModule.this.fillColor.getValue().getAlpha() * placeEchoAlpha)
               );
               Render3DUtilPlus.drawFill(matrices, placeEchoBoxx, placeEchoFill);
            }

            if (PlaceEchoModule.this.boxEnabled.getValue() && PlaceEchoModule.this.boxColor.getValue().getAlpha() > 0) {
               Color placeEchoOutline = ColorUtilPlus.injectAlpha(
                  PlaceEchoModule.this.boxColor.getValue(), (int)(PlaceEchoModule.this.boxColor.getValue().getAlpha() * placeEchoAlpha)
               );
               Render3DUtilPlus.drawBox(matrices, placeEchoBoxx, placeEchoOutline);
            }

            return false;
         }
      }
   }
}
