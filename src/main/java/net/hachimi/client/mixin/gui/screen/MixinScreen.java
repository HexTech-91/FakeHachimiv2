package net.hachimi.client.mixin.gui.screen;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Screen.class})
public abstract class MixinScreen {
   @Shadow
   public int width;
   @Shadow
   public int height;
   @Shadow
   @Final
   private List<Drawable> drawables;
   @Shadow
   @Final
   protected Text title;
   @Shadow
   @Nullable
   protected MinecraftClient client;

   @Shadow
   protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T var1);

   @Shadow
   public void tick() {
   }

   @Unique
   public List<Drawable> getDrawables() {
      return this.drawables;
   }
}
