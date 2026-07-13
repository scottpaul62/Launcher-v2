package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/**
 * Fond du menu adaptatif. On peut fournir jusqu'a 2 images :
 *  - background.png       (principale, ex. 16:9)
 *  - background_wide.png  (optionnelle, ex. ultrawide 21:9)
 * A l'affichage, on choisit AUTOMATIQUEMENT celle dont le ratio est le plus proche
 * de l'ecran, puis on remplit bord-a-bord (COVER, ratio preserve). -> parfait sur
 * ecran normal ET ultrawide (si les 2 images sont fournies).
 */
public final class HWBg {
    private static boolean tried = false;
    private static final Identifier[] ids = new Identifier[2];
    private static final int[] tws = new int[2];
    private static final int[] ths = new int[2];
    private static int count = 0;
    private HWBg() {}

    private static void loadOne(String path, String key) {
        try (InputStream in = HWBg.class.getResourceAsStream(path)) {
            if (in == null) return;
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            tex.setFilter(true, false);
            Identifier id = new Identifier("heroworld", key);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            ids[count] = id; tws[count] = img.getWidth(); ths[count] = img.getHeight(); count++;
            System.out.println("[HWBG] " + key + " -> " + img.getWidth() + "x" + img.getHeight());
        } catch (Throwable t) {
            System.out.println("[HWBG] echec " + key + " : " + t);
        }
    }

    private static void ensure() {
        if (tried) return;
        tried = true;
        loadOne("/assets/heroworld/textures/gui/background.png", "hw_bg_main");
        loadOne("/assets/heroworld/textures/gui/background_wide.png", "hw_bg_wide");
        if (count == 0) System.out.println("[HWBG] aucune image de fond trouvee");
    }

    public static boolean draw(DrawContext ctx, int w, int h) {
        ensure();
        if (count == 0) return false;
        float screen = (float) w / h;
        int best = 0; float bestDiff = Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            float diff = Math.abs(((float) tws[i] / ths[i]) - screen);
            if (diff < bestDiff) { bestDiff = diff; best = i; }
        }
        float scale = Math.max((float) w / tws[best], (float) h / ths[best]); // COVER
        int dw = Math.round(tws[best] * scale), dh = Math.round(ths[best] * scale);
        ctx.drawTexture(ids[best], (w - dw) / 2, (h - dh) / 2, dw, dh, 0f, 0f, tws[best], ths[best], tws[best], ths[best]);
        return true;
    }
}
