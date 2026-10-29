package net.hachimi.client.init;

import net.hachimi.client.api.render.layers.LightmapManager;
import net.hachimi.client.api.render.shader.ShaderManager;
import net.hachimi.client.impl.manager.EventManager;
import net.hachimi.client.impl.manager.ModuleManager;
import net.hachimi.client.impl.manager.anticheat.AntiCheatManager;
import net.hachimi.client.impl.manager.client.AccountManager;
import net.hachimi.client.impl.manager.client.CommandManager;
import net.hachimi.client.impl.manager.client.MacroManager;
import net.hachimi.client.impl.manager.client.SocialManager;
import net.hachimi.client.impl.manager.client.cape.CapeManager;
import net.hachimi.client.impl.manager.combat.HitboxManager;
import net.hachimi.client.impl.manager.combat.PearlManager;
import net.hachimi.client.impl.manager.combat.TotemManager;
import net.hachimi.client.impl.manager.combat.hole.HoleManager;
import net.hachimi.client.impl.manager.mojang.LookupManager;
import net.hachimi.client.impl.manager.network.NetworkManager;
import net.hachimi.client.impl.manager.player.InventoryManager;
import net.hachimi.client.impl.manager.player.MovementManager;
import net.hachimi.client.impl.manager.player.PositionManager;
import net.hachimi.client.impl.manager.player.interaction.InteractionManager;
import net.hachimi.client.impl.manager.player.rotation.RotationManager;
import net.hachimi.client.impl.manager.world.BlockManager;
import net.hachimi.client.impl.manager.world.BreakManager;
import net.hachimi.client.impl.manager.world.WaypointManager;
import net.hachimi.client.impl.manager.world.sound.SoundManager;
import net.hachimi.client.impl.manager.world.tick.TickManager;

public class Managers {
   public static NetworkManager NETWORK;
   public static MacroManager MACRO;
   public static ModuleManager MODULE;
   public static EventManager EVENT;
   public static CommandManager COMMAND;
   public static SocialManager SOCIAL;
   public static WaypointManager WAYPOINT;
   public static AccountManager ACCOUNT;
   public static TickManager TICK;
   public static InventoryManager INVENTORY;
   public static PositionManager POSITION;
   public static RotationManager ROTATION;
   public static AntiCheatManager ANTICHEAT;
   public static MovementManager MOVEMENT;
   public static HoleManager HOLE;
   public static TotemManager TOTEM;
   public static InteractionManager INTERACT;
   public static SoundManager SOUND;
   public static CapeManager CAPES;
   public static ShaderManager SHADER;
   public static LookupManager LOOKUP;
   public static LightmapManager LIGHT_MAP;
   public static BlockManager BLOCK;
   public static BreakManager BREAK;
   public static HitboxManager HITBOX;
   public static PearlManager PEARL;
   private static boolean initialized;

   public static void init() {
      if (!isInitialized()) {
         NETWORK = new NetworkManager();
         MACRO = new MacroManager();
         MODULE = new ModuleManager();
         EVENT = new EventManager();
         SOCIAL = new SocialManager();
         WAYPOINT = new WaypointManager();
         ACCOUNT = new AccountManager();
         TICK = new TickManager();
         INVENTORY = new InventoryManager();
         POSITION = new PositionManager();
         ROTATION = new RotationManager();
         BLOCK = new BlockManager();
         BREAK = new BreakManager();
         HITBOX = new HitboxManager();
         PEARL = new PearlManager();
         ANTICHEAT = new AntiCheatManager();
         MOVEMENT = new MovementManager();
         HOLE = new HoleManager();
         TOTEM = new TotemManager();
         INTERACT = new InteractionManager();
         COMMAND = new CommandManager();
         SOUND = new SoundManager();
         SHADER = new ShaderManager();
         LOOKUP = new LookupManager();
         initialized = true;
      }
   }

   public static void postInit() {
      if (isInitialized()) {
         MACRO.postInit();
         ACCOUNT.postInit();
         CAPES = new CapeManager();
         LIGHT_MAP = new LightmapManager();
      }
   }

   public static boolean isInitialized() {
      return initialized;
   }
}
