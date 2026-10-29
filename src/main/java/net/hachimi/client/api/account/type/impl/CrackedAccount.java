package net.hachimi.client.api.account.type.impl;

import java.util.Optional;
import java.util.UUID;
import net.hachimi.client.api.account.type.MinecraftAccount;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;

public record CrackedAccount(String username) implements MinecraftAccount {
   @Override
   public Session login() {
      return new Session(this.username(), UUID.randomUUID(), "", Optional.empty(), Optional.empty(), AccountType.LEGACY);
   }
}
