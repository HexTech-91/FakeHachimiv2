package net.hachimi.client.impl.imixin;

import net.minecraft.network.packet.Packet;

@IMixin
public interface IClientPlayNetworkHandler {
   void sendQuietPacket(Packet<?> var1);
}
