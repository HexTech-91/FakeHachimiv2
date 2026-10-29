package net.hachimi.client;

public class ShutdownHook extends Thread {
   public ShutdownHook() {
      this.setName("Hachimi-ShutdownHook");
   }

   @Override
   public void run() {
      Hachimi.info("Saving configurations and shutting down!");
      Hachimi.CONFIG.saveClient();
      Hachimi.CONFIG.saveClickGui();
      Hachimi.CONFIG.saveFonts();
   }
}
