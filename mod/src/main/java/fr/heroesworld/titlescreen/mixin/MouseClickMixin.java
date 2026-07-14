package fr.heroesworld.titlescreen.mixin;

import fr.heroesworld.titlescreen.HWHudManager;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Compte les clics souris pour le mod CPS (clics par seconde). */
@Mixin(Mouse.class)
public class MouseClickMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), require = 0)
    private void heroworld$click(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1 && (button == 0 || button == 1)) HWHudManager.onClick();
    }
}
