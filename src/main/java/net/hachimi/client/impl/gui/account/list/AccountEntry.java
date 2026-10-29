package net.hachimi.client.impl.gui.account.list;

import net.hachimi.client.api.account.type.MinecraftAccount;
import net.hachimi.client.api.account.type.impl.CrackedAccount;
import net.hachimi.client.api.account.type.impl.MicrosoftAccount;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.network.TextureDownloader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget.Entry;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AccountEntry extends Entry<AccountEntry> implements Globals {
   private static final TextureDownloader FACE_DOWNLOADER = new TextureDownloader();
   private final MinecraftAccount account;
   private long lastClickTime = -1L;

   public AccountEntry(MinecraftAccount account) {
      this.account = account;
   }

   @Override
   public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
      context.drawTextWithShadow(mc.textRenderer, Text.of(this.account.username()), x + 20, y + entryHeight / 2 - 9 / 2, hovered ? 5635925 : -1);
      if (this.account instanceof CrackedAccount || this.account instanceof MicrosoftAccount msa && msa.getUsernameOrNull() != null) {
         String id = "face_" + this.account.username().toLowerCase();
         if (!FACE_DOWNLOADER.exists(id)) {
            if (!FACE_DOWNLOADER.isDownloading(id)) {
               FACE_DOWNLOADER.downloadTexture(id, "https://minotar.net/helm/" + this.account.username() + "/15", false);
            }

            return;
         }

         Identifier texture = FACE_DOWNLOADER.get(id);
         if (texture != null) {
            context.drawTexture(texture, x + 2, y + 2, 0.0F, 0.0F, 15, 15, 15, 15);
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         long time = System.currentTimeMillis() - this.lastClickTime;
         if (time > 0L && time < 500L) {
            Session session = this.account.login();
            if (session != null) {
               Managers.ACCOUNT.setSession(session);
            }
         }

         this.lastClickTime = System.currentTimeMillis();
         return false;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public Text getNarration() {
      return this.account instanceof MicrosoftAccount msa && msa.username() == null ? null : Text.of(this.account.username());
   }

   public MinecraftAccount getAccount() {
      return this.account;
   }
}
