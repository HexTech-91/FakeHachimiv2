package net.hachimi.client.impl.module;

import java.util.Comparator;
import java.util.function.Predicate;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.init.Managers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class CombatModule extends RotationModule {
   protected Config<Boolean> multitaskConfig = this.register(new BooleanConfig("Multitask", "Allows actions while using items", false));

   public CombatModule(String name, String desc, ModuleCategory category) {
      super(name, desc, category);
   }

   public CombatModule(String name, String desc, ModuleCategory category, int rotationPriority) {
      super(name, desc, category, rotationPriority);
   }

   public PlayerEntity getClosestPlayer(double range) {
      return mc.world
         .getPlayers()
         .stream()
         .filter(e -> !(e instanceof ClientPlayerEntity) && !e.isSpectator())
         .filter(e -> mc.player.squaredDistanceTo(e) <= range * range)
         .filter(e -> !Managers.SOCIAL.isFriend(e.getName().getString()))
         .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
         .orElse(null);
   }

   public PlayerEntity getClosestPlayer(Predicate<AbstractClientPlayerEntity> entityPredicate, double range) {
      return mc.world
         .getPlayers()
         .stream()
         .filter(e -> !(e instanceof ClientPlayerEntity) && !e.isSpectator())
         .filter(entityPredicate)
         .filter(e -> mc.player.squaredDistanceTo(e) <= range * range)
         .filter(e -> !Managers.SOCIAL.isFriend(e.getName().getString()))
         .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
         .orElse(null);
   }

   public boolean checkMultitask() {
      return this.checkMultitask(false);
   }

   public boolean checkMultitask(boolean checkOffhand) {
      return checkOffhand && mc.player.getActiveHand() != Hand.MAIN_HAND ? false : mc.player.isUsingItem();
   }
}
