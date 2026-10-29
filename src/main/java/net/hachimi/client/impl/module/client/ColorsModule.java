package net.hachimi.client.impl.module.client;

import java.awt.Color;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.ColorConfig;
import net.hachimi.client.api.module.ConcurrentModule;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.impl.event.ClientColorEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.util.math.MathHelper;

public class ColorsModule extends ConcurrentModule {
   private static ColorsModule INSTANCE;
   Config<Color> colorConfig = this.register(new ColorConfig("Global", "The primary client color", new Color(130, 60, 220), false, false));

   public ColorsModule() {
      super("Colors", "Client color scheme", ModuleCategory.CLIENT);
      INSTANCE = this;
   }

   public static ColorsModule getInstance() {
      return INSTANCE;
   }

   @EventListener
   public void onClientColor(ClientColorEvent event) {
      event.setRgb(this.getRGB());
   }

   public Color getColor() {
      return this.colorConfig.getValue();
   }

   public Color getColor(float alpha) {
      ColorConfig config = (ColorConfig)(Object)this.colorConfig;
      return new Color(config.getRed() / 255.0F, config.getGreen() / 255.0F, config.getBlue() / 255.0F, MathHelper.clamp(alpha, 0.0F, 1.0F));
   }

   public Color getColor(int alpha) {
      ColorConfig config = (ColorConfig)(Object)this.colorConfig;
      return new Color(config.getRed(), config.getGreen(), config.getBlue(), MathHelper.clamp(alpha, 0, 255));
   }

   public Integer getRGB() {
      return this.getColor().getRGB();
   }

   public int getRGB(int a) {
      return this.getColor(a).getRGB();
   }
}
