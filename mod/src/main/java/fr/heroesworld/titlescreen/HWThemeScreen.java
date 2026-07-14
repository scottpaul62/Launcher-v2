package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Selecteur de themes facon Lunar : panneau propre, grille de cartes a apercu, "Bientot" sur les verrouilles. */
public class HWThemeScreen extends Screen {
    private final Screen parent;
    private int pw, ph, px, py, cardW, cardH, gridTop, gap = 14, cols = 3;

    public HWThemeScreen(Screen parent) { super(Text.literal("Themes")); this.parent = parent; }

    private void back() { this.client.setScreen(parent != null ? parent : new HWTitleScreen()); }

    @Override
    protected void init() {
        pw = Math.min(this.width - 40, 580);
        ph = Math.min(this.height - 40, 410);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        cardW = (pw - 24 - (cols - 1) * gap) / cols;
        cardH = (int) (cardW * 0.58f);
        gridTop = py + 46;
        this.addDrawableChild(new HWButton(px + 12, py + ph - 30, 150, 22, Text.literal("Retour"), HWButton.SECONDARY, 0, b -> back()));
        this.addDrawableChild(new HWButton(px + pw - 162, py + ph - 30, 150, 22, Text.literal("Reinitialiser"), HWButton.SECONDARY, 0,
            b -> { HWTheme.index = 0; }));
    }

    private int cardX(int i) { return px + 12 + (i % cols) * (cardW + gap); }
    private int cardY(int i) { return gridTop + (i / cols) * (cardH + 20 + gap); }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < HWTheme.THEMES.length; i++) {
            if (!HWTheme.THEMES[i].available) continue;
            int x = cardX(i), y = cardY(i);
            if (mx >= x && mx <= x + cardW && my >= y && my <= y + cardH) { HWTheme.index = i; return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) super.renderBackground(ctx, mouseX, mouseY, delta);
        else { HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0xB0000000); }

        ctx.fill(px, py, px + pw, py + ph, 0xE60E0C14);
        border(ctx, px, py, pw, ph, 0xFFE8C56A);
        ctx.fill(px, py + 34, px + pw, py + 35, 0x66E8C56A);
        HWLogo.draw(ctx, px + 22, py + 17, 24);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§eSelectionner un theme"), px + 40, py + 13, 0xFFFFFFFF);

        float t = (System.currentTimeMillis() % 600000L) / 1000f;
        for (int i = 0; i < HWTheme.THEMES.length; i++) {
            HWTheme.Theme th = HWTheme.THEMES[i];
            int x = cardX(i), y = cardY(i);
            boolean active = (i == HWTheme.index);
            boolean hover = mouseX >= x && mouseX <= x + cardW && mouseY >= y && mouseY <= y + cardH;
            // apercu : ciel + etoiles + lueur
            ctx.fillGradient(x, y, x + cardW, y + cardH, th.skyTop, th.skyBottom);
            for (int s = 0; s < 16; s++) {
                int sx = x + 6 + (s * 37) % (cardW - 10);
                int sy = y + 6 + (s * 53) % (cardH - 22);
                int a = (int) (60 + 120 * (0.5f + 0.5f * Math.sin(t * 1.5 + s)));
                ctx.fill(sx, sy, sx + 1, sy + 1, (a << 24) | (th.star & 0xFFFFFF));
            }
            ctx.fillGradient(x + cardW / 2 - 22, y + cardH - 26, x + cardW / 2 + 22, y + cardH - 4,
                (0x66 << 24) | (th.glow & 0xFFFFFF), 0x00000000);
            if (!th.available) {
                ctx.fill(x, y, x + cardW, y + cardH, 0xB0000000);
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Bientot"), x + cardW / 2, y + cardH / 2 - 4, 0xFFCFC9DA);
            } else if (active) {
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6●"), x + cardW - 8, y + 6, 0xFFE8C56A);
            }
            int b = active ? 0xFFE8C56A : (hover && th.available ? 0xAAE8C56A : 0x44FFFFFF);
            border(ctx, x, y, cardW, cardH, b);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal((active ? "§e" : "§f") + th.name),
                x + cardW / 2, y + cardH + 5, 0xFFFFFFFF);
        }
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Actif : §f" + HWTheme.current().name), px + 12, py + ph - 46, 0xFFCFC9DA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override public void close() { back(); }
    @Override public boolean shouldPause() { return false; }
}
