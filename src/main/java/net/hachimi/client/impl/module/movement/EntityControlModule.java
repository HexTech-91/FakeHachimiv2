package net.hachimi.client.impl.module.movement;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.TickEvent;
import net.hachimi.client.impl.event.entity.mob.PigAIEvent;
import net.hachimi.client.impl.event.entity.passive.EntitySteerEvent;
import net.hachimi.client.impl.event.network.MountJumpStrengthEvent;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.LlamaEntity;

public class EntityControlModule extends ToggleModule {
   Config<Float> jumpStrengthConfig = this.register(new NumberConfig<>("JumpStrength", "The fixed jump strength of the mounted entity", 0.1F, 0.7F, 2.0F));
   Config<Boolean> noPigMoveConfig = this.register(new BooleanConfig("NoPigAI", "Prevents the pig movement when controlling pigs", false));

   public EntityControlModule() {
      super("EntityControl", "Allows you to steer entities without a saddle", ModuleCategory.MOVEMENT);
   }

   @EventListener
   public void onTick(TickEvent event) {
      Entity vehicle = mc.player.getVehicle();
      if (vehicle != null) {
         vehicle.setYaw(mc.player.getYaw());
         if (vehicle instanceof LlamaEntity llama) {
            llama.headYaw = mc.player.getYaw();
         }
      }
   }

   @EventListener
   public void onEntitySteer(EntitySteerEvent event) {
      event.cancel();
   }

   @EventListener
   public void onMountJumpStrength(MountJumpStrengthEvent event) {
      event.cancel();
      event.setJumpStrength(this.jumpStrengthConfig.getValue());
   }

   @EventListener
   public void onPigAI(PigAIEvent event) {
      if (mc.player != null) {
         if (this.noPigMoveConfig.getValue() && event.getPigEntity().getPassengerList().contains(mc.player)) {
            event.cancel();
         }
      }
   }
}
