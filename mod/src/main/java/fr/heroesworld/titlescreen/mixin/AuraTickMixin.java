package fr.heroesworld.titlescreen.mixin;

import fr.heroesworld.titlescreen.HWAura;
import fr.heroesworld.titlescreen.HWCosmetics;
import fr.heroesworld.titlescreen.HWWings;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fait apparaître l'aura équipée autour du joueur local, chaque tick (client-side). */
@Mixin(ClientPlayerEntity.class)
public class AuraTickMixin {
    private long heroworld$t = 0L;
    private boolean heroworld$prevKey = false;

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void heroworld$aura(CallbackInfo ci) {
        heroworld$t++;
        try {
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            if (self.getWorld() instanceof ClientWorld cw) {
                HWAura.spawn(cw, self, HWCosmetics.aura, heroworld$t);
                HWWings.spawn(cw, self, HWCosmetics.wings, heroworld$t);
            }
        } catch (Throwable ignored) {}
        try {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            boolean pressed = mc != null && net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(), 344); // Maj-droite
            if (pressed && !heroworld$prevKey && mc.currentScreen == null) {
                mc.setScreen(new fr.heroesworld.titlescreen.HWClientSettings(null));
            }
            heroworld$prevKey = pressed;
        } catch (Throwable ignored) {}
    }
}
