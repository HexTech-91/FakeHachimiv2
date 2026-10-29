package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.event.Event;
import net.minecraft.client.util.math.MatrixStack;

public class RenderWorldEvent extends Event {
   private final MatrixStack matrices;
   private final float tickDelta;

   public RenderWorldEvent(MatrixStack matrices, float tickDelta) {
      this.matrices = matrices;
      this.tickDelta = tickDelta;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public static class Game extends RenderWorldEvent {
      public Game(MatrixStack matrices, float tickDelta) {
         super(matrices, tickDelta);
      }
   }

   public static class Hand extends RenderWorldEvent {
      public Hand(MatrixStack matrices, float tickDelta) {
         super(matrices, tickDelta);
      }
   }
}
