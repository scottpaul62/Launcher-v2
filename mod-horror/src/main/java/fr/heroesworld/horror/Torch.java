package fr.heroesworld.horror;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;

/** État et logique client de la lampe torche. La vérité côté monde (blocs lumière) vit dans le plugin HeroesHorror. */
public final class Torch {
    /** Lampe allumée (état local, optimiste — le serveur suit via /hwtorch). */
    public static boolean on = false;

    private Torch() {}

    public static boolean holdingTorch(ClientPlayerEntity p) {
        return isTorch(p.getMainHandStack()) || isTorch(p.getOffHandStack());
    }

    /** Détecte l'item ItemsAdder heroesworld:lampe_torche : matériau + custom model data. */
    public static boolean isTorch(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        try {
            if (!Registries.ITEM.getId(s.getItem()).toString().equals(HorrorConfig.torchMaterial)) return false;
            CustomModelDataComponent cmd = s.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            return cmd != null && cmd.value() == HorrorConfig.torchCustomModelData;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Bascule l'état, prévient le serveur, clic sonore local. */
    public static void toggle(MinecraftClient mc) {
        on = !on;
        try {
            if (mc.player != null) mc.player.networkHandler.sendChatCommand(on ? "hwtorch on" : "hwtorch off");
        } catch (Throwable t) {
            HWHorrorClient.LOG.warn("[HeroesWorld-Horror] envoi /hwtorch impossible", t);
        }
        try {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_LEVER_CLICK, on ? 1.7f : 1.2f, 0.5f));
        } catch (Throwable ignored) {}
    }

    /** Extinction locale silencieuse (lampe lâchée, déconnexion...) — le serveur s'éteint de son côté. */
    public static void forceOff() {
        on = false;
    }
}
