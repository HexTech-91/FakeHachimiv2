package net.hachimi.client.mixin.item;

import net.hachimi.client.impl.event.item.DurabilityEvent;
import net.hachimi.eventbus.EventBus;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemStack.class})
public abstract class MixinItemStack {
   @Shadow
   public abstract int getDamage();

   @Shadow
   public abstract void setDamage(int var1);

   @Inject(
      method = {"<init>(Lnet/minecraft/item/ItemConvertible;I)V"},
      at = {@At("RETURN")}
   )
   private void hookInitItem(ItemConvertible item, int count, CallbackInfo ci) {
      if (EventBus.INSTANCE != null) {
         DurabilityEvent durabilityEvent = new DurabilityEvent(this.getDamage());
         EventBus.INSTANCE.dispatch(durabilityEvent);
         if (durabilityEvent.isCanceled()) {
            this.setDamage(durabilityEvent.getDamage());
         }
      }
   }
}
