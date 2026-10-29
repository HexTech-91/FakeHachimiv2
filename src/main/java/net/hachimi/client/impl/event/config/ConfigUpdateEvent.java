package net.hachimi.client.impl.event.config;

import net.hachimi.client.api.config.Config;
import net.hachimi.eventbus.event.StageEvent;

public class ConfigUpdateEvent extends StageEvent {
   private final Config<?> config;

   public ConfigUpdateEvent(Config<?> config) {
      this.config = config;
   }

   public Config<?> getConfig() {
      return this.config;
   }
}
