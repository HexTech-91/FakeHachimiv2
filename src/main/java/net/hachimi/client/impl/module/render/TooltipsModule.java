package net.hachimi.client.impl.module.render;

import net.hachimi.client.api.config.Config;
import net.hachimi.client.api.config.setting.BooleanConfig;
import net.hachimi.client.api.module.ModuleCategory;
import net.hachimi.client.api.module.ToggleModule;
import net.hachimi.client.api.render.RenderManager;
import net.hachimi.client.impl.event.gui.RenderTooltipEvent;
import net.hachimi.client.impl.module.client.ColorsModule;
import net.hachimi.client.util.world.ItemUtil;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.collection.DefaultedList;

public class TooltipsModule extends ToggleModule {
   Config<Boolean> shulkersConfig = this.register(new BooleanConfig("Shulkers", "Renders all the contents of shulkers in tooltips", true));
   Config<Boolean> mapsConfig = this.register(new BooleanConfig("Maps", "Renders a preview of maps in tooltips", false));

   public TooltipsModule() {
      super("Tooltips", "Renders detailed tooltips showing items", ModuleCategory.RENDER);
   }

   @EventListener
   public void onRenderTooltip(RenderTooltipEvent event) {
      ItemStack stack = event.getStack();
      if (!stack.isEmpty()) {
         if (this.shulkersConfig.getValue() && ItemUtil.isShulker(stack.getItem())) {
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container == null || container.streamNonEmpty().findAny().isEmpty()) {
               return;
            }

            event.cancel();
            event.context.getMatrices().push();
            event.context.getMatrices().translate(0.0F, 0.0F, 600.0F);
            DefaultedList<ItemStack> shulkerInventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
            container.stream().filter(itemStackx -> !itemStackx.isEmpty()).forEach(itemStackx -> {
               for (int ix = 0; ix < shulkerInventory.size(); ix++) {
                  if (shulkerInventory.get(ix).isEmpty()) {
                     shulkerInventory.set(ix, itemStackx.copy());
                     break;
                  }
               }
            });
            RenderManager.rect(event.context.getMatrices(), event.getX() + 8.0, event.getY() - 21.0, 150.0, 13.0, ColorsModule.getInstance().getRGB(170));
            RenderManager.enableScissor(event.getX() + 8.0, event.getY() - 21.0, event.getX() + 158.0, event.getY() - 8.0);
            RenderManager.renderText(event.getContext(), stack.getName().getString(), event.getX() + 11.0F, event.getY() - 18.0F, -1);
            RenderManager.disableScissor();
            RenderManager.rect(event.context.getMatrices(), event.getX() + 8.0, event.getY() - 8.0, 150.0, 55.0, 1996488704);

            for (int i = 0; i < shulkerInventory.size(); i++) {
               ItemStack itemStack = shulkerInventory.get(i);
               if (!itemStack.isEmpty()) {
                  int x = event.getX() + i % 9 * 16 + 9;
                  int y = event.getY() + i / 9 * 16 - 5;
                  event.context.drawItem(itemStack, x, y);
                  event.context.drawItemInSlot(mc.textRenderer, itemStack, x, y);
               }
            }

            event.context.getMatrices().pop();
         } else if (this.mapsConfig.getValue() && stack.getItem() instanceof FilledMapItem) {
            event.cancel();
            event.context.getMatrices().push();
            event.context.getMatrices().translate(0.0F, 0.0F, 600.0F);
            RenderManager.rect(event.context.getMatrices(), event.getX() + 8.0, event.getY() - 21.0, 128.0, 13.0, ColorsModule.getInstance().getRGB(170));
            RenderManager.enableScissor(event.getX() + 8.0, event.getY() - 21.0, event.getX() + 132.0, event.getY() - 8.0);
            RenderManager.renderText(event.getContext(), stack.getName().getString(), event.getX() + 11.0F, event.getY() - 18.0F, -1);
            RenderManager.disableScissor();
            event.getContext().getMatrices().translate(event.getX() + 8.0F, event.getY() - 8.0F, 0.0F);
            MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
            MapState mapState = FilledMapItem.getMapState(mapIdComponent, mc.world);
            if (mapState != null) {
               mc.gameRenderer
                  .getMapRenderer()
                  .draw(event.getContext().getMatrices(), event.getContext().getVertexConsumers(), mapIdComponent, mapState, true, 1);
            }

            event.context.getMatrices().pop();
         }
      }
   }
}
