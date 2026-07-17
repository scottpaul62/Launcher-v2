package fr.heroesworld.horror.mixin;

import fr.heroesworld.horror.TorchHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dessine l'indicateur lampe torche par-dessus l'ATH vanilla (même hook que le HUD heroworld-titlescreen). */
@Mixin(InGameHud.class)
public class TorchHudMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void heroworld$torchHud(DrawContext context, float tickDelta, CallbackInfo ci) {
        TorchHud.render(context);
    }
}
