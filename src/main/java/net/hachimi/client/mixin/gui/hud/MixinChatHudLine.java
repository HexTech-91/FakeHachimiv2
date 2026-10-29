package net.hachimi.client.mixin.gui.hud;

import net.hachimi.client.impl.imixin.IChatHudLine;
import net.hachimi.client.util.Globals;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({ChatHudLine.class})
public abstract class MixinChatHudLine implements IChatHudLine, Globals {
   @Unique
   private int id;

   @Override
   public int getId() {
      return this.id;
   }

   @Override
   public void setId(int id) {
      this.id = id;
   }
}
