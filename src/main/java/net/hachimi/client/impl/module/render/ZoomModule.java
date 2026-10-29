package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.MacroConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.macro.Macro;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.keyboard.KeyboardInputEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;

public class ZoomModule extends ToggleModule {
   Config<Integer> zoomConfig = this.register(new NumberConfig<>("Zoom", "The zoom value", 10, 30, 50));
   Config<Boolean> smoothCameraConfig = this.register(new BooleanConfig("SmoothCamera", "Adds motion reduction to the camera", true));
   Config<Macro> zoomKeyConfig = this.register(new MacroConfig("ZoomKey", "The zoom key bind", new Macro(this.getId() + "-zoomkey", 67, () -> {})));
   private boolean flag;
   private boolean flag1 = true;
   private boolean isPressed;
   private int defaultFov = 100;

   public ZoomModule() {
      super("Zoom", "Zooms in the camera perspective", ModuleCategory.RENDER);
   }

   @EventListener
   public void onKey(KeyboardInputEvent event) {
      if (event.getAction() != 2 && event.getKeycode() == this.zoomKeyConfig.getValue().getKeycode()) {
         this.isPressed = event.getAction() == 1;
      }
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE && mc.currentScreen == null) {
         if (this.isPressed) {
            if (this.flag1) {
               this.defaultFov = mc.options.getFov().getValue();
               this.flag1 = false;
            }

            mc.options.smoothCameraEnabled = this.smoothCameraConfig.getValue();
            mc.options.hudHidden = true;
            mc.options.getFov().setValue(this.zoomConfig.getValue());
            this.flag = true;
         } else if (this.flag) {
            mc.options.smoothCameraEnabled = false;
            mc.options.hudHidden = false;
            mc.options.getFov().setValue(this.defaultFov);
            this.flag = false;
            this.flag1 = true;
         }
      }
   }
}
