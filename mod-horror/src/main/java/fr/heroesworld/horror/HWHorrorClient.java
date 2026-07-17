package fr.heroesworld.horror;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Point d'entrée client du mod horreur HEROES-WORLD (Failles de l'Hadès). */
public final class HWHorrorClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("HeroWorldHorror");

    @Override
    public void onInitializeClient() {
        HorrorConfig.load();
        LOG.info("[HeroesWorld-Horror] initialisé — touche lampe torche: {} | item: {} (CMD {})",
                HorrorConfig.torchKey, HorrorConfig.torchMaterial, HorrorConfig.torchCustomModelData);
    }
}
