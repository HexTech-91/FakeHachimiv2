package net.hachimi.client.impl.module.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.config.setting.StringConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class KitModule extends ToggleModule {
   private static KitModule INSTANCE;
   private final Map<Integer, Item> kitInventory = new ConcurrentHashMap<>();
   Config<String> nameConfig = this.register(new StringConfig("Name", "The name of the kit to save/load", "default"));
   Config<Boolean> saveConfig = this.register(new BooleanConfig("Save", "Saves your current inventory as the named kit", false) {
      @Override
      public void setValue(Boolean in) {
         super.setValue(in);
         if (in) {
            KitModule.this.saveCurrentInventoryAsKit(KitModule.this.nameConfig.getValue());
            super.setValue(false);
         }
      }
   });
   Config<Boolean> loadConfig = this.register(new BooleanConfig("Load", "Loads the named kit and swaps your inventory to match it", false) {
      @Override
      public void setValue(Boolean in) {
         super.setValue(in);
         if (in) {
            if (KitModule.this.loadKit(KitModule.this.nameConfig.getValue())) {
               KitModule.this.applyKitToInventory();
            }

            super.setValue(false);
         }
      }
   });

   public KitModule() {
      super("Kit", "Saves and loads inventory kits", ModuleCategory.MISCELLANEOUS);
      INSTANCE = this;
   }

   public static KitModule getInstance() {
      return INSTANCE;
   }

   public String getKitName() {
      return this.nameConfig.getValue();
   }

   public void setKitName(String name) {
      this.nameConfig.setValue(name);
   }

   public boolean isPartOfKit(Item item) {
      return this.kitInventory.isEmpty() ? true : this.kitInventory.containsValue(item);
   }

   public boolean hasKitLoaded() {
      return !this.kitInventory.isEmpty();
   }

   public Map<Integer, Item> getKitItems() {
      return Collections.unmodifiableMap(this.kitInventory);
   }

   public String getActiveKitName() {
      return this.kitInventory.isEmpty() ? null : this.nameConfig.getValue();
   }

   public boolean isOwnShulker(BlockPos pos) {
      return false;
   }

   public void applyKitToInventory() {
      if (mc.player != null && !this.kitInventory.isEmpty()) {
         int syncId = mc.player.playerScreenHandler.syncId;

         for (int targetSlot = 0; targetSlot < 36; targetSlot++) {
            Item required = this.kitInventory.getOrDefault(targetSlot, Items.AIR);
            if (required != Items.AIR) {
               ItemStack current = mc.player.getInventory().getStack(targetSlot);
               if (current.getItem() != required) {
                  int source = this.findItemInInventory(required, targetSlot);
                  if (source != -1) {
                     int handlerTarget = this.invIndexToHandlerSlot(targetSlot);
                     int handlerSource = this.invIndexToHandlerSlot(source);
                     mc.interactionManager.clickSlot(syncId, handlerSource, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(syncId, handlerTarget, 0, SlotActionType.PICKUP, mc.player);
                     mc.interactionManager.clickSlot(syncId, handlerSource, 0, SlotActionType.PICKUP, mc.player);
                  }
               }
            }
         }

         ChatUtil.clientSendMessage("Kit '" + this.nameConfig.getValue() + "' applied!");
      }
   }

   private int invIndexToHandlerSlot(int invIndex) {
      return invIndex < 9 ? invIndex + 36 : invIndex;
   }

   private int findItemInInventory(Item item, int excludeSlot) {
      for (int i = 0; i < 36; i++) {
         if (i != excludeSlot && mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public void saveCurrentInventoryAsKit(String kitName) {
      if (mc.player != null) {
         this.kitInventory.clear();

         for (int i = 0; i < 36; i++) {
            this.kitInventory.put(i, mc.player.getInventory().getStack(i).getItem());
         }

         try {
            Path file = this.getKitFile(kitName);
            JsonObject out = new JsonObject();
            JsonArray items = new JsonArray();

            for (int i = 0; i < 36; i++) {
               Item item = this.kitInventory.getOrDefault(i, Items.AIR);
               Identifier id = Registries.ITEM.getId(item);
               items.add(id.toString());
            }

            out.add("items", items);
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
            ChatUtil.clientSendMessage("Kit '" + kitName + "' saved to " + file + "!");
         } catch (IOException var8) {
            Hachimi.error("Could not save kit '{}'", kitName);
            var8.printStackTrace();
            ChatUtil.error("Could not save kit '" + kitName + "'!");
         }
      }
   }

   public boolean loadKit(String kitName) {
      try {
         Path file = this.getKitFile(kitName);
         if (!Files.exists(file)) {
            ChatUtil.error("Kit '" + kitName + "' does not exist!");
            return false;
         } else {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject in = JsonParser.parseString(content).getAsJsonObject();
            if (!in.has("items")) {
               ChatUtil.error("Kit '" + kitName + "' is invalid!");
               return false;
            } else {
               JsonArray items = in.getAsJsonArray("items");
               this.kitInventory.clear();

               for (int i = 0; i < items.size() && i < 36; i++) {
                  Identifier id = Identifier.tryParse(items.get(i).getAsString());
                  Item item = id != null && Registries.ITEM.containsId(id) ? Registries.ITEM.get(id) : Items.AIR;
                  this.kitInventory.put(i, item);
               }

               ChatUtil.clientSendMessage("Kit '" + kitName + "' loaded!");
               return true;
            }
         }
      } catch (IOException var9) {
         Hachimi.error("Could not load kit '{}'", kitName);
         var9.printStackTrace();
         ChatUtil.error("Could not load kit '" + kitName + "'!");
         return false;
      }
   }

   public void deleteKit(String kitName) {
      try {
         Path file = this.getKitFile(kitName);
         if (!Files.exists(file)) {
            ChatUtil.error("Kit '" + kitName + "' does not exist!");
            return;
         }

         Files.delete(file);
         ChatUtil.clientSendMessage("Kit '" + kitName + "' deleted!");
      } catch (IOException var3) {
         Hachimi.error("Could not delete kit '{}'", kitName);
         var3.printStackTrace();
         ChatUtil.error("Could not delete kit '" + kitName + "'!");
      }
   }

   private Path getKitFile(String kitName) throws IOException {
      Path kitsDir = mc.runDirectory.toPath().resolve("hachimi").resolve("Kits");
      if (!Files.exists(kitsDir)) {
         Files.createDirectories(kitsDir);
      }

      String safeName = kitName.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
      return kitsDir.resolve(safeName + ".json");
   }
}
