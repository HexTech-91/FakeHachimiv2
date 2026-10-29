package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.render.CameraClipEvent;
import net.hachimi.eventbus.annotation.EventListener;

public class ViewClipModule extends ToggleModule {
   Config<Float> distanceConfig = this.register(new NumberConfig<>("Distance", "The third-person camera clip distance", 1.0F, 3.5F, 20.0F));

   public ViewClipModule() {
      super("ViewClip", "Clips your third-person camera through blocks", ModuleCategory.RENDER);
   }

   @EventListener
   public void onCameraClip(CameraClipEvent event) {
      event.cancel();
      event.setDistance(this.distanceConfig.getValue());
   }
}
