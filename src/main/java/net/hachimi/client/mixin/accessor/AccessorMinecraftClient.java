package net.hachimi.client.mixin.accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MinecraftClient.class})
public interface AccessorMinecraftClient {
   @Accessor("itemUseCooldown")
   void hookSetItemUseCooldown(int var1);

   @Accessor("itemUseCooldown")
   int hookGetItemUseCooldown();

   @Accessor("attackCooldown")
   void hookSetAttackCooldown(int var1);

   @Invoker("doItemUse")
   void hookDoItemUse();

   @Accessor("session")
   @Final
   @Mutable
   void setSession(Session var1);

   @Accessor("world")
   void hookSetWorld(ClientWorld var1);

   @Accessor("disconnecting")
   void hookSetDisconnecting(boolean var1);
}
