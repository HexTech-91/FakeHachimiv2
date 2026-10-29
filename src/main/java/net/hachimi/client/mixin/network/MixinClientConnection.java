package net.hachimi.client.mixin.network;

import io.netty.channel.ChannelHandlerContext;
import net.hachimi.client.impl.event.network.DecodePacketEvent;
import net.hachimi.client.impl.event.network.DisconnectEvent;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class MixinClientConnection {
   @Shadow
   @Nullable
   private volatile PacketListener packetListener;
   @Shadow
   @Final
   private static Logger LOGGER;

   @Inject(
      method = {"exceptionCaught"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookExceptionCaught(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
      DecodePacketEvent decodePacketEvent = new DecodePacketEvent();
      EventBus.INSTANCE.dispatch(decodePacketEvent);
      if (decodePacketEvent.isCanceled()) {
         LOGGER.error("Exception caught on network thread:", ex);
         ci.cancel();
      }
   }

   @Inject(
      method = {"sendImmediately"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookSendImmediately(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
      PacketEvent.Outbound packetOutboundEvent = new PacketEvent.Outbound(packet);
      EventBus.INSTANCE.dispatch(packetOutboundEvent);
      if (packetOutboundEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"sendImmediately"},
      at = {@At("TAIL")},
      cancellable = true
   )
   private void hookSendImmediately$2(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
      PacketEvent.OutboundPost packetOutboundEvent = new PacketEvent.OutboundPost(packet);
      EventBus.INSTANCE.dispatch(packetOutboundEvent);
      if (packetOutboundEvent.isCanceled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookChannelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
      PacketListener ownedPacketListener = this.packetListener;
      if (packet != null && ownedPacketListener != null && ownedPacketListener.accepts(packet)) {
         PacketEvent.Inbound packetInboundEvent = new PacketEvent.Inbound(this.packetListener, packet);
         EventBus.INSTANCE.dispatch(packetInboundEvent);
         if (packetInboundEvent.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"disconnect"},
      at = {@At("HEAD")}
   )
   private void hookDisconnect(Text disconnectReason, CallbackInfo ci) {
      DisconnectEvent disconnectEvent = new DisconnectEvent();
      EventBus.INSTANCE.dispatch(disconnectEvent);
   }
}
