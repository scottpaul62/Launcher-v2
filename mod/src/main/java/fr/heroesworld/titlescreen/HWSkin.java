package fr.heroesworld.titlescreen;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

/** Skin reelle du compte connecte (texture + modele slim/classic), resolue une fois en arriere-plan. */
public final class HWSkin {
    private static volatile Identifier texture = null;
    private static volatile boolean slim = false;
    private static volatile boolean requested = false;
    private HWSkin() {}

    public static Identifier texture(MinecraftClient mc) {
        fetch(mc);
        if (texture != null) return texture;
        try { return mc.getSkinProvider().getSkinTextures(mc.getGameProfile()).texture(); }
        catch (Throwable t) { return null; }
    }
    public static boolean slim(MinecraftClient mc) {
        fetch(mc);
        if (texture != null) return slim;
        try { return mc.getSkinProvider().getSkinTextures(mc.getGameProfile()).model() == SkinTextures.Model.SLIM; }
        catch (Throwable t) { return false; }
    }

    private static void fetch(MinecraftClient mc) {
        if (requested || mc == null) return;
        requested = true;
        // 1) chemin direct : fetchSkinTextures (async)
        try {
            mc.getSkinProvider().fetchSkinTextures(mc.getGameProfile()).thenAccept(o -> apply(o));
        } catch (Throwable ignored) {}
        // 2) chemin robuste : profil complet via le service de session Mojang (textures signees)
        CompletableFuture.runAsync(() -> {
            try {
                GameProfile base = mc.getGameProfile();
                if (base == null || base.getId() == null) return;
                com.mojang.authlib.yggdrasil.ProfileResult r = mc.getSessionService().fetchProfile(base.getId(), false);
                if (r == null) return;
                GameProfile full = r.profile();
                mc.execute(() -> {
                    try {
                        SkinTextures st = mc.getSkinProvider().getSkinTextures(full);
                        if (st != null && st.texture() != null) { texture = st.texture(); slim = st.model() == SkinTextures.Model.SLIM; }
                        mc.getSkinProvider().fetchSkinTextures(full).thenAccept(o -> apply(o));
                    } catch (Throwable ignored) {}
                });
            } catch (Throwable ignored) {}
        });
    }

    private static void apply(Object o) {
        try {
            Object v = o;
            if (v instanceof java.util.Optional<?> opt) v = opt.orElse(null);
            if (v instanceof SkinTextures st && st.texture() != null) {
                texture = st.texture();
                slim = st.model() == SkinTextures.Model.SLIM;
            }
        } catch (Throwable ignored) {}
    }
}
