package net.hachimi.client.impl.module.render;

import java.awt.Color;
import java.text.DecimalFormat;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.render.RenderWorldEvent;
import net.hachimi.client.impl.manager.world.BreakManager;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.render.ColorUtilPlus;
import net.hachimi.client.util.render.Render3DUtilPlus;
import net.hachimi.client.util.render.animation.EasingPlus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

public final class BreakEspModule extends ToggleModule {
   private final Config<Boolean> progress = this.register(new BooleanConfig("Progress", "Show break progress", true));
   private final Config<Color> box = this.register(new ColorConfig("Box", "Box outline color", new Color(198, 176, 12, 255), true, true));
   private final Config<Color> fill = this.register(new ColorConfig("Fill", "Box fill color", new Color(198, 176, 12, 78), true, true));
   private final Config<Color> boxFriend = this.register(new ColorConfig("FriendBox", "Friend outline color", new Color(30, 45, 169, 255), true, true));
   private final Config<Color> fillFriend = this.register(new ColorConfig("FriendFill", "Friend fill color", new Color(30, 45, 169, 78), true, true));
   private final Config<EasingPlus> ease = this.register(new EnumConfig<>("Ease", "Box animation easing", EasingPlus.CubicInOut, EasingPlus.values()));
   private final Config<BreakEspModule.ColorMode> colorMode = this.register(
      new EnumConfig<>("ColorMode", "Box color mode", BreakEspModule.ColorMode.Static, BreakEspModule.ColorMode.values())
   );
   private final Config<Float> rainbowSpeed = this.register(
      new NumberConfig<>(
         "RainbowSpeed", "Rainbow speed (ms/cycle)", 500.0F, 3000.0F, 10000.0F, () -> this.colorMode.getValue() == BreakEspModule.ColorMode.Rainbow
      )
   );
   private final Config<Boolean> second = this.register(new BooleanConfig("Second", "Show double-mine blocks", true));
   private final Config<Color> secondBox = this.register(
      new ColorConfig("SecondBox", "Double outline color", new Color(255, 255, 255, 255), true, true, () -> this.second.getValue())
   );
   private final Config<Color> secondFill = this.register(
      new ColorConfig("SecondFill", "Double fill color", new Color(255, 255, 255, 100), true, true, () -> this.second.getValue())
   );
   private final Color startColor = new Color(255, 6, 6);
   private final Color endColor = new Color(0, 255, 12);
   private final Color doubleColor = new Color(255, 179, 96);
   private final DecimalFormat df = new DecimalFormat("0.0");

   public BreakEspModule() {
      super("BreakESP", "Highlight blocks being mined by other players", ModuleCategory.RENDER);
   }

   private Color getBoxColor(PlayerEntity player, int index, int total) {
      if (this.isFriend(player)) {
         return this.boxFriend.getValue();
      } else {
         return switch ((BreakEspModule.ColorMode)(Object)this.colorMode.getValue()) {
            case Rainbow -> ColorUtilPlus.rainbowColor(
               (double)index / Math.max(total, 1), this.rainbowSpeed.getValue().floatValue(), 0.9F, 1.0F, this.box.getValue().getAlpha()
            );
            case Pulse -> ColorUtilPlus.pulseColor(this.box.getValue(), index, total, 0.001);
            default -> (Color)(Object)this.box.getValue();
         };
      }
   }

   private Color getFillColor(PlayerEntity player, int index, int total) {
      if (this.isFriend(player)) {
         return this.fillFriend.getValue();
      } else {
         Color b = this.getBoxColor(player, index, total);
         return this.colorMode.getValue() == BreakEspModule.ColorMode.Static
            ? this.fill.getValue()
            : ColorUtilPlus.injectAlpha(b, this.fill.getValue().getAlpha());
      }
   }

   private boolean isFriend(PlayerEntity player) {
      return Managers.SOCIAL != null && Managers.SOCIAL.isFriend(player.getName().getString());
   }

