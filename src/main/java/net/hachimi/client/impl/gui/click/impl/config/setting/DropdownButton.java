package net.hachimi.client.impl.gui.click.impl.config.setting;

import java.util.Arrays;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.EnumConfig;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.gui.click.impl.config.CategoryFrame;
import net.hachimi.client.impl.gui.click.impl.config.ModuleButton;
import net.hachimi.client.impl.manager.world.sound.SoundManager;
import net.hachimi.client.impl.module.client.ClickGuiModule;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.string.EnumFormatter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class DropdownButton extends ConfigButton<Enum<?>> {
   private int index;
   private boolean open;
   private String[] values;

   public DropdownButton(CategoryFrame frame, ModuleButton moduleButton, Config<Enum<?>> config, float x, float y) {
      super(frame, moduleButton, config, x, y);
      this.cacheValues();
   }

   private void cacheValues() {
      Enum<?>[] enumValues = ((EnumConfig)(Object)this.config).getValues();
      this.values = Arrays.stream(enumValues).map(Enum::name).toArray(String[]::new);
      Enum<?> val = this.config.getValue();
      if (val != null) {
         for (int i = 0; i < this.values.length; i++) {
            if (this.values[i].equals(val.name())) {
               this.index = i;
               break;
            }
         }
      }
   }

   private float rowH() {
      return RenderManager.textHeight() + 4.0F;
   }

   @Override
   public void render(DrawContext context, float ix, float iy, float mouseX, float mouseY, float delta) {
      float s = ClickGuiModule.CLICK_GUI_SCALE;
      float base = this.rowH();
      float totalH = this.open ? base + this.values.length * base : base;
      this.setHeight(totalH);
      this.x = ix;
      this.y = iy;
      String val = EnumFormatter.formatEnum(this.config.getValue());
      boolean over = this.isWithin(mouseX, mouseY);
      if (over != this.hoverAnimation.getState()) {
         this.hoverAnimation.setState(over);
      }

      int hoverAlpha = (int)(80.0 * MathHelper.clamp(this.hoverAnimation.getFactor(), 0.0, 1.0));
      this.fill(context, ix, iy, this.width * s, base * s, ClickGuiModule.getInstance().getColor(100 + hoverAlpha));
      this.drawStringScaled(context, this.config.getName(), ix + 2.0F * s, iy + 4.0F * s, -1);
      float nameW = RenderManager.textWidth(this.config.getName()) * s;
      int valColor = ClickGuiModule.getInstance().getColor(255);
      this.drawStringScaled(context, " " + val, ix + 2.0F * s + nameW, iy + 4.0F * s, valColor);
      String arrow = this.open ? " ▲" : " ▼";
      float arrowW = RenderManager.textWidth(this.config.getName() + " " + val) * s;
      this.drawStringScaled(context, arrow, ix + 2.0F * s + arrowW, iy + 4.0F * s, -5592406);
      if (this.open) {
         for (int i = 0; i < this.values.length; i++) {
            float rowY = iy + base * s + i * base * s;
            boolean rowOver = this.isMouseOver(mouseX, mouseY, ix, rowY, this.width * s, base * s);
            int bgColor;
            if (i == this.index) {
               bgColor = ClickGuiModule.getInstance().getColor(160);
            } else if (rowOver) {
               bgColor = ClickGuiModule.getInstance().getColor(120);
            } else {
               bgColor = ClickGuiModule.getInstance().getColor(70);
            }

            this.fill(context, ix, rowY, this.width * s, base * s, bgColor);
            String label = EnumFormatter.formatEnum(Enum.valueOf((Class)this.config.getValue().getClass(), this.values[i]));
            int textColor = i == this.index ? -1 : -3355444;
            this.drawStringScaled(context, " " + label, ix + 2.0F * s, rowY + 4.0F * s, textColor);
         }
      }
   }

   @Override
   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (!this.isWithin(mouseX, mouseY) && !this.openAreaContains(mouseX, mouseY)) {
         if (this.open) {
            this.open = false;
         }
      } else {
         float s = ClickGuiModule.CLICK_GUI_SCALE;
         float base = this.rowH();
         if (button == 1) {
            if (this.isWithin(mouseX, mouseY)) {
               this.open = !this.open;
               this.playClick();
            }
         } else {
            if (button == 0) {
               if (this.open) {
                  for (int i = 0; i < this.values.length; i++) {
                     float rowY = this.y + base * s + i * base * s;
                     if (this.isMouseOver(mouseX, mouseY, this.x, rowY, this.width * s, base * s)) {
                        this.index = i;
                        this.config.setValue(Enum.valueOf((Class)this.config.getValue().getClass(), this.values[i]));
                        this.open = false;
                        this.playClick();
                        return;
                     }
                  }

                  if (this.isWithin(mouseX, mouseY)) {
                     this.open = false;
                     this.playClick();
                  }
               } else {
                  this.index = this.index + 1 > this.values.length - 1 ? 0 : this.index + 1;
                  this.config.setValue(Enum.valueOf((Class)this.config.getValue().getClass(), this.values[this.index]));
                  this.playClick();
               }
            }
         }
      }
   }

   private boolean openAreaContains(double mouseX, double mouseY) {
      if (!this.open) {
         return false;
      } else {
         float s = ClickGuiModule.CLICK_GUI_SCALE;
         float base = this.rowH();
         return this.isMouseOver(mouseX, mouseY, this.x, this.y + base * s, this.width * s, this.values.length * base * s);
      }
   }

   private boolean isMouseOver(double mx, double my, float rx, float ry, float rw, float rh) {
      return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
   }

   private void playClick() {
      if (ClickGuiModule.getInstance().getSounds()) {
         Managers.SOUND.playSound(SoundManager.GUI_CLICK);
      }
   }

   @Override
   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   @Override
   public void keyPressed(int keyCode, int scanCode, int modifiers) {
   }

   @Override
   public void charTyped(char character, int modifiers) {
   }
}
