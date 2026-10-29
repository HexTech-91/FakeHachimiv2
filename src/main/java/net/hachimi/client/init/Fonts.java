package net.hachimi.client.init;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.hachimi.client.Hachimi;
import net.hachimi.client.impl.font.AWTFontRenderer;
import net.hachimi.client.impl.font.VanillaTextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class Fonts {
   public static final VanillaTextRenderer VANILLA = new VanillaTextRenderer();
   public static final String DEFAULT_FONT_FILE_PATH = "assets/hachimiclient/font/verdana.ttf";
   public static String FONT_FILE_PATH = "assets/hachimiclient/font/verdana.ttf";
   public static AWTFontRenderer CLIENT;
   public static AWTFontRenderer CLIENT_UNSCALED;
   public static float FONT_SIZE = 9.0F;
   private static boolean initialized;

   public static void init() {
      if (!initialized) {
         Hachimi.CONFIG.loadFonts();
         loadFonts();
         Hachimi.info("Loaded fonts!");
         initialized = true;
      }
   }

   public static void loadFonts() {
      try {
         Identifier identifier = Identifier.of("hachimiclient", "font/verdana.ttf");
         InputStream stream = MinecraftClient.getInstance().getResourceManager().getResource(identifier).get().getInputStream();
         CLIENT = new AWTFontRenderer((InputStream)(FONT_FILE_PATH.startsWith("assets") ? stream : new FileInputStream(FONT_FILE_PATH)), FONT_SIZE);
         CLIENT_UNSCALED = new AWTFontRenderer((InputStream)(FONT_FILE_PATH.startsWith("assets") ? stream : new FileInputStream(FONT_FILE_PATH)), 9.0F);
      } catch (IOException var2) {
      }
   }

   public static void closeFonts() {
      CLIENT.close();
      CLIENT_UNSCALED.close();
   }

   public static void setSize(float size) {
      FONT_SIZE = size;

      try {
         CLIENT = new AWTFontRenderer(new FileInputStream(FONT_FILE_PATH), FONT_SIZE);
      } catch (IOException var2) {
      }
   }

   public static boolean isInitialized() {
      return initialized;
   }
}
