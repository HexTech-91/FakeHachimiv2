package net.hachimi.client.api.config.setting;

import com.google.gson.JsonObject;
import net.hachimi.client.api.config.Config;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

public class ItemConfig extends Config<Item> {
   private final Item[] values;

   public ItemConfig(String name, String desc, Item value, Item[] values) {
      super(name, desc, value);
      this.values = values;
   }

   public ItemConfig(String name, String desc, Item value) {
      this(name, desc, value, (Item[])Registries.ITEM.stream().toArray());
   }

   @Override
   public JsonObject toJson() {
      return null;
   }

   public Item fromJson(JsonObject jsonObj) {
      return null;
   }
}
