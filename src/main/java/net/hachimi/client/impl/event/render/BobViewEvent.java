package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.client.util.math.MatrixStack;

@Cancelable
public class BobViewEvent extends Event {
   private final MatrixStack matrixStack;
   private final float tickDelta;
   private float y;

   public BobViewEvent(MatrixStack matrixStack, float tickDelta) {
      this.matrixStack = matrixStack;
      this.tickDelta = tickDelta;
   }

   public float getY() {
      return this.y;
   }

   public void setY(float y) {
      this.y = y;
   }

   public MatrixStack getMatrixStack() {
      return this.matrixStack;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }
}
