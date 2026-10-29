package net.hachimi.client.impl.event.entity;

import net.hachimi.eventbus.event.Event;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

public class StatusEffectEvent extends Event {
   private final StatusEffectInstance statusEffectInstance;

   public StatusEffectEvent(StatusEffectInstance statusEffectInstance) {
      this.statusEffectInstance = statusEffectInstance;
   }

   public StatusEffectInstance getStatusEffect() {
      return this.statusEffectInstance;
   }

   public static class Add extends StatusEffectEvent {
      public Add(StatusEffectInstance statusEffectInstance) {
         super(statusEffectInstance);
      }
   }

   public static class Remove extends StatusEffectEvent {
      private final RegistryEntry<StatusEffect> type;

      public Remove(RegistryEntry<StatusEffect> type) {
         super(null);
         this.type = type;
      }

      public RegistryEntry<StatusEffect> getType() {
         return this.type;
      }
   }
}
