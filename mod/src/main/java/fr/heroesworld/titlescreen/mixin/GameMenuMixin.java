package fr.heroesworld.titlescreen.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Habillage du menu Échap : marque HEROES-WORLD en or au sommet.
 * (Injection en fin de rendu : zéro risque pour la logique vanilla.)
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuMixin extends Screen {

    protected GameMenuMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void heroworld$brand(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("⚡ HEROES-WORLD ⚡"), this.width / 2, 8, 0xFFD700);
    }
}
