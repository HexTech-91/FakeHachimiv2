package net.hachimi.client.impl.event.render.block;

import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;

public class RenderTileEntityEvent extends Event {
   @Cancelable
   public static class EnchantingTableBook extends RenderTileEntityEvent {
   }
}
