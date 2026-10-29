package net.hachimi.client.security;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class AntiPacketLogger {
   public void convertMinecraftProtocol(Packet<?> packet) {
      new PacketByteBuf(Unpooled.buffer());
   }
}
