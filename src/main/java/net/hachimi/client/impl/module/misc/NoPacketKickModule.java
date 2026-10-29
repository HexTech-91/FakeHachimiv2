package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.DecodePacketEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class NoPacketKickModule extends ToggleModule {
   public NoPacketKickModule() {
      super("NoPacketKick", "Prevents getting kicked by packets", ModuleCategory.MISCELLANEOUS);
   }

   @EventListener
   public void onDecodePacket(DecodePacketEvent event) {
      event.cancel();
   }
}
