package net.hachimi.client.mixin.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import net.hachimi.client.impl.event.network.DecodePacketEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.network.handler.DecoderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DecoderHandler.class})
public class MixinDecoderHandler {
   @Inject(
      method = {"decode"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/NetworkPhase;getId()Ljava/lang/String;",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void hookDecode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> objects, CallbackInfo ci) {
      DecodePacketEvent decodePacketEvent = new DecodePacketEvent();
      EventBus.INSTANCE.dispatch(decodePacketEvent);
      if (decodePacketEvent.isCanceled()) {
         ci.cancel();
      }
   }
}
