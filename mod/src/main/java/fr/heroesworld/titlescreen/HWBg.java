package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fond du menu adaptatif et NET a toutes les tailles de fenetre.
 * Deux familles d'images (normale 16:9 et ultrawide), chacune declinee en plusieurs
 * resolutions pre-reduites en haute qualite (Lanczos + accentuation) au build :
 *   background.png (3840x2160) / background_2560.png / background_1920.png
 *   background_wide.png (5160x2160) / background_wide_3440.png / background_wide_2580.png
 * A l'affichage : famille choisie par ratio d'ecran, puis la PLUS PETITE variante qui couvre
 * la resolution PHYSIQUE de la fenetre -> le GPU ne reduit presque pas, l'image reste nette
 * (la reduction forte a la volee ecrasait les details type fumee/brume).
 * Chargement paresseux : seule la variante utilisee est decodee (memoire maitrisee).
 */
public final class HWBg {
    private static final String DIR = "/assets/heroworld/textures/gui/";
    // {nom, largeur, hauteur} — triees de la plus petite a la plus grande
    private static final Object[][] MAIN_DEF = {
        {"background_1920", 1920, 1080}, {"background_2560", 2560, 1440}, {"background", 3840, 2160}
    };
    private static final Object[][] WIDE_DEF = {
        {"background_wide_2580", 2580, 1080}, {"background_wide_3440", 3440, 1440}, {"background_wide", 5160, 2160}
    };

    private static final Map<String, Identifier> LOADED = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();
    private HWBg() {}

    private static Identifier texture(String name) {
        Identifier cached = LOADED.get(name);
        if (cached != null) return cached;
        if (FAILED.contains(name)) return null;
        try (InputStream in = HWBg.class.getResourceAsStream(DIR + name + ".png")) {
            if (in == null) { FAILED.add(name); return null; }
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            tex.setFilter(true, false); // lissage bilineaire
            Identifier id = new Identifier("heroworld", "hw_bg_" + name);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            LOADED.put(name, id);
            System.out.println("[HWBG] charge " + name + " (" + img.getWidth() + "x" + img.getHeight() + ")");
            return id;
        } catch (Throwable t) {
            System.out.println("[HWBG] echec " + name + " : " + t);
            FAILED.add(name);
            return null;
        }
    }

    public static boolean draw(DrawContext ctx, int w, int h) {
        float screen = (float) w / h;
        float rMain = 3840f / 2160f, rWide = 5160f / 2160f;
        Object[][] fam = Math.abs(rMain - screen) <= Math.abs(rWide - screen) ? MAIN_DEF : WIDE_DEF;

        // resolution physique reelle de la fenetre (pas les coordonnees GUI)
        int fbW = w, fbH = h;
        try {
            fbW = MinecraftClient.getInstance().getWindow().getFramebufferWidth();
            fbH = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        } catch (Throwable ignored) {}

        // plus petite variante qui couvre l'ecran ; sinon on monte ; repli : l'autre famille
        Object[] chosen = null;
        for (Object[] v : fam) {
            if (FAILED.contains((String) v[0])) continue;
            chosen = v;
            if ((Integer) v[1] >= fbW * 1.02f && (Integer) v[2] >= fbH * 1.02f) break;
        }
        Identifier id = chosen != null ? texture((String) chosen[0]) : null;
        if (id == null) {
            Object[][] other = fam == MAIN_DEF ? WIDE_DEF : MAIN_DEF;
            for (Object[] v : other) {
                if (FAILED.contains((String) v[0])) continue;
                chosen = v;
                if ((Integer) v[1] >= fbW * 1.02f && (Integer) v[2] >= fbH * 1.02f) break;
            }
            id = chosen != null ? texture((String) chosen[0]) : null;
        }
        if (id == null) return false;

        int tw = (Integer) chosen[1], th = (Integer) chosen[2];
        ctx.fill(0, 0, w, h, 0xFF0A0E1A); // matte sobre : jamais de bord gris, quelle que soit la resolution
        float scale = Math.max((float) w / tw, (float) h / th) * 1.04f; // COVER + tres leger recadrage
        int dw = Math.round(tw * scale), dh = Math.round(th * scale);
        ctx.drawTexture(id, (w - dw) / 2, (h - dh) / 2, dw, dh, 0f, 0f, tw, th, tw, th);
        return true;
    }
}
