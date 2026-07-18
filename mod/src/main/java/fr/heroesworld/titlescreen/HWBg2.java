package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/**
 * Fond « Cosmic Olympus » (kit adaptatif) pour les ecrans du centre (mods, reglages, vestiaire...).
 * Variante choisie selon la resolution PHYSIQUE, rendu cover, chargement paresseux.
 */
public final class HWBg2 {
    private static final Object[][] DEF = { {"bg2_1920", 1920, 1080}, {"bg2_2560", 2560, 1440} };
    private static final Identifier[] IDS = new Identifier[DEF.length];
    private static final boolean[] FAILED = new boolean[DEF.length];
    private HWBg2() {}

    private static Identifier tex(int i) {
        if (FAILED[i]) return null;
        if (IDS[i] != null) return IDS[i];
        try (InputStream in = HWBg2.class.getResourceAsStream("/assets/heroworld/textures/gui/" + DEF[i][0] + ".png")) {
            if (in == null) { FAILED[i] = true; return null; }
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture t = new NativeImageBackedTexture(img);
            t.setFilter(true, false);
            Identifier id = new Identifier("heroworld", "hw_" + DEF[i][0]);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, t);
            IDS[i] = id;
            return id;
        } catch (Throwable t) { FAILED[i] = true; return null; }
    }

    public static boolean draw(DrawContext ctx, int w, int h) {
        int fbW = w;
        try { fbW = MinecraftClient.getInstance().getWindow().getFramebufferWidth(); } catch (Throwable ignored) {}
        int pick = fbW > (Integer) DEF[0][1] ? 1 : 0;
        Identifier id = tex(pick);
        if (id == null && (id = tex(1 - pick)) == null) return false;
        int i = IDS[0] == id ? 0 : 1;
        int tw = (Integer) DEF[i][1], th = (Integer) DEF[i][2];
        float scale = Math.max((float) w / tw, (float) h / th) * 1.02f;
        int dw = Math.round(tw * scale), dh = Math.round(th * scale);
        ctx.fill(0, 0, w, h, 0xFF020712);
        ctx.drawTexture(id, (w - dw) / 2, (h - dh) / 2, dw, dh, 0f, 0f, tw, th, tw, th);
        return true;
    }
}
