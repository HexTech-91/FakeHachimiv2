package net.hachimi.client.util.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Set;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public class EnchantmentUtil {
   public static int getLevel(ItemStack stack, RegistryKey<Enchantment> enchantmentRegistryKey) {
      if (!stack.getComponents().contains(DataComponentTypes.ENCHANTMENTS)) {
         return 0;
      } else {
         for (Entry<RegistryEntry<Enchantment>> e : stack.getComponents().get(DataComponentTypes.ENCHANTMENTS).getEnchantmentEntries()) {
            if (e.getKey().getKey().isPresent() && e.getKey().getKey().get().equals(enchantmentRegistryKey)) {
               return e.getIntValue();
            }
         }

         return 0;
      }
   }

   public static boolean isFakeEnchant2b2t(ItemStack itemStack) {
      Set<Entry<RegistryEntry<Enchantment>>> enchants = EnchantmentHelper.getEnchantments(itemStack).getEnchantmentEntries();
      if (enchants.size() > 1) {
         return false;
      } else {
         for (Entry<RegistryEntry<Enchantment>> e : enchants) {
            RegistryEntry<Enchantment> enchantment = e.getKey();
            int lvl = e.getIntValue();
            if (lvl == 0 && enchantment.getKey().isPresent() && enchantment.getKey().get() == Enchantments.PROTECTION) {
               return true;
            }
         }

         return false;
      }
   }
}
