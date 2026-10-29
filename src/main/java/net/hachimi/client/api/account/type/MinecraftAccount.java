package net.hachimi.client.api.account.type;

import com.google.gson.JsonObject;
import net.minecraft.client.session.Session;

public interface MinecraftAccount {
   Session login();

   String username();

   default JsonObject toJSON() {
      JsonObject object = new JsonObject();
      object.addProperty("username", this.username());
      return object;
   }
}
