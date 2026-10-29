package net.hachimi.client.impl.module.player;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.NumberConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.impl.event.network.PacketEvent;
import net.hachimi.client.impl.event.network.PlayerTickEvent;
import net.hachimi.client.impl.imixin.IPlayerInteractEntityC2SPacket;
import net.hachimi.client.util.network.InteractType;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.util.Hand;

public class AutoMountModule extends ToggleModule {
   Config<Float> rangeConfig = this.register(new NumberConfig<>("Range", "The range to mount entities", 0.1F, 4.0F, 6.0F));
   Config<Boolean> forceConfig = this.register(new BooleanConfig("ForceMount", "Forces the mounting without packets", false));
   Config<Boolean> horseConfig = this.register(new BooleanConfig("Horse", "Mounts horses", true));
   Config<Boolean> donkeyConfig = this.register(new BooleanConfig("Donkey", "Mounts donkeys", true));
   Config<Boolean> muleConfig = this.register(new BooleanConfig("Mule", "Mounts mules", true));
   Config<Boolean> llamaConfig = this.register(new BooleanConfig("Llama", "Mounts llamas", false));

   public AutoMountModule() {
      super("AutoMount", "Mounts nearby entities", ModuleCategory.PLAYER);
   }

   @EventListener
   public void onPlayerTick(PlayerTickEvent event) {
      if (!mc.player.isRiding()) {
         for (Entity entity : mc.world.getEntities()) {
            double dist = mc.player.getEyePos().distanceTo(entity.getPos());
            if (!(dist > this.rangeConfig.getValue().floatValue()) && this.checkMount(entity)) {
               mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
               return;
            }
         }
      }
   }

   @EventListener
   public void onPacketOutbound(PacketEvent.Outbound event) {
      if (event.getPacket() instanceof IPlayerInteractEntityC2SPacket packet
         && this.forceConfig.getValue()
         && packet.getType() == InteractType.INTERACT_AT
         && this.checkMount(packet.getEntity())) {
         event.cancel();
      }
   }

   private boolean checkMount(Entity entity) {
      return this.horseConfig.getValue() && (entity instanceof HorseEntity horseEntity && !horseEntity.isBaby() || entity instanceof SkeletonHorseEntity)
         || this.donkeyConfig.getValue() && entity instanceof DonkeyEntity
         || this.muleConfig.getValue() && entity instanceof MuleEntity
         || this.llamaConfig.getValue() && entity instanceof LlamaEntity llamaEntity && !llamaEntity.isBaby();
   }
}
