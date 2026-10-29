package net.hachimi.client.impl.event.network;

import com.mojang.authlib.GameProfile;
import net.hachimi.eventbus.annotation.Cancelable;
import net.hachimi.eventbus.event.Event;
import net.minecraft.util.Identifier;

@Cancelable
public class CapesEvent extends Event {
   private final GameProfile gameProfile;
   private Identifier texture;
   private boolean optifine;

   public CapesEvent(GameProfile gameProfile) {
      this.gameProfile = gameProfile;
   }

   public GameProfile getGameProfile() {
      return this.gameProfile;
   }

   public void setTexture(Identifier texture) {
      this.texture = texture;
   }

   public Identifier getTexture() {
      return this.texture;
   }

   public void setShowOptifine(boolean optifine) {
      this.optifine = optifine;
   }

   public boolean getShowOptifine() {
      return this.optifine;
   }
}
