package net.hachimi.client.impl.event.block;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.block.Block;

@Cancelable
public class BlockSlipperinessEvent extends Event {
   private final Block block;
   private float slipperiness;

   public BlockSlipperinessEvent(Block block, float slipperiness) {
      this.block = block;
      this.slipperiness = slipperiness;
   }

   public Block getBlock() {
      return this.block;
   }

   public float getSlipperiness() {
      return this.slipperiness;
   }

   public void setSlipperiness(float slipperiness) {
      this.slipperiness = slipperiness;
   }
}
