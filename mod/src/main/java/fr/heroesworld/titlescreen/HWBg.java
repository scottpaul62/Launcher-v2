package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/**
 * Fond du menu. L'image (résolution fixe) est affichée ENTIÈRE et NETTE (contain :
 * proportions exactes, aucun zoom, aucune déformation), centrée. Les bords sont comblés
 * par une version sombre de la même image (jamais de bandes noires) -> s'adapte à tout écran.
 */
public final class HWBg {
    private static final Identifier ID = new Identifier("heroworld", "hw_background_dyn");
    private static boolean tried = false, ok = false;
    private static int tw = 1, th = 1;
    private HWBg() {}

    private static void ensure() {
        if (tried) return;
        tried = true;
        try (InputStream in = HWBg.class.getResourceAsStream("/assets/heroworld/textures/gui/background.png")) {
            if (in != null) {
                NativeImage img = NativeImage.read(in);
                tw = img.getWidth();
                th = img.getHeight();
                NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
                tex.setFilter(true, false);
                MinecraftClient.getInstance().getTextureManager().registerTexture(ID, tex);
                ok = true;
            }
            System.out.println("[HWBG] background.png -> " + (ok ? (tw + "x" + th + " charge") : "ABSENT du jar"));
        } catch (Throwable t) {
            ok = false;
            System.out.println("[HWBG] echec chargement background.png : " + t);
        }
    }

    public static boolean draw(DrawContext ctx, int w, int h) {
        ensure();
        if (!ok) return false;
        // 1. Remplissage des bords : image en COVER, assombrie (jamais de bandes noires).
        float sc = Math.max((float) w / tw, (float) h / th);
        int cw = Math.round(tw * sc), ch = Math.round(th * sc);
        ctx.drawTexture(ID, (w - cw) / 2, (h - ch) / 2, cw, ch, 0f, 0f, tw, th, tw, th);
        ctx.fill(0, 0, w, h, 0x99000000);
        // 2. Image ENTIÈRE et nette (CONTAIN), centrée : proportions exactes, aucun zoom.
        float sf = Math.min((float) w / tw, (float) h / th);
        int fw = Math.round(tw * sf), fh = Math.round(th * sf);
        ctx.drawTexture(ID, (w - fw) / 2, (h - fh) / 2, fw, fh, 0f, 0f, tw, th, tw, th);
        return true;
    }
}
