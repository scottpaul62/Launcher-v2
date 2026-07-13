package fr.heroesworld.titlescreen.mixin;

import fr.heroesworld.titlescreen.HWHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dessine le HUD HERO WORLD par-dessus l'ATH vanilla. */
@Mixin(InGameHud.class)
public class HudMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void heroworld$hud(DrawContext context, float tickDelta, CallbackInfo ci) {
        HWHud.render(context, MinecraftClient.getInstance());
    }
}
