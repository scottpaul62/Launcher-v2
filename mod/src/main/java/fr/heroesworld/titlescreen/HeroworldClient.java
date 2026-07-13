package fr.heroesworld.titlescreen;

import net.fabricmc.api.ClientModInitializer;

/**
 * Point d'entrée client du mod HEROES-WORLD.
 * Charge la configuration (adresse du serveur) au démarrage du jeu.
 */
public class HeroworldClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HWConfig.load();
        HWCosmetics.load();
    }
}
