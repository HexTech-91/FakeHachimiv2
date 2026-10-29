package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.StringConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.text.TextVisitEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class NameProtectModule extends ToggleModule {
   Config<String> placeholderConfig = this.register(new StringConfig("Placeholder", "The placeholder name for the player", "Player"));

   public NameProtectModule() {
      super("NameProtect", "Hides the player name in chat and tablist", ModuleCategory.RENDER);
   }

   @EventListener
   public void onTextVisit(TextVisitEvent event) {
      if (mc.player != null) {
         String username = mc.getSession().getUsername();
         String text = event.getText();
         if (text.contains(username)) {
            event.cancel();
            event.setText(text.replace(username, this.placeholderConfig.getValue()));
         }
      }
   }
}