   private boolean fillEnabled(Config<Color> cfg) {
      return cfg.getValue().getAlpha() > 0;
   }

   private boolean boxEnabled(Config<Color> cfg) {
      return cfg.getValue().getAlpha() > 0;
   }

   @EventListener
   public void onRenderWorld(RenderWorldEvent event) {
      if (mc.player != null && mc.world != null) {
         BreakManager bm = Managers.BREAK;
         if (bm != null) {
            MatrixStack matrixStack = event.getMatrices();
            int idx = 0;
            int total = bm.breakMap.size();

            for (BreakManager.BreakData breakData : bm.breakMap.values()) {
               if (breakData != null && breakData.getEntity() != null) {
                  PlayerEntity player = (PlayerEntity)breakData.getEntity();
                  double size = 0.5 * (1.0 - breakData.fade.ease(this.ease.getValue()));
                  Box cbox = new Box(breakData.pos).shrink(size, size, size).shrink(-size, -size, -size);
                  if (this.fillEnabled(this.fill)) {
                     Render3DUtilPlus.drawFill(matrixStack, cbox, this.getFillColor(player, idx, total));
                  }

                  if (this.boxEnabled(this.box)) {
                     Render3DUtilPlus.drawBox(matrixStack, cbox, this.getBoxColor(player, idx, total));
                  }

                  Render3DUtilPlus.drawText3D(
                     player.getName().getString(), breakData.pos.toCenterPos().add(0.0, this.progress.getValue() ? 0.15 : 0.0, 0.0), -1
                  );
                  if (this.progress.getValue()) {
                     double ratio = breakData.timer.getMs() / breakData.breakTime;
                     String label = breakData.failed ? "§4Failed" : (breakData.complete ? "Broke" : this.df.format(Math.min(1.0, ratio) * 100.0));
                     Color labelColor = breakData.complete
                        ? (mc.world.isAir(breakData.pos) ? this.endColor : this.startColor)
                        : ColorUtilPlus.fadeColor(this.startColor, this.endColor, ratio);
                     Render3DUtilPlus.drawText3D(Text.of(label), breakData.pos.toCenterPos().add(0.0, -0.15, 0.0), 0.0, 0.0, 1.0, labelColor);
                  }

                  idx++;
               } else {
                  idx++;
               }
            }

            if (this.second.getValue()) {
               for (int i : bm.doubleMap.keySet()) {
                  BreakManager.BreakData breakDatax = bm.doubleMap.get(i);
                  if (breakDatax != null && breakDatax.getEntity() != null && !mc.world.isAir(breakDatax.pos)) {
                     BreakManager.BreakData singleBreakData = bm.breakMap.get(i);
                     if (singleBreakData == null || !singleBreakData.pos.equals(breakDatax.pos)) {
                        double sizex = 0.5 * (1.0 - breakDatax.fade.ease(this.ease.getValue()));
                        Box cboxx = new Box(breakDatax.pos).shrink(sizex, sizex, sizex).shrink(-sizex, -sizex, -sizex);
                        if (this.fillEnabled(this.secondFill)) {
                           Render3DUtilPlus.drawFill(matrixStack, cboxx, this.secondFill.getValue());
                        }

                        if (this.boxEnabled(this.secondBox)) {
                           Render3DUtilPlus.drawBox(matrixStack, cboxx, this.secondBox.getValue());
                        }

                        Render3DUtilPlus.drawText3D(breakDatax.getEntity().getName().getString(), breakDatax.pos.toCenterPos().add(0.0, 0.15, 0.0), -1);
                        Render3DUtilPlus.drawText3D("Double", breakDatax.pos.toCenterPos().add(0.0, -0.15, 0.0), this.doubleColor.getRGB());
                     }
                  } else {
                     bm.doubleMap.remove(i);
                  }
               }
            }
         }
      }
   }

   public static enum ColorMode {
      Static,
      Rainbow,
      Pulse;
   }
}
