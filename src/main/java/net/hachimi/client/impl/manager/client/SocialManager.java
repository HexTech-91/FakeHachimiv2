package net.hachimi.client.impl.manager.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.hachimi.client.api.social.SocialRelation;
import net.hachimi.client.impl.module.client.SocialsModule;
import net.hachimi.client.util.Globals;
import net.hachimi.client.util.chat.ChatUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

public class SocialManager implements Globals {
   private final ConcurrentMap<String, SocialRelation> relationships = new ConcurrentHashMap<>();

   public boolean isRelation(String name, SocialRelation relation) {
      return this.relationships.get(name) == relation;
   }

   public boolean isFriend(String name) {
      return !SocialsModule.getInstance().isFriendsEnabled() ? false : this.isRelation(name, SocialRelation.FRIEND);
   }

   public boolean isFriendInternal(String name) {
      return this.isRelation(name, SocialRelation.FRIEND);
   }

   public boolean isFriend(Text name) {
      return !SocialsModule.getInstance().isFriendsEnabled() ? false : name != null && this.isRelation(name.getString(), SocialRelation.FRIEND);
   }

   public void addRelation(String name, SocialRelation relation) {
      if (mc.player == null || !name.equals(mc.player.getDisplayName().getString())) {
         SocialRelation relationship = this.relationships.get(name);
         if (relationship != null) {
            this.relationships.replace(name, relation);
         } else {
            this.relationships.put(name, relation);
         }
      }
   }

   public void addFriend(String name) {
      this.addRelation(name, SocialRelation.FRIEND);
      if (SocialsModule.getInstance().shouldNotify() && mc.world != null) {
         for (PlayerListEntry playerListEntry : mc.player.networkHandler.getPlayerList()) {
            String playerName = playerListEntry.getProfile().getName();
            String[] names = playerName.split(" ");

            for (String name1 : names) {
               if (name1.equals(name)) {
                  ChatUtil.serverSendCommand("msg " + name + " I just added you as a friend on Hachimi!");
                  break;
               }
            }
         }
      }
   }

   public void addFriend(Text name) {
      this.addFriend(name.getString());
   }

   public SocialRelation remove(String playerName) {
      return this.relationships.remove(playerName);
   }

   public SocialRelation remove(Text playerName) {
      return this.relationships.remove(playerName.getString());
   }

   public Collection<String> getRelations(SocialRelation relation) {
      List<String> friends = new ArrayList<>();

      for (Entry<String, SocialRelation> relationship : this.relationships.entrySet()) {
         if (relationship.getValue() == relation) {
            friends.add(relationship.getKey());
         }
      }

      return friends;
   }

   public Collection<String> getFriends() {
      return this.getRelations(SocialRelation.FRIEND);
   }
}
