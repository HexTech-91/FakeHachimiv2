package net.hachimi.client.mixin.item;

import net.hachimi.client.impl.event.item.FinishUsingEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Item.class})
public class MixinItem {
   @Inject(
      method = {"finishUsing"},
      at = {@At("HEAD")}
   )
   private void hookFinishUsing(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
      FinishUsingEvent finishUsingEvent = new FinishUsingEvent(stack);
      EventBus.INSTANCE.dispatch(finishUsingEvent);
   }
}
