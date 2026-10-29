package net.hachimi.client.mixin.entity.mob;

import net.hachimi.client.impl.event.entity.mob.PigAIEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PigEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MobEntity.class})
public class MixinMobEntity {
   @Inject(
      method = {"isAiDisabled"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hookIsAiDisabled(CallbackInfoReturnable<Boolean> cir) {
      MobEntity mobEntity = (MobEntity)(Object)this;
      if (mobEntity instanceof PigEntity) {
         PigAIEvent pigAIEvent = new PigAIEvent((PigEntity)mobEntity);
         EventBus.INSTANCE.dispatch(pigAIEvent);
         if (pigAIEvent.isCanceled()) {
            cir.cancel();
            cir.setReturnValue(true);
         }
      }
   }
}
