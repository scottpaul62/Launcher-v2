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
    private static final Object[][] DEF = {
        {"bg2_1280", 1280, 720}, {"bg2_1600", 1600, 900}, {"bg2_1920", 1920, 1080},
        {"bg2_2560", 2560, 1440}, {"bg2_3840", 3840, 2160}
    };
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
        int fbW = w, fbH = h;
        try {
            fbW = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
            fbH = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        } catch (Throwable ignored) {}
        // plus PETITE variante couvrant la fenetre -> le GPU n'agrandit jamais, nettete constante
        int i = DEF.length - 1;
        for (int k = 0; k < DEF.length; k++) {
            if ((Integer) DEF[k][1] >= fbW && (Integer) DEF[k][2] >= fbH) { i = k; break; }
        }
        Identifier id = tex(i);
        for (int k = 0; id == null && k < DEF.length; k++) { i = k; id = tex(k); }
        if (id == null) return false;
        int tw = (Integer) DEF[i][1], th = (Integer) DEF[i][2];
        float scale = Math.max((float) w / tw, (float) h / th); // cover exact, aucun zoom ajoute
        int dw = Math.round(tw * scale), dh = Math.round(th * scale);
        ctx.fill(0, 0, w, h, 0xFF020712);
        ctx.drawTexture(id, (w - dw) / 2, (h - dh) / 2, dw, dh, 0f, 0f, tw, th, tw, th);
        return true;
    }
}
