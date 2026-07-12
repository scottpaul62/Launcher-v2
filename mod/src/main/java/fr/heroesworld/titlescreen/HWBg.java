package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/** Charge l'image de fond (background.png) au runtime et l'ajuste plein écran. */
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
                tex.setFilter(true, false); // bilinéaire = lissage propre à l'agrandissement
                MinecraftClient.getInstance().getTextureManager().registerTexture(ID, tex);
                ok = true;
            }
            System.out.println("[HWBG] background.png -> " + (ok ? (tw + "x" + th + " charge") : "ABSENT du jar"));
        } catch (Throwable t) {
            ok = false;
            System.out.println("[HWBG] echec chargement background.png : " + t);
        }
    }

    /** Ajusté au plein écran (étiré pour remplir, sans recadrage). */
    public static boolean draw(DrawContext ctx, int w, int h) {
        ensure();
        if (!ok) return false;
        ctx.drawTexture(ID, 0, 0, w, h, 0f, 0f, tw, th, tw, th);
        return true;
    }
}
