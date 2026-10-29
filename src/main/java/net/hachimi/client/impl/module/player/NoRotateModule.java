package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.ServerRotationEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.eventbus.annotation.EventListener;

public class NoRotateModule extends ToggleModule {
   Config<Boolean> positionAdjustConfig = this.register(new BooleanConfig("PositionAdjust", "Adjusts outgoing rotation packets", false));

   public NoRotateModule() {
      super("NoRotate", "Prevents server from forcing rotations", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onServerRotation(ServerRotationEvent event) {
      event.cancel();
      if (this.positionAdjustConfig.getValue()) {
         float yaw = Managers.ROTATION.getServerYaw();
         float pitch = Managers.ROTATION.getServerPitch();
         if (Managers.ROTATION.isRotating()) {
            yaw = Managers.ROTATION.getRotationYaw();
            pitch = Managers.ROTATION.getRotationPitch();
         }

         event.setYaw(yaw);
         event.setPitch(pitch);
      }
   }
}
