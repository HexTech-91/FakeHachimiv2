package net.hachimi.client.mixin.accessor;

import java.util.Optional;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({UpdateBeaconC2SPacket.class})
public interface AccessorUpdateBeaconC2SPacket {
   @Accessor("primary")
   @Mutable
   void setPrimaryEffect(Optional<StatusEffect> var1);

   @Accessor("secondary")
   @Mutable
   void setSecondaryEffect(Optional<StatusEffect> var1);
}
