package net.hachimi.client.mixin.gui.screen;

import net.hachimi.client.util.Globals;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({DisconnectedScreen.class})
public abstract class MixinDisconnectedScreen extends MixinScreen implements Globals {
}
