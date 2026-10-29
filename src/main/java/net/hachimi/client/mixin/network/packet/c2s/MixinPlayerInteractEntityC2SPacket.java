package net.hachimi.client.mixin.network.packet.c2s;

import io.netty.buffer.Unpooled;
import net.hachimi.client.impl.imixin.IPlayerInteractEntityC2SPacket;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.network.InteractType;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({PlayerInteractEntityC2SPacket.class})
public abstract class MixinPlayerInteractEntityC2SPacket implements IPlayerInteractEntityC2SPacket, Globals {
   @Shadow
   @Final
   private int entityId;

   @Shadow
   public abstract void write(PacketByteBuf var1);

   @Override
   public Entity getEntity() {
      return mc.world == null ? null : mc.world.getEntityById(this.entityId);
   }

   @Override
   public InteractType getType() {
      PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
      this.write(packetBuf);
      packetBuf.readVarInt();
      return packetBuf.readEnumConstant(InteractType.class);
   }
}
