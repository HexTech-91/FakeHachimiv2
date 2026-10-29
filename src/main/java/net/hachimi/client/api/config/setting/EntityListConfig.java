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
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class EntityListConfig<T extends List<EntityType>> extends Config<T> {
   public EntityListConfig(String name, String desc, EntityType... values) {
      super(name, desc, (T)(new ArrayList<EntityType>(Arrays.asList(values))));
   }

   public void add(EntityType entityType) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().add(entityType);
      configUpdateEvent.setStage(StageEvent.EventStage.POST);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
   }

   public void remove(EntityType<?> entityType) {
      ConfigUpdateEvent configUpdateEvent = new ConfigUpdateEvent(this);
      configUpdateEvent.setStage(StageEvent.EventStage.PRE);
      EventBus.INSTANCE.dispatch(configUpdateEvent);
      this.getValue().remove(entityType);
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
      return obj instanceof EntityType ? this.value.contains(obj) : false;
   }

   @Override
   public JsonObject toJson() {
      JsonObject jsonObj = super.toJson();
      JsonArray array = new JsonArray();

      for (EntityType entityType : (List<EntityType>)(Object)this.getValue()) {
         Identifier id = Registries.ENTITY_TYPE.getId(entityType);
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
         List<EntityType> temp = new ArrayList<>();

         for (JsonElement je : element.getAsJsonArray()) {
            String val = je.getAsString();
            EntityType entityType = Registries.ENTITY_TYPE.get(Identifier.of(val));
            temp.add(entityType);
         }

         return (T)temp;
      }
   }
}
