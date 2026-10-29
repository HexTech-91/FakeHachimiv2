package net.hachimi.client.impl.event.gui.hud;

import java.util.UUID;
import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.text.Text;

@Cancelable
public class PlayerListNameEvent extends Event {
   private Text playerName;
   private final UUID id;

   public PlayerListNameEvent(Text playerName, UUID id) {
      this.playerName = playerName;
      this.id = id;
   }

   public void setPlayerName(Text playerName) {
      this.playerName = playerName;
   }

   public Text getPlayerName() {
      return this.playerName;
   }

   public UUID getId() {
      return this.id;
   }
}
