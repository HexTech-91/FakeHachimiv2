package net.hachimi.client.impl.module.misc;

import java.util.Set;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class NoSoundLagModule extends ToggleModule {
   private static final Set<SoundEvent> LAG_SOUNDS = Set.of(
      SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value(),
      SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA.value(),
      SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(),
      SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value(),
      SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(),
      SoundEvents.ITEM_ARMOR_EQUIP_GOLD.value(),
      SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(),
      SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value()
   );

   public NoSoundLagModule() {
      super("NoSoundLag", "Prevents sound effects from lagging the game", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onPacketInbound(PacketEvent.Inbound event) {
      if (event.getPacket() instanceof PlaySoundFromEntityS2CPacket packet && LAG_SOUNDS.contains(packet.getSound().value())
         || event.getPacket() instanceof PlaySoundS2CPacket packet2 && LAG_SOUNDS.contains(packet2.getSound().value())) {
         event.cancel();
      }
   }
}
