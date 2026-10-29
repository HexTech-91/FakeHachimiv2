package net.hachimi.client.impl.event.network;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

@Cancelable
public class InteractBlockEvent extends Event {
   private final ClientPlayerEntity player;
   private final Hand hand;
   private final BlockHitResult hitResult;

   public InteractBlockEvent(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
      this.player = player;
      this.hand = hand;
      this.hitResult = hitResult;
   }

   public ClientPlayerEntity getPlayer() {
      return this.player;
   }

   public Hand getHand() {
      return this.hand;
   }

   public BlockHitResult getHitResult() {
      return this.hitResult;
   }
}
