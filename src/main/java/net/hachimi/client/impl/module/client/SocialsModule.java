package net.hachimi.client.impl.module.client;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.module.ConcurrentModule;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.ClientColorEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class SocialsModule extends ConcurrentModule {
   private static SocialsModule INSTANCE;
   Config<Boolean> friendsConfig = this.register(new BooleanConfig("Friends", "Allows friend system to function", true));
   Config<Boolean> addNotifyConfig = this.register(
      new BooleanConfig("AddNotify", "Notifies players when you add them as a friend", false, () -> this.friendsConfig.getValue())
   );
   Config<Color> friendsColorConfig = this.register(
      new ColorConfig("FriendsColor", "The color for friends in the client", new Color(-10027009), false, false, () -> this.friendsConfig.getValue())
   );

   public SocialsModule() {
      super("Socials", "The client socials system", ModuleCategory.CLIENT);
      INSTANCE = this;
   }

   public static SocialsModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onClientFriendColor(ClientColorEvent.Friend event) {
      event.setRgb(this.getFriendRGB());
   }

   public boolean isFriendsEnabled() {
      return this.friendsConfig.getValue();
   }

   public boolean shouldNotify() {
      return this.addNotifyConfig.getValue();
   }

   public Color getFriendColor() {
      return this.friendsColorConfig.getValue();
   }

   public int getFriendRGB() {
      return this.getFriendColor().getRGB();
   }
}
