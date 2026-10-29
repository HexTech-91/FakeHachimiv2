package net.hachimi.client.impl.gui.account.list;

import java.util.List;
import java.util.Objects;
import net.hachimi.client.api.account.type.MinecraftAccount;
import net.hachimi.client.init.Managers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;

public final class AccountListWidget extends AlwaysSelectedEntryListWidget<AccountEntry> {
   private String searchFilter;

   public AccountListWidget(MinecraftClient minecraftClient, int i, int j, int k, int l) {
      super(minecraftClient, i, j, k, l);
   }

   public void populateEntries() {
      this.clearEntries();
      List<MinecraftAccount> accounts = Managers.ACCOUNT.getAccounts();
      if (!accounts.isEmpty()) {
         for (MinecraftAccount account : accounts) {
            this.addEntry(new AccountEntry(account));
         }

         this.setSelected(this.getEntry(0));
      }
   }

   @Override
   protected void renderList(DrawContext context, int mouseX, int mouseY, float delta) {
      List<AccountEntry> entries = this.children();
      if (this.searchFilter != null && !this.searchFilter.isEmpty()) {
         entries = entries.stream().filter(entryx -> entryx.getAccount().username().toLowerCase().contains(this.searchFilter.toLowerCase())).toList();
      }

      int x = this.getRowLeft();
      int width = this.getRowWidth();
      int height = this.itemHeight - 4;
      int size = entries.size();

      for (int i = 0; i < size; i++) {
         int y = this.getRowTop(i);
         int m = this.getRowBottom(i);
         if (m >= this.getY() && y <= this.getBottom()) {
            AccountEntry entry = entries.get(i);
            boolean isHovered = Objects.equals(this.getHoveredEntry(), entry);
            entry.drawBorder(context, i, y, x, width, height, mouseX, mouseY, isHovered, delta);
            if (Objects.equals(this.getSelectedOrNull(), entry)) {
               int color = this.isFocused() ? -1 : -8355712;
               this.drawSelectionHighlight(context, y, width, height, color, -16777216);
            }

            boolean selected = this.client.getSession() != null && this.client.getSession().getUsername().equalsIgnoreCase(entry.getAccount().username());
            entry.render(context, i, y, x, width, height, mouseX, mouseY, selected, delta);
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.updateScrollingState(mouseX, mouseY, button);
      AccountEntry entry = this.getEntryAtPosition(mouseX, mouseY);
      if (entry != null) {
         this.setSelected(entry);
      }

      return this.getSelectedOrNull() != null ? this.getSelectedOrNull().mouseClicked(mouseX, mouseY, button) : true;
   }

   public void setSearchFilter(String searchFilter) {
      this.searchFilter = searchFilter;
   }
}
