package fr.heroesworld.titlescreen.mixin;

import fr.heroesworld.titlescreen.HWTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Remplace l'écran titre vanilla ET la liste des serveurs par l'écran
 * HEROES-WORLD, où que le jeu tente de les ouvrir (démarrage, déconnexion,
 * bouton retour...). Comparaison de classe EXACTE : aucune récursion possible,
 * et les écrans hérités d'autres mods ne sont pas touchés.
 */
@Mixin(MinecraftClient.class)
public class ScreenSwapMixin {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen heroworld$swapScreen(Screen screen) {
        if (screen != null) {
            Class<?> type = screen.getClass();
            if (type == TitleScreen.class || type == MultiplayerScreen.class) {
                return new HWTitleScreen();
            }
        }
        return screen;
    }
}
