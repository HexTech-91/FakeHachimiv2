package net.hachimi.client.impl.module.misc;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;

public class AutoReconnectModule extends ToggleModule {
   Config<Integer> delayConfig = this.register(new NumberConfig<>("Delay", "The delay between reconnects to a server", 0, 5, 100));

   public AutoReconnectModule() {
      super("AutoReconnect", "Automatically reconnects to a server immediately after disconnecting", ModuleCategory.MISCELLANEOUS);
   }

   public int getDelay() {
      return this.delayConfig.getValue();
   }
}
