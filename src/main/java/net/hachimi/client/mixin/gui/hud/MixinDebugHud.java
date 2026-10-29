package net.hachimi.client.mixin.gui.hud;

import java.util.List;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DebugHud.class})
public abstract class MixinDebugHud {
   @Shadow
   protected abstract List<String> getLeftText();

   @Redirect(
      method = {"drawLeftText"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/DebugHud;getLeftText()Ljava/util/List;"
      )
   )
   private List<String> redirectRightTextEarly(DebugHud instance) {
      List<String> list = this.getLeftText();
      String modName = String.format(
         "%s %s (%s%s%s)", "Hachimi", "2.0", "\"release\"", !"\"release\"".equals("dev") ? "-2.0" : "", !"null".equals("null") ? "-null" : ""
      );
      list.add(1, modName);
      return list;
   }
}
