package net.hachimi.client.mixin.network;

import com.mojang.authlib.GameProfile;
import net.hachimi.client.impl.event.network.CapesEvent;
import net.hachimi.client.impl.event.network.LoadCapeEvent;
import net.hachimi.client.util.Globals;
import net.hachimi.eventbus.EventBus;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListEntry.class})
public class MixinPlayerListEntry implements Globals {
   @Shadow
   @Final
   private GameProfile profile;
   @Unique
   private Identifier capeTexture;
   @Unique
   private boolean capeTextureLoaded;

   @Inject(
      method = {"<init>(Lcom/mojang/authlib/GameProfile;Z)V"},
      at = {@At("TAIL")}
   )
   private void hookInit(GameProfile profile, boolean secureChatEnforced, CallbackInfo ci) {
      if (!this.capeTextureLoaded) {
         LoadCapeEvent loadCapeEvent = new LoadCapeEvent(profile, identifier -> this.capeTexture = identifier);
         EventBus.INSTANCE.dispatch(loadCapeEvent);
         this.capeTextureLoaded = true;
      }
   }

   @Inject(
      method = {"getSkinTextures"},
      at = {@At("TAIL")},
      cancellable = true
   )
   private void hookGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
      CapesEvent capesEvent = new CapesEvent(this.profile);
      EventBus.INSTANCE.dispatch(capesEvent);
      if (capesEvent.isCanceled()) {
         Identifier identifier = capesEvent.getShowOptifine() ? this.capeTexture : null;
         if (capesEvent.getTexture() != null) {
            identifier = capesEvent.getTexture();
         }

         if (identifier != null) {
            SkinTextures t = cir.getReturnValue();
            SkinTextures customCapeTexture = new SkinTextures(t.texture(), t.textureUrl(), identifier, identifier, t.model(), t.secure());
            cir.setReturnValue(customCapeTexture);
         }
      }
   }
}
