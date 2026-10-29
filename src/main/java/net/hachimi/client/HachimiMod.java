package net.hachimi.client;

public class HachimiMod {
   public static final String MOD_NAME = "Hachimi";
   public static final String MOD_VER = "2.0";
   public static final String MOD_MC_VER = "1.21.1";

   public HachimiMod() {
      System.out.println("Baritone has been removed");
   }

   public void onInitializeClient() {
      Hachimi.init();
   }

   public static boolean isBaritonePresent() {
      return false;
   }
}
