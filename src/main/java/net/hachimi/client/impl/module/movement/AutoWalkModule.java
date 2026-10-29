package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;

public class AutoWalkModule extends ToggleModule {
   private static AutoWalkModule INSTANCE;
   Config<Boolean> lockConfig = this.register(new BooleanConfig("Lock", "Stops movement when sneaking or jumping", false));

   public AutoWalkModule() {
      super("AutoWalk", "Automatically moves forward", ModuleCategory.MOVEMENT);
      INSTANCE = this;
   }

   public static AutoWalkModule getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      System.out.println("Baritone has been removed");
   }

   @Override
   public void onDisable() {
      mc.options.forwardKey.setPressed(false);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         mc.options
            .forwardKey
            .setPressed(!mc.options.sneakKey.isPressed() && (!this.lockConfig.getValue() || !mc.options.jumpKey.isPressed() && mc.player.isOnGround()));
      }
   }
}
