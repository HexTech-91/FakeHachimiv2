package net.hachimi.client.util.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.hachimi.client.Hachimi;
import net.hachimi.client.util.Globals;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public final class TextureDownloader implements Globals {
   private final CloseableHttpClient client = HttpClients.createDefault();
   private final Map<String, Identifier> cache = new ConcurrentHashMap<>();
   private final Set<String> downloading = new HashSet<>();

   public void downloadTexture(String id, String url, boolean force) {
      if (this.downloading.add(id) && !this.cache.containsKey(id)) {
         Hachimi.EXECUTOR.execute(() -> {
            HttpGet request = new HttpGet(url);

            try {
               CloseableHttpResponse response = this.client.execute(request);

               try {
                  InputStream stream = response.getEntity().getContent();
                  NativeImage image = NativeImage.read(stream);
                  Identifier textureIdentifier = mc.getTextureManager().registerDynamicTexture(id, new NativeImageBackedTexture(image));
                  this.cache.put(id, textureIdentifier);
               } catch (Throwable var10) {
                  if (response != null) {
                     try {
                        response.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (response != null) {
                  response.close();
               }
            } catch (IOException var11) {
               var11.printStackTrace();
               if (force) {
                  this.downloading.remove(id);
               }
            }
         });
      }
   }

   public void removeTexture(String id) {
      Identifier identifier = this.cache.get(id);
      if (identifier != null) {
         mc.getTextureManager().destroyTexture(identifier);
         this.cache.remove(id);
      }
   }

   public Identifier get(String id) {
      return this.cache.get(id);
   }

   public boolean exists(String id) {
      return this.cache.containsKey(id);
   }

   public boolean isDownloading(String id) {
      return this.downloading.contains(id);
   }
}
