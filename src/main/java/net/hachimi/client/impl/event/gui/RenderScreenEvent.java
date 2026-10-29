package net.hachimi.client.impl.event.gui;

import net.hachimi.eventbus.event.Event;
import net.minecraft.client.util.math.MatrixStack;

public class RenderScreenEvent extends Event {
   public final MatrixStack matrixStack;

   public RenderScreenEvent(MatrixStack matrixStack) {
      this.matrixStack = matrixStack;
   }
}
