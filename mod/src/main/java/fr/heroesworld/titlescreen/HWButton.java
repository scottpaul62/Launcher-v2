package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Bouton HEROES-WORLD : panneau sombre translucide, coins adoucis, liseré or,
 * survol lumineux. Rien de la texture grise vanilla. Style « Lunar » maison.
 */
public class HWButton extends ButtonWidget {

    private final boolean primary;

    public HWButton(int x, int y, int w, int h, Text msg, boolean primary, PressAction onPress) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.primary = primary;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
        boolean hov = this.isHovered() || this.isFocused();

        int fill = primary
            ? (hov ? 0xFF3A2E14 : 0xFF241C0C)
            : (hov ? 0xE0221D2C : 0xC016131E);
        // ombre portée
        rrect(ctx, x, y + 2, w, h, 0x40000000);
        // corps (coins adoucis 1px)
        rrect(ctx, x, y, w, h, fill);
        // liseré or à gauche (accent)
        ctx.fill(x + 1, y + 2, x + 4, y + h - 2, primary ? 0xFFE8C56A : (hov ? 0xCCE8C56A : 0x66E8C56A));
        // bordure
        int border = hov ? 0xFFE8C56A : (primary ? 0xAAE8C56A : 0x33FFFFFF);
        borderR(ctx, x, y, w, h, border);
        // léger reflet haut
        ctx.fill(x + 2, y + 1, x + w - 2, y + 2, 0x18FFFFFF);

        int col = primary ? 0xFFFFEFC6 : (hov ? 0xFFFFFFFF : 0xFFCFC9DA);
        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawCenteredTextWithShadow(mc.textRenderer, this.getMessage(), x + w / 2, y + (h - 8) / 2, col);
    }

    // rectangle à coins adoucis (chanfrein 1px)
    private static void rrect(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x + 1, y, x + w - 1, y + h, c);
        ctx.fill(x, y + 1, x + w, y + h - 1, c);
    }
    private static void borderR(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x + 1, y, x + w - 1, y + 1, c);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, c);
        ctx.fill(x, y + 1, x + 1, y + h - 1, c);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }
}
