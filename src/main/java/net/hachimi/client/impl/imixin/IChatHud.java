package net.hachimi.client.impl.imixin;

import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;

@IMixin
public interface IChatHud {
   void addMessage(Text var1, MessageIndicator var2, int var3);
}
