package fr.heroesworld.titlescreen.mixin;

import fr.heroesworld.titlescreen.HWHudManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scoreboard deplacable : enveloppe le rendu vanilla dans une translation/echelle HERO WORLD. */
@Mixin(InGameHud.class)
public class ScoreboardMixin {
    @Unique private boolean heroworld$pushed = false;

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), require = 0)
    private void heroworld$pre(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        try {
            HWHudManager.El e = HWHudManager.byId("scoreboard");
            if (e == null || !e.enabled) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
            int w = HWHudManager.scaledW(mc, e), h = HWHudManager.scaledH(e);
            int ax = HWHudManager.actualX(e, sw, w), ay = HWHudManager.actualY(e, sh, h);
            // le sidebar vanilla est colle au bord DROIT et centre verticalement :
            // on fait correspondre son coin droit/centre au bord droit/centre du cadre HERO WORLD.
            context.getMatrices().push();
            context.getMatrices().translate(ax + w, ay + h / 2f, 0);
            context.getMatrices().scale(e.scale, e.scale, 1f);
            context.getMatrices().translate(-(sw - 1f), -(sh / 2f), 0);
            heroworld$pushed = true;
        } catch (Throwable ignored) {}
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("RETURN"), require = 0)
    private void heroworld$post(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        try {
            if (heroworld$pushed) { context.getMatrices().pop(); heroworld$pushed = false; }
        } catch (Throwable ignored) {}
    }
}
