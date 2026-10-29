package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.LookDirectionEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.hachimi.eventbus.event.StageEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.util.math.MathHelper;

public class YawModule extends ToggleModule {
   Config<Boolean> lockConfig = this.register(new BooleanConfig("Lock", "Locks the yaw in cardinal direction", false));

   public YawModule() {
      super("Yaw", "Locks player yaw to a cardinal axis", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      if (event.getStage() == StageEvent.EventStage.PRE) {
         float yaw = Math.round(mc.player.getYaw() / 45.0F) * 45.0F;
         Entity vehicle = mc.player.getVehicle();
         if (vehicle != null) {
            vehicle.setYaw(yaw);
            if (vehicle instanceof LlamaEntity llama) {
               llama.setHeadYaw(yaw);
            }

            return;
         }

         mc.player.setYaw(yaw);
         mc.player.setHeadYaw(yaw);
      }
   }

   @EventListener
   public void onLookDirection(LookDirectionEvent event) {
      if (this.lockConfig.getValue()) {
         event.cancel();
         float f = (float)event.getCursorDeltaY() * 0.15F;
         mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + f, -90.0F, 90.0F));
      }
   }
}
