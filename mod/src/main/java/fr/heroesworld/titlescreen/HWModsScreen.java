package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Menu central du client HERO WORLD (facon Lunar) : mods HUD en cartes, onglets, editeur de HUD. */
public class HWModsScreen extends Screen {
    private final Screen parent;
    private static String activeCat = "Tous";
    private static final String[] CATS = {"Tous", "Performance", "Info", "Mecanique", "Combat"};

    public HWModsScreen(Screen parent) { super(Text.literal("Mods HERO WORLD")); this.parent = parent; }

    private void back() {
        if (parent != null) this.client.setScreen(parent);
        else if (this.client.world != null) this.client.setScreen(null);
        else this.client.setScreen(new HWTitleScreen());
    }
    private void reopen() { this.client.setScreen(new HWModsScreen(parent)); }

    private int pw, ph, px, py;

    @Override
    protected void init() {
        pw = Math.min(this.width - 40, 640);
        ph = Math.min(this.height - 40, 430);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;

        // Onglets de categories
        int tabW = Math.min(112, (pw - 24) / CATS.length);
        int tx = px + 12;
        int tyTab = py + 40;
        for (String c : CATS) {
            final String cat = c;
            int style = c.equals(activeCat) ? HWButton.PRIMARY : HWButton.SECONDARY;
            this.addDrawableChild(new HWButton(tx, tyTab, tabW - 4, 18, Text.literal(c), style, 0,
                b -> { activeCat = cat; reopen(); }));
            tx += tabW;
        }

        // Cartes de mods (2 colonnes)
        List<HWHudManager.El> vis = new ArrayList<>();
        for (HWHudManager.El e : HWHudManager.ELEMENTS)
            if (activeCat.equals("Tous") || e.category.equals(activeCat)) vis.add(e);

        int gap = 8, cols = 2;
        int gridTop = py + 68;
        int gridBottom = py + ph - 40;
        int cardW = (pw - 24 - gap) / cols;
        int rows = Math.max(1, (vis.size() + cols - 1) / cols);
        int cardH = Math.max(18, Math.min(28, (gridBottom - gridTop - (rows - 1) * gap) / rows));
        for (int i = 0; i < vis.size(); i++) {
            final HWHudManager.El e = vis.get(i);
            int col = i % cols, row = i / cols;
            int cxp = px + 12 + col * (cardW + gap);
            int cyp = gridTop + row * (cardH + gap);
            if (cyp + cardH > gridBottom) break;
            String lbl = "§f" + e.name + "   " + (e.enabled ? "§aON" : "§8OFF");
            this.addDrawableChild(new HWButton(cxp, cyp, cardW - 26, cardH, Text.literal(lbl), HWButton.SECONDARY, iconKind(e.id),
                b -> { e.enabled = !e.enabled; HWHudManager.save(); reopen(); }));
            this.addDrawableChild(new HWButton(cxp + cardW - 24, cyp, 24, cardH, Text.literal(""), HWButton.SECONDARY, 5,
                b -> this.client.setScreen(new HWModSettingsScreen(this, e))));
        }

        // Barre du bas
        int by = py + ph - 30;
        int n = 5;
        int bw = (pw - 24 - (n - 1) * 6) / n;
        int bx = px + 12;
        this.addDrawableChild(new HWButton(bx, by, bw, 22, Text.literal("Editer le HUD"), HWButton.PRIMARY, 0,
            b -> this.client.setScreen(new HWHudEditScreen(this)))); bx += bw + 6;
        this.addDrawableChild(new HWButton(bx, by, bw, 22, Text.literal("Cosmetiques"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWCosmeticsScreen(this)))); bx += bw + 6;
        this.addDrawableChild(new HWButton(bx, by, bw, 22, Text.literal("Theme"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWThemeScreen(this)))); bx += bw + 6;
        this.addDrawableChild(new HWButton(bx, by, bw, 22, Text.literal("Options MC"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options)))); bx += bw + 6;
        this.addDrawableChild(new HWButton(bx, by, bw, 22, Text.literal("Fermer"), HWButton.SECONDARY, 0, b -> back()));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) super.renderBackground(ctx, mouseX, mouseY, delta);
        else { HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0xB0000000); }

        ctx.fill(px, py, px + pw, py + ph, 0xE60E0C14);
        drawBorder(ctx, px, py, pw, ph, 0xFFE8C56A);
        ctx.fill(px, py + 30, px + pw, py + 31, 0x66E8C56A);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§6HERO WORLD §7Client"), px + 12, py + 11, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§8v1.1.6"), px + pw - 44, py + 11, 0xFF888888);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private static int iconKind(String id) {
        switch (id) {
            case "fps": return 6; case "coords": return 7; case "direction": return 8; case "time": return 9;
            case "day": return 10; case "session": return 11; case "keystrokes": return 12; case "armor": return 13;
            case "ping": return 14; case "effects": return 15; case "cps": return 16;
            default: return 0;
        }
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x, y, x + w, y + 1, col);
        ctx.fill(x, y + h - 1, x + w, y + h, col);
        ctx.fill(x, y, x + 1, y + h, col);
        ctx.fill(x + w - 1, y, x + w, y + h, col);
    }

    @Override public void close() { back(); }
    @Override public boolean shouldPause() { return false; }
}
