package fr.heroesworld.titlescreen.mixin;

import fr.heroesworld.titlescreen.HWZoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applique le zoom HERO WORLD en modifiant le FOV calcule (sans toucher aux options du joueur). */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void heroworld$zoomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        try {
            if (HWZoom.zooming()) cir.setReturnValue(HWZoom.applyFov(cir.getReturnValue()));
        } catch (Throwable ignored) {}
    }
}
