package net.hachimi.client.impl.imixin;

import net.hachimi.client.util.network.InteractType;
import net.minecraft.entity.Entity;

@IMixin
public interface IPlayerInteractEntityC2SPacket {
   Entity getEntity();

   InteractType getType();
}
