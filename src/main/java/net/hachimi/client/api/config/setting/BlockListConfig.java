package net.hachimi.client.api.config.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.impl.event.config.ConfigUpdateEvent;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class BlockListConfig<T extends List<Block>> extends Config<T> {
   public BlockListConfig(String name, String desc, Block... values) {
      super(name, desc, (T)(new ArrayList<Block>(Arrays.asList(values))));
   }

   public void add(Block block) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().add(block);
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public void remove(Block block) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().remove(block);
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public void clear() {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().clear();
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public boolean contains(Object obj) {
      return obj instanceof Block block ? this.value.contains(block) : false;
   }

   @Override
   public JsonObject toJson() {
      JsonObject jsonObj = super.toJson();
      JsonArray array = new JsonArray();

      for (Block block : (List<Block>)(Object)this.getValue()) {
         Identifier id = Registries.BLOCK.getId(block);
         array.add(id.toString());
      }

      jsonObj.add("value", array);
      return jsonObj;
   }

   public T fromJson(JsonObject jsonObj) {
      if (!jsonObj.has("value")) {
         return null;
      } else {
         JsonElement element = jsonObj.get("value");
         List<Block> temp = new ArrayList<>();

         for (JsonElement je : element.getAsJsonArray()) {
            String val = je.getAsString();
            Block block = Registries.BLOCK.get(Identifier.of(val));
            temp.add(block);
         }

         return (T)temp;
      }
   }
}
