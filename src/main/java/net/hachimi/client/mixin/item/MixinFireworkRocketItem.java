package net.hachimi.client.mixin.item;

import net.hachimi.client.impl.event.item.FireworkUseEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FireworkRocketItem.class})
public class MixinFireworkRocketItem {
   @Inject(
      method = {"use"},
      at = {@At("HEAD")}
   )
   private void hookUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
      FireworkUseEvent fireworkUseEvent = new FireworkUseEvent();
      EventBus.INSTANCE.dispatch(fireworkUseEvent);
   }
}
