package fr.heroesworld.horror;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

/** Indicateur lampe torche en bas à gauche : touche + état, fond translucide (discret, esprit jeu d'horreur). */
public final class TorchHud {
    private static final int BG = 0x59000000;      // noir ~35%
    private static final int KEY_BG = 0x66FFFFFF;  // chip touche, blanc translucide
    private static final int TXT_ON = 0xFFFFD770;  // ambre (allumée)
    private static final int TXT_OFF = 0xFFB8B8B8; // gris (éteinte)
    private static final int DOT_ON = 0xFFFFC64D;
    private static final int DOT_OFF = 0xFF555555;

    private static int cachedKey = Integer.MIN_VALUE;
    private static String keyLabel = "R";

    private TorchHud() {}

    public static void render(DrawContext ctx) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null || mc.options == null || mc.options.hudHidden || !HorrorConfig.hud) return;
            if (!Torch.holdingTorch(mc.player)) return;

            if (cachedKey != HorrorConfig.torchKey) { // pas de lookup GLFW à chaque frame
                cachedKey = HorrorConfig.torchKey;
                try {
                    keyLabel = InputUtil.Type.KEYSYM.createFromCode(cachedKey).getLocalizedText().getString().toUpperCase();
                } catch (Throwable t) { keyLabel = "?"; }
            }

            TextRenderer tr = mc.textRenderer;
            String label = "Lampe torche";
            int kw = tr.getWidth(keyLabel);
            int lw = tr.getWidth(label);

            int h = mc.getWindow().getScaledHeight();
            int x = 8, y = h - 24, pad = 4, boxH = 16;
            int keyW = kw + 8;
            int w = pad + keyW + 5 + lw + 6 + 5 + pad; // chip + texte + pastille

            ctx.fill(x, y, x + w, y + boxH, BG);
            // chip de la touche
            int kx = x + pad, ky = y + 3;
            ctx.fill(kx, ky, kx + keyW, ky + 10, KEY_BG);
            ctx.drawText(tr, keyLabel, kx + 4, ky + 1, 0xFF1A1A1A, false);
            // libellé
            ctx.drawText(tr, label, kx + keyW + 5, y + 4, Torch.on ? TXT_ON : TXT_OFF, true);
            // pastille d'état
            int dx = kx + keyW + 5 + lw + 6, dy = y + 6;
            ctx.fill(dx, dy, dx + 4, dy + 4, Torch.on ? DOT_ON : DOT_OFF);
        } catch (Throwable ignored) {}
    }
}
