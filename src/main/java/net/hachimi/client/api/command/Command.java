package net.hachimi.client.api.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.minecraft.command.CommandSource;

public abstract class Command implements Globals {
   private final String name;
   private final String desc;
   private final List<LiteralArgumentBuilder<CommandSource>> builders = new ArrayList<>();

   public Command(String name, String desc, LiteralArgumentBuilder<CommandSource> builder) {
      this.name = name;
      this.desc = desc;
      this.builders.add(builder);
   }

   public Command(String name, String desc, List<LiteralArgumentBuilder<CommandSource>> builder) {
      this.name = name;
      this.desc = desc;
      this.builders.addAll(builder);
   }

   public abstract void buildCommand(LiteralArgumentBuilder<CommandSource> var1);

   protected static LiteralArgumentBuilder<CommandSource> literal(String name) {
      return LiteralArgumentBuilder.literal(name);
   }

   protected static List<LiteralArgumentBuilder<CommandSource>> literal(String... name) {
      List<LiteralArgumentBuilder<CommandSource>> builders = Lists.newArrayList();

      for (String s : name) {
         builders.add(LiteralArgumentBuilder.literal(s));
      }

      return builders;
   }

   protected static <T> RequiredArgumentBuilder<CommandSource, T> argument(String name, ArgumentType<T> type) {
      return RequiredArgumentBuilder.argument(name, type);
   }

   protected static SuggestionProvider<CommandSource> suggest(String... suggestions) {
      return (context, builder) -> CommandSource.suggestMatching(Lists.newArrayList(suggestions), builder);
   }

   public List<LiteralArgumentBuilder<CommandSource>> getCommandBuilders() {
      return this.builders;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.desc;
   }

   public String getUsage() {
      LiteralArgumentBuilder<CommandSource> builder = this.builders.getFirst();
      return Managers.COMMAND.getDispatcher().getAllUsage(builder.build(), Managers.COMMAND.getSource(), false)[0];
   }
}
