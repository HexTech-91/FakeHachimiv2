package net.hachimi.client.impl.manager.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.command.Command;
import net.hachimi.client.api.module.Module;
import net.hachimi.client.impl.command.BindCommand;
import net.hachimi.client.impl.command.ConfigCommand;
import net.hachimi.client.impl.command.ConnectCommand;
import net.hachimi.client.impl.command.CoordsCommand;
import net.hachimi.client.impl.command.CustomFontCommand;
import net.hachimi.client.impl.command.DisableAllCommand;
import net.hachimi.client.impl.command.DrawnCommand;
import net.hachimi.client.impl.command.FriendCommand;
import net.hachimi.client.impl.command.HClipCommand;
import net.hachimi.client.impl.command.HelpCommand;
import net.hachimi.client.impl.command.HideAllCommand;
import net.hachimi.client.impl.command.HistoryCommand;
import net.hachimi.client.impl.command.KitCommand;
import net.hachimi.client.impl.command.LeaveCommand;
import net.hachimi.client.impl.command.LoadCommand;
import net.hachimi.client.impl.command.MacroCommand;
import net.hachimi.client.impl.command.ModuleCommand;
import net.hachimi.client.impl.command.ModulesCommand;
import net.hachimi.client.impl.command.NbtCommand;
import net.hachimi.client.impl.command.NotifyCommand;
import net.hachimi.client.impl.command.OpenFolderCommand;
import net.hachimi.client.impl.command.PrefixCommand;
import net.hachimi.client.impl.command.QueueCommand;
import net.hachimi.client.impl.command.ReloadSoundCommand;
import net.hachimi.client.impl.command.ResetCommand;
import net.hachimi.client.impl.command.ResetGuiCommand;
import net.hachimi.client.impl.command.SaveCommand;
import net.hachimi.client.impl.command.SkinGrabCommand;
import net.hachimi.client.impl.command.StatsCommand;
import net.hachimi.client.impl.command.ToggleCommand;
import net.hachimi.client.impl.command.VClipCommand;
import net.hachimi.client.impl.command.VanishCommand;
import net.hachimi.client.impl.command.WaypointCommand;
import net.hachimi.client.impl.command.YawCommand;
import net.hachimi.client.impl.event.gui.chat.ChatMessageEvent;
import net.hachimi.client.impl.event.gui.screen.SuggestChatEvent;
import net.hachimi.client.impl.event.keyboard.KeyboardInputEvent;
import net.hachimi.client.init.Managers;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;

public class CommandManager implements Globals {
   private final List<Command> commands = new ArrayList<>();
   private String prefix = ".";
   private int prefixKey = 46;
   private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
   private final CommandSource source = new ClientCommandSource(null, mc);

   public CommandManager() {
      EventBus.INSTANCE.subscribe(this);
      this.register(
         new BindCommand(),
         new ConfigCommand(),
         new ConnectCommand(),
         new CoordsCommand(),
         new DisableAllCommand(),
         new DrawnCommand(),
         new CustomFontCommand(),
         new FriendCommand(),
         new HClipCommand(),
         new HelpCommand(),
         new HideAllCommand(),
         new HistoryCommand(),
         new KitCommand(),
         new LeaveCommand(),
         new LoadCommand(),
         new MacroCommand(),
         new ModulesCommand(),
         new NbtCommand(),
         new NotifyCommand(),
         new OpenFolderCommand(),
         new PrefixCommand(),
         new QueueCommand(),
         new ResetCommand(),
         new ResetGuiCommand(),
         new SaveCommand(),
         new ReloadSoundCommand(),
         new SkinGrabCommand(),
         new StatsCommand(),
         new ToggleCommand(),
         new VanishCommand(),
         new VClipCommand(),
         new WaypointCommand(),
         new YawCommand()
      );

      for (Module module : Managers.MODULE.getModules()) {
         this.register(new ModuleCommand(module));
      }

      Hachimi.info("Registered {} commands!", this.commands.size());

      for (Command command : this.commands) {
         for (LiteralArgumentBuilder<CommandSource> builder : command.getCommandBuilders()) {
            command.buildCommand(builder);
            this.dispatcher.register(builder);
         }
      }
   }

   @EventListener(
      priority = 999
   )
   public void onChatMessage(ChatMessageEvent.Client event) {
      String text = event.getMessage().trim();
      if (text.startsWith(this.prefix)) {
         String literal = text.substring(1);
         event.cancel();
         mc.inGameHud.getChatHud().addToMessageHistory(text);

         try {
            this.dispatcher.execute(this.dispatcher.parse(literal, this.source));
         } catch (Exception var5) {
         }
      }
   }

   @EventListener
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (event.getAction() == 1 && event.getKeycode() == this.prefixKey && mc.currentScreen == null) {
         event.cancel();
         mc.setScreen(new ChatScreen(""));
      }
   }

   @EventListener
   public void onChatSuggest(SuggestChatEvent event) {
      event.setPrefix(this.prefix);
      event.setDispatcher(this.dispatcher);
      event.setSource(this.source);
   }

   private LiteralArgumentBuilder<Object> redirectBuilder(String alias, LiteralCommandNode<?> destination) {
      LiteralArgumentBuilder<Object> literalArgumentBuilder = (LiteralArgumentBuilder<Object>)LiteralArgumentBuilder.literal(alias.toLowerCase())
         .requires((Predicate)destination.getRequirement())
         .forward(destination.getRedirect(), destination.getRedirectModifier(), destination.isFork())
         .executes(destination.getCommand());

      for (CommandNode<?> child : destination.getChildren()) {
         literalArgumentBuilder.then((CommandNode<Object>)child);
      }

      return literalArgumentBuilder;
   }

   private void register(Command... commands) {
      for (Command command : commands) {
         this.register(command);
      }
   }

   private void register(Command command) {
      this.commands.add(command);
   }

   public List<Command> getCommands() {
      return this.commands;
   }

   public Command getCommand(String name) {
      for (Command command : this.commands) {
         if (command.getName().equalsIgnoreCase(name)) {
            return command;
         }
      }

      return null;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix, int prefixKey) {
      this.prefix = prefix;
      this.prefixKey = prefixKey;
   }

   public CommandDispatcher<CommandSource> getDispatcher() {
      return this.dispatcher;
   }

   public CommandSource getSource() {
      return this.source;
   }
}
