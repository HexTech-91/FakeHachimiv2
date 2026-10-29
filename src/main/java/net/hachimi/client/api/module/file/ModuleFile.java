package net.hachimi.client.api.module.file;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.hachimi.client.Hachimi;
import net.hachimi.client.api.file.ConfigFile;
import net.hachimi.client.api.module.Module;

public class ModuleFile extends ConfigFile {
   private final Module module;

   public ModuleFile(Path dir, Module module) {
      super(dir, module.getId());
      this.module = module;
   }

   @Override
   public void save() {
      try {
         Path filepath = this.getFilepath();
         if (!Files.exists(filepath)) {
            Files.createFile(filepath);
         }

         JsonObject json = this.module.toJson();
         this.write(filepath, this.serialize(json));
      } catch (IOException var3) {
         Hachimi.error("Could not save file for {}!", this.module.getName());
         var3.printStackTrace();
      }
   }

   @Override
   public void load() {
      try {
         Path filepath = this.getFilepath();
         if (Files.exists(filepath)) {
            String content = this.read(filepath);
            this.module.fromJson(this.parseObject(content));
         }
      } catch (IOException var3) {
         Hachimi.error("Could not read file for {}!", this.module.getName());
         var3.printStackTrace();
      }
   }
}
