package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Centre Heroes-World : onglets Mods / Reglages / Waypoints, colonne profils, categories, grille de widgets. */
public class HWModsScreen extends Screen {
    private final Screen parent;
    // DESIGN_TOKENS
    private static final int SURF = 0xF2111318, RAISED = 0xFF181B22, BORDER = 0xFF323844,
            FOCUS = 0xFF49BDF2, GOLD = 0xFFD6B85B, TXT = 0xFFF4F5F7, TXT2 = 0xFFA9AFBA, OVERLAY = 0xB8080A0D;

    private static int tab = 0;                 // 0 Mods, 1 Reglages, 2 Waypoints
    private static String cat = "Tous";
    private static final String[] TABS = {"MODS", "REGLAGES", "WAYPOINTS"};
    private static final String[] CATS = {"Tous", "Performance", "Info", "Mecanique", "Combat"};

    private int pw, ph, px, py, mainX, mainW;
    private boolean showProfiles;

    public HWModsScreen(Screen parent) { super(Text.literal("Centre Heroes-World")); this.parent = parent; }

    private void back() {
        if (parent != null) this.client.setScreen(parent);
        else if (this.client.world != null) this.client.setScreen(null);
        else this.client.setScreen(new HWTitleScreen());
    }
    private void reopen() { this.client.setScreen(new HWModsScreen(parent)); }

    @Override
    protected void init() {
        pw = Math.min(this.width - 60, 1000);
        ph = Math.min(this.height - 60, 600);
        px = (this.width - pw) / 2; py = (this.height - ph) / 2;
        showProfiles = pw >= 560;
        mainX = showProfiles ? px + 158 : px + 14;
        mainW = px + pw - 14 - mainX;

        // onglets
        int tw = 96, tg = 6, tx = px + pw / 2 - (TABS.length * tw + (TABS.length - 1) * tg) / 2, ty = py + 8;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            this.addDrawableChild(new HWButton(tx + i * (tw + tg), ty, tw, 20, Text.literal(TABS[i]),
                i == tab ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { tab = t; reopen(); }));
        }
        // fermer
        this.addDrawableChild(new HWButton(px + pw - 30, py + 8, 20, 20, Text.literal("X"), HWButton.SECONDARY, 0, b -> back()));

        // colonne profils
        if (showProfiles) {
            int y = py + 62;
            for (String name : HWProfiles.names) {
                final String n = name;
                boolean act = n.equals(HWProfiles.active);
                this.addDrawableChild(new HWButton(px + 12, y, 138, 22, Text.literal((act ? "§f" : "§7") + n),
                    act ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { HWProfiles.switchTo(n); reopen(); }));
                y += 26;
            }
            this.addDrawableChild(new HWButton(px + 12, y + 2, 138, 20, Text.literal("+ Nouveau profil"), HWButton.SECONDARY, 0,
                b -> { HWProfiles.create("Profil " + (HWProfiles.names.size() + 1)); reopen(); }));
        }

        if (tab == 0) initMods();
        else if (tab == 1) initSettings();
        // tab 2 : waypoints -> etat vide dessine dans render()

        // fermer (bas)
        this.addDrawableChild(new HWButton(px + pw - 110, py + ph - 30, 96, 22, Text.literal("Fermer"), HWButton.SECONDARY, 0, b -> back()));
    }

    private void initMods() {
        // categories
        int cx = mainX, cy = py + 44, cw = Math.min(104, (mainW - 4 * 6) / CATS.length);
        for (String c : CATS) {
            final String cc = c;
            this.addDrawableChild(new HWButton(cx, cy, cw, 18, Text.literal(c), c.equals(cat) ? HWButton.PRIMARY : HWButton.SECONDARY, 0,
                b -> { cat = cc; reopen(); }));
            cx += cw + 6;
        }
        // grille de widgets
        List<HWHudManager.El> vis = new ArrayList<>();
        for (HWHudManager.El e : HWHudManager.ELEMENTS) if (cat.equals("Tous") || e.category.equals(cat)) vis.add(e);
        int cols = mainW > 520 ? 2 : 1, gap = 8;
        int gridTop = py + 70, gridBottom = py + ph - 42;
        int cardW = (mainW - (cols - 1) * gap) / cols;
        int rows = Math.max(1, (vis.size() + cols - 1) / cols);
        int cardH = Math.max(20, Math.min(30, (gridBottom - gridTop - (rows - 1) * gap) / rows));
        for (int i = 0; i < vis.size(); i++) {
            final HWHudManager.El e = vis.get(i);
            int col = i % cols, row = i / cols;
            int x = mainX + col * (cardW + gap), y = gridTop + row * (cardH + gap);
            if (y + cardH > gridBottom) break;
            String lbl = "§f" + e.name + "   " + (e.enabled ? "§aON" : "§8OFF");
            this.addDrawableChild(new HWButton(x, y, cardW - 24, cardH, Text.literal(lbl), HWButton.SECONDARY, iconKind(e.id),
                b -> { e.enabled = !e.enabled; HWHudManager.save(); reopen(); }));
            this.addDrawableChild(new HWButton(x + cardW - 22, y, 22, cardH, Text.literal(""), HWButton.SECONDARY, 5,
                b -> this.client.setScreen(new HWModSettingsScreen(this, e))));
        }
        // editer le HUD
        this.addDrawableChild(new HWButton(mainX, py + ph - 30, 150, 22, Text.literal("Editer le HUD"), HWButton.PRIMARY, 0,
            b -> this.client.setScreen(new HWHudEditScreen(this))));
    }

    private void initSettings() {
        int x = mainX, y = py + 48, w = Math.min(320, mainW);
        this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Options Minecraft"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options)))); y += 28;
        this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Theme du menu"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWThemeScreen(this)))); y += 28;
        this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Cosmetiques (vestiaire)"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWCosmeticsScreen(this)))); y += 28;
        boolean hud = !(this.client.options != null && this.client.options.hudHidden);
        this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Afficher le HUD : " + (hud ? "§aON" : "§8OFF")), HWButton.SECONDARY, 0,
            b -> { if (this.client.options != null) this.client.options.hudHidden = !this.client.options.hudHidden; reopen(); }));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) ctx.fill(0, 0, this.width, this.height, OVERLAY); // voile sombre leger uniforme
        else { HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0xB0000000); }

        ctx.fill(px, py, px + pw, py + ph, SURF);
        border(ctx, px, py, pw, ph, BORDER);
        ctx.fill(px, py + 32, px + pw, py + 33, 0x33D6B85B);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§6HEROES-WORLD"), px + 14, py + 13, GOLD);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§8v1.1.6"), px + 14 + this.textRenderer.getWidth("HEROES-WORLD") + 8, py + 14, TXT2);

        if (showProfiles) {
            ctx.fill(px + 8, py + 40, px + 152, py + ph - 8, RAISED);
            border(ctx, px + 8, py + 40, 144, ph - 48, BORDER);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7PROFILS"), px + 14, py + 48, TXT2);
        }

        if (tab == 2) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Aucun waypoint pour l'instant."), mainX + mainW / 2, py + ph / 2 - 8, TXT2);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§8Les waypoints (par dimension, couleur, distance) arrivent bientot."), mainX + mainW / 2, py + ph / 2 + 6, TXT2);
        } else if (tab == 1) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Reglages"), mainX, py + 40, TXT2);
        }
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

    private static void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override public void close() { back(); }
    @Override public boolean shouldPause() { return false; }
}
