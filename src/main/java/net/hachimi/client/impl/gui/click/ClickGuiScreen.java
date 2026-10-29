package net.hachimi.client.impl.gui.click;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.gui.click.component.ScissorStack;
import net.hachimi.client.impl.gui.click.impl.config.CategoryFrame;
import net.hachimi.client.impl.gui.click.impl.config.ModuleButton;
import net.hachimi.client.impl.gui.click.impl.config.setting.BindButton;
import net.hachimi.client.impl.gui.click.impl.config.setting.ColorButton;
import net.hachimi.client.impl.gui.click.impl.config.setting.ConfigButton;
import net.hachimi.client.impl.gui.click.impl.config.setting.SliderButton;
import net.hachimi.client.impl.gui.click.impl.config.setting.TextButton;
import net.hachimi.client.impl.module.client.ClickGuiModule;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.render.ColorUtil;
import net.hachimi.client.util.render.animation.Animation;
import net.hachimi.client.util.render.animation.Easing;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class ClickGuiScreen extends Screen implements Globals {
   public static int MOUSE_X;
   public static int MOUSE_Y;
   public static boolean MOUSE_RIGHT_CLICK;
   public static boolean MOUSE_RIGHT_HOLD;
   public static boolean MOUSE_LEFT_CLICK;
   public static boolean MOUSE_LEFT_HOLD;
   public static final ScissorStack SCISSOR_STACK = new ScissorStack();
   private final List<CategoryFrame> frames = new CopyOnWriteArrayList<>();
   private final ClickGuiModule module;
   private String text;
   private final Animation animation = new Animation(false, 200.0F, Easing.LINEAR);
   private boolean shouldCloseOnEsc = true;

   public ClickGuiScreen(ClickGuiModule module) {
      super(Text.literal("ClickGui"));
      this.module = module;
      float x = 15.0F;

      for (ModuleCategory category : ModuleCategory.values()) {
         CategoryFrame frame = new CategoryFrame(category, x, 15.0F);
         this.frames.add(frame);
         x += frame.getWidth() + 2.0F * ClickGuiModule.CLICK_GUI_SCALE;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      context.getMatrices().push();
      this.renderAndScaleGUI(context);
      boolean hovering = false;

      for (CategoryFrame frame : this.frames) {
         if (frame.isWithin(mouseX, mouseY) && MOUSE_LEFT_HOLD && this.checkDragging()) {
            frame.setDragging(true);
         }

         frame.render(context, mouseX, mouseY, delta);

         for (ModuleButton moduleButton : frame.getModuleButtons()) {
            if (moduleButton.isWithin(mouseX, mouseY)) {
               this.text = moduleButton.getModule().getDescription();
               hovering = true;
               break;
            }

            if (moduleButton.isOpen()) {
               for (ConfigButton<?> configButton : moduleButton.getConfigButtons()) {
                  if (configButton.isWithin(mouseX, mouseY)) {
                     this.text = configButton.getConfig().getDescription();
                     hovering = true;
                     break;
                  }
               }
            }
         }
      }

      context.getMatrices().pop();
      if (ClickGuiModule.getInstance().getDescriptions()) {
         this.animation.setState(hovering);
         if (this.animation.getFactor() > 0.01F) {
            context.getMatrices().scale(ClickGuiModule.CLICK_GUI_SCALE, ClickGuiModule.CLICK_GUI_SCALE, 0.0F);
            float j = 1.0F / ClickGuiModule.CLICK_GUI_SCALE;
            int width = RenderManager.textWidth(this.text);
            int hoverAlpha = (int)(60.0 * this.animation.getFactor());
            int unfilledColor = ClickGuiModule.getInstance().fixTransparency(new Color(0, 0, 0, 50 + hoverAlpha).getRGB());
            RenderManager.rect(
               context.getMatrices(), (mouseX + 10.0F) * j, (mouseY - 8.0F) * j, width + 4.0F, (RenderManager.textHeight() + 2.0F) * j, unfilledColor
            );
            RenderManager.renderText(
               context, this.text, (mouseX + 12.0F) * j, (mouseY - 6.0F) * j, ColorUtil.fixTransparency(-1, (float)this.animation.getFactor())
            );
            context.getMatrices().scale(j, j, 0.0F);
         }
      }

      MOUSE_LEFT_CLICK = false;
      MOUSE_RIGHT_CLICK = false;
      MOUSE_X = mouseX;
      MOUSE_Y = mouseY;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         MOUSE_LEFT_CLICK = true;
         MOUSE_LEFT_HOLD = true;
      } else if (button == 1) {
         MOUSE_RIGHT_CLICK = true;
         MOUSE_RIGHT_HOLD = true;
      }

      for (CategoryFrame frame : this.frames) {
         frame.mouseClicked(mouseX, mouseY, button);
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         MOUSE_LEFT_HOLD = false;
      } else if (button == 1) {
         MOUSE_RIGHT_HOLD = false;
      }

      for (CategoryFrame frame : this.frames) {
         frame.mouseReleased(mouseX, mouseY, button);
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      for (CategoryFrame frame : this.frames) {
         float scrolledY = (float)(frame.getY() + verticalAmount * ClickGuiModule.getInstance().getScrollSpeed());
         scrolledY = MathHelper.clamp(scrolledY, -(frame.getTotalHeight() - 10.0F), 15.0F);
         frame.setPos(frame.getX(), scrolledY);
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 82 && (modifiers & 2) != 0) {
      }

      this.shouldCloseOnEsc = true;

      for (CategoryFrame frame : this.frames) {
         if (frame.getSearchButton() != null && frame.getSearchButton().isTyping()) {
            this.shouldCloseOnEsc = false;
         } else {
            for (ModuleButton moduleButton : frame.getModuleButtons()) {
               for (ConfigButton<?> configButton : moduleButton.getConfigButtons()) {
                  if (configButton instanceof BindButton bindButton && bindButton.isListening()
                     || configButton instanceof TextButton textButton && textButton.isTyping()
                     || configButton instanceof ColorButton colorButton && colorButton.isTyping()
                     || configButton instanceof SliderButton sliderButton && sliderButton.isTyping()) {
                     this.shouldCloseOnEsc = false;
                  }
               }
            }
         }
      }

      for (CategoryFrame framex : this.frames) {
         framex.keyPressed(keyCode, scanCode, modifiers);
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (CategoryFrame frame : this.frames) {
         frame.charTyped(chr, modifiers);
      }

      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void close() {
      this.module.disable();
      MOUSE_LEFT_CLICK = false;
      MOUSE_LEFT_HOLD = false;
      MOUSE_RIGHT_CLICK = false;
      MOUSE_RIGHT_HOLD = false;
      super.close();
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return this.shouldCloseOnEsc;
   }

   private void renderAndScaleGUI(DrawContext context) {
      int backgroundColor = ClickGuiModule.getInstance().fixTransparency(-1156771817);
      RenderManager.rect(context.getMatrices(), 0.0, 0.0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), backgroundColor);
      float currentProgress = ClickGuiModule.getInstance().getScaleFactor();
      if (currentProgress != 1.0F) {
         context.getMatrices().translate((float)context.getScaledWindowHeight(), context.getScaledWindowHeight() / 2.0F, 0.0F);
         float goal = currentProgress * 0.2F;
         context.getMatrices().scale(0.8F + goal, 0.8F + goal, 0.0F);
         context.getMatrices().translate((float)(-context.getScaledWindowHeight()), -context.getScaledWindowHeight() / 2.0F, 0.0F);
      }
   }

   private boolean checkDragging() {
      for (CategoryFrame frame : this.frames) {
         if (frame.isDragging()) {
            return false;
         }
      }

      return true;
   }

   public List<CategoryFrame> getCategoryFrames() {
      return this.frames;
   }
}
