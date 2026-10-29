package net.hachimi.client.impl.event.network;

import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.event.Event;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class SetCurrentHandEvent extends Event implements Globals {
   private final Hand hand;

   public SetCurrentHandEvent(Hand hand) {
      this.hand = hand;
   }

   public Hand getHand() {
      return this.hand;
   }

   public ItemStack getStackInHand() {
      return mc.player.getStackInHand(this.hand);
   }
}
