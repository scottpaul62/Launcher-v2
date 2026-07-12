package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/**
 * Charge l'image de fond du menu (assets/heroworld/textures/gui/background.png)
 * directement depuis le jar au runtime (méthode fiable, indépendante du pack de
 * ressources). Dessine en "cover" (remplit l'écran, ratio conservé).
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
                MinecraftClient.getInstance().getTextureManager().registerTexture(ID, new NativeImageBackedTexture(img));
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
        float scale = Math.max((float) w / tw, (float) h / th);
        int dw = Math.round(tw * scale), dh = Math.round(th * scale);
        int dx = (w - dw) / 2, dy = (h - dh) / 2;
        ctx.drawTexture(ID, dx, dy, dw, dh, 0f, 0f, tw, th, tw, th);
        return true;
    }
}
