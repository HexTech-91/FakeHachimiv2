package net.hachimi.client.impl.event.render;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.client.util.math.MatrixStack;

@Cancelable
public class RenderShaderEvent extends Event {
   private final MatrixStack matrices;
   private final float tickDelta;

   public RenderShaderEvent(MatrixStack matrices, float tickDelta) {
      this.matrices = matrices;
      this.tickDelta = tickDelta;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public float getTickDelta() {
      return this.tickDelta;
   }

   public static class BlockEntities extends RenderShaderEvent {
      public BlockEntities(MatrixStack matrices, float tickDelta) {
         super(matrices, tickDelta);
      }
   }
}
