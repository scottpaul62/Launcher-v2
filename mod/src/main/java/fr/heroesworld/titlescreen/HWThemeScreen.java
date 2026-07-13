package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Sélecteur de thèmes : décor assombri + cartes avec aperçu animé + actif surligné. */
public class HWThemeScreen extends Screen {
    private final Screen parent;
    private static final int CARD_W = 150, CARD_H = 96, GAP = 16;

    public HWThemeScreen(Screen parent) { super(Text.literal("Thèmes")); this.parent = parent; }

    @Override
    protected void init() {
        this.addDrawableChild(new HWButton(this.width / 2 - 70, this.height - 46, 140, 24,
            Text.literal("Retour"), HWButton.SECONDARY, 0, b -> this.client.setScreen(parent != null ? parent : new HWTitleScreen())));
    }

    private int rowStartX() {
        int n = HWTheme.THEMES.length;
        int total = n * CARD_W + (n - 1) * GAP;
        return this.width / 2 - total / 2;
    }
    private int cardX(int i) { return rowStartX() + i * (CARD_W + GAP); }
    private int cardY() { return this.height / 2 - CARD_H / 2; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int y = cardY();
        for (int i = 0; i < HWTheme.THEMES.length; i++) {
            int x = cardX(i);
            if (mx >= x && mx <= x + CARD_W && my >= y && my <= y + CARD_H) {
                HWTheme.index = i;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        HWScene.draw(ctx, this.width, this.height);
        ctx.fill(0, 0, this.width, this.height, 0xB0000000); // assombrit le décor
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eThèmes du menu"),
            this.width / 2, cardY() - 40, 0xFFE8C56A);

        float t = (System.currentTimeMillis() % 600000L) / 1000f;
        int y = cardY();
        for (int i = 0; i < HWTheme.THEMES.length; i++) {
            HWTheme.Theme th = HWTheme.THEMES[i];
            int x = cardX(i);
            boolean active = (i == HWTheme.index);
            boolean hover = mouseX >= x && mouseX <= x + CARD_W && mouseY >= y && mouseY <= y + CARD_H;
            // aperçu (mini ciel + étoiles + lueur de brasero)
            ctx.fillGradient(x, y, x + CARD_W, y + CARD_H, th.skyTop, th.skyBottom);
            for (int s = 0; s < 18; s++) {
                int sx = x + 8 + (s * 37) % (CARD_W - 12);
                int sy = y + 8 + (s * 53) % (CARD_H - 40);
                int a = (int) (60 + 120 * (0.5f + 0.5f * Math.sin(t * 1.5 + s)));
                ctx.fill(sx, sy, sx + 1, sy + 1, (a << 24) | (th.star & 0xFFFFFF));
            }
            ctx.fillGradient(x + CARD_W / 2 - 26, y + CARD_H - 40, x + CARD_W / 2 + 26, y + CARD_H - 12,
                (0x66 << 24) | (th.glow & 0xFFFFFF), 0x00000000);
            // bordure (or si actif / survol)
            int b = active ? 0xFFE8C56A : (hover ? 0xAAE8C56A : 0x44FFFFFF);
            border(ctx, x, y, CARD_W, CARD_H, b);
            // nom + actif
            ctx.fill(x, y + CARD_H - 16, x + CARD_W, y + CARD_H, 0xCC000000);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal((active ? "§e" : "§f") + th.name),
                x + CARD_W / 2, y + CARD_H - 13, 0xFFFFFFFF);
            if (active) ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6● actif"), x + CARD_W / 2, y + 6, 0xFFE8C56A);
        }
        // description du thème courant
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7" + HWTheme.current().desc),
            this.width / 2, y + CARD_H + 16, 0xFFCFC9DA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override public boolean shouldPause() { return false; }
}
