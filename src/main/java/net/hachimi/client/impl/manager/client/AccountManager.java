package net.hachimi.client.impl.manager.client;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.account.config.AccountFile;
import net.hachimi.client.api.account.config.EncryptedAccountFile;
import net.hachimi.client.api.account.msa.MSAAuthenticator;
import net.hachimi.client.api.account.type.MinecraftAccount;
import net.hachimi.client.mixin.accessor.AccessorMinecraftClient;
import net.hachimi.client.util.Globals;
import net.minecraft.client.session.Session;

public final class AccountManager implements Globals {
   public static final MSAAuthenticator MSA_AUTHENTICATOR = new MSAAuthenticator();
   private final List<MinecraftAccount> accounts = new LinkedList<>();
   private AccountFile configFile;

   public void postInit() {
      Path runDir = Hachimi.CONFIG.getClientDirectory();
      if (runDir.resolve("accounts_enc.json").toFile().exists()) {
         System.out.println("Encrypted account file exists");
         this.configFile = new EncryptedAccountFile(runDir);
      } else {
         System.out.println("Normal account file");
         this.configFile = new AccountFile(runDir);
      }

      Hachimi.CONFIG.addFile(this.configFile);
   }

   public void register(MinecraftAccount account) {
      this.accounts.add(account);
   }

   public void unregister(MinecraftAccount account) {
      this.accounts.remove(account);
   }

   public void setSession(Session session) {
      ((AccessorMinecraftClient)mc).setSession(session);
      Hachimi.info("Set session to {} ({})", session.getUsername(), session.getUuidOrNull());
   }

   public List<MinecraftAccount> getAccounts() {
      return this.accounts;
   }

   public boolean isEncrypted() {
      return this.configFile instanceof EncryptedAccountFile;
   }
}
