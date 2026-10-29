package net.hachimi.client.mixin.accessor;

import java.util.List;
import net.minecraft.entity.data.DataTracker.SerializedEntry;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({EntityTrackerUpdateS2CPacket.class})
public interface AccessorEntityTrackerUpdateS2CPacket {
   @Accessor("trackedValues")
   @Mutable
   void hookSetTrackedValues(List<SerializedEntry<?>> var1);
}
