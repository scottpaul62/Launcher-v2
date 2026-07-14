package fr.heroesworld.titlescreen.mixin;

import fr.heroesworld.titlescreen.HWHudManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Masque le crosshair vanilla quand le crosshair personnalise HERO WORLD est actif. */
@Mixin(InGameHud.class)
public class CrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void heroworld$hideVanilla(DrawContext context, float tickDelta, CallbackInfo ci) {
        try {
            HWHudManager.El e = HWHudManager.byId("crosshair");
            if (e != null && e.enabled) ci.cancel();
        } catch (Throwable ignored) {}
    }
}
