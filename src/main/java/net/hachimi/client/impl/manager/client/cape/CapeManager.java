package net.hachimi.client.impl.manager.client.cape;

import com.mojang.authlib.GameProfile;
import java.io.InputStream;
import java.net.URL;
import net.hachimi.client.impl.event.network.LoadCapeEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.hachimi.eventbus.annotation.EventListener;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class CapeManager implements Globals {
   public CapeManager() {
      EventBus.INSTANCE.subscribe(this);
   }

   @EventListener
   public void onLoadCape(LoadCapeEvent event) {
      this.loadPlayerCape(event.getGameProfile(), event.getTexture());
   }

   public void loadPlayerCape(GameProfile profile, CapeManager.CapeTexture texture) {
      Util.getMainWorkerExecutor().execute(() -> {
         String uuid = profile.getId().toString();
         String url = String.format("http://s.optifine.net/capes/%s.png", profile.getName());

         try {
            URL optifineUrl = new URL(url);
            InputStream stream = optifineUrl.openStream();
            NativeImage cape = NativeImage.read(stream);
            NativeImage nativeImage = this.imageFromStream(cape);
            NativeImageBackedTexture t = new NativeImageBackedTexture(nativeImage);
            Identifier identifier = mc.getTextureManager().registerDynamicTexture("of-capes-" + uuid, t);
            texture.callback(identifier);
            stream.close();
         } catch (Exception var11) {
         }
      });
   }

   private NativeImage imageFromStream(NativeImage image) {
      int imageWidth = 64;
      int imageHeight = 32;
      int imageSrcWidth = image.getWidth();
      int srcHeight = image.getHeight();

      for (int imageSrcHeight = image.getHeight(); imageWidth < imageSrcWidth || imageHeight < imageSrcHeight; imageHeight *= 2) {
         imageWidth *= 2;
      }

      NativeImage img = new NativeImage(imageWidth, imageHeight, true);

      for (int x = 0; x < imageSrcWidth; x++) {
         for (int y = 0; y < srcHeight; y++) {
            img.setColor(x, y, image.getColor(x, y));
         }
      }

      image.close();
      return img;
   }

   public interface CapeTexture {
      void callback(Identifier var1);
   }
}
