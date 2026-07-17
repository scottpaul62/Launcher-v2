package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Centre Heroes-World : onglets Mods / Reglages / Waypoints, profils, recherche instantanee, sous-pages de reglages. */
public class HWModsScreen extends Screen {
    private final Screen parent;
    // DESIGN_TOKENS
    private static final int SURF = 0xF2111318, RAISED = 0xFF181B22, BORDER = 0xFF323844,
            FOCUS = 0xFF49BDF2, GOLD = 0xFFD6B85B, TXT = 0xFFF4F5F7, TXT2 = 0xFFA9AFBA, OVERLAY = 0xB8080A0D;

    private static int tab = 0;                 // 0 Mods, 1 Reglages, 2 Waypoints
    private static int scat = 0;                // sous-page reglages : 0 General, 1 Performance, 2 Interface, 3 Accessibilite
    private static String cat = "Tous";
    private static String search = "";
    private static final String[] TABS = {"MODS", "REGLAGES", "WAYPOINTS"};
    private static final String[] CATS = {"Tous", "Performance", "Info", "Mecanique", "Combat"};
    private static final String[] SCATS = {"General", "Performance", "Interface", "Accessibilite"};

    private int pw, ph, px, py, mainX, mainW;
    private boolean showProfiles;
    private TextFieldWidget searchField;
    private final List<ClickableWidget> gridWidgets = new ArrayList<>();

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
        gridWidgets.clear();

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
                this.addDrawableChild(new HWButton(px + 12, y, 138, 22,
                    Text.literal(profColor(n) + "\u25cf " + (act ? "§f" : "§7") + n),
                    act ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { HWProfiles.switchTo(n); reopen(); }));
                y += 26;
            }
            this.addDrawableChild(new HWButton(px + 12, y + 2, 138, 20, Text.literal("+ Nouveau profil"), HWButton.SECONDARY, 0,
                b -> { HWProfiles.create("Profil " + (HWProfiles.names.size() + 1)); reopen(); }));
        }

        if (tab == 0) initMods();
        else if (tab == 1) initSettings();
        else initWaypoints();

        // fermer (bas)
        this.addDrawableChild(new HWButton(px + pw - 110, py + ph - 30, 96, 22, Text.literal("Fermer"), HWButton.SECONDARY, 0, b -> back()));
    }

    private void initMods() {
        // recherche instantanee
        searchField = new TextFieldWidget(this.textRenderer, mainX, py + 40, mainW, 16, Text.literal("Rechercher"));
        searchField.setMaxLength(40);
        searchField.setText(search);
        searchField.setChangedListener(s -> { search = s; rebuildGrid(); });
        this.addDrawableChild(searchField);

        // categories
        int cx = mainX, cy = py + 62, cw = Math.min(104, (mainW - 4 * 6) / CATS.length);
        for (String c : CATS) {
            final String cc = c;
            this.addDrawableChild(new HWButton(cx, cy, cw, 18, Text.literal(c), c.equals(cat) ? HWButton.PRIMARY : HWButton.SECONDARY, 0,
                b -> { cat = cc; reopen(); }));
            cx += cw + 6;
        }

        buildGrid();

        // editer le HUD
        this.addDrawableChild(new HWButton(mainX, py + ph - 30, 150, 22, Text.literal("Editer le HUD"), HWButton.PRIMARY, 0,
            b -> this.client.setScreen(new HWHudEditScreen(this))));
    }

    private void rebuildGrid() {
        for (ClickableWidget w : gridWidgets) this.remove(w);
        gridWidgets.clear();
        buildGrid();
    }

    private void buildGrid() {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<HWHudManager.El> vis = new ArrayList<>();
        for (HWHudManager.El e : HWHudManager.ELEMENTS) {
            if (!(cat.equals("Tous") || e.category.equals(cat))) continue;
            if (!q.isEmpty() && !e.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            vis.add(e);
        }
        int cols = mainW > 520 ? 2 : 1, gap = 8;
        int gridTop = py + 86, gridBottom = py + ph - 42;
        int cardW = (mainW - (cols - 1) * gap) / cols;
        int rows = Math.max(1, (vis.size() + cols - 1) / cols);
        int cardH = Math.max(20, Math.min(30, (gridBottom - gridTop - (rows - 1) * gap) / rows));
        for (int i = 0; i < vis.size(); i++) {
            final HWHudManager.El e = vis.get(i);
            int col = i % cols, row = i / cols;
            int x = mainX + col * (cardW + gap), y = gridTop + row * (cardH + gap);
            if (y + cardH > gridBottom) break;
            String lbl = "§f" + e.name + "   " + (e.enabled ? "§aON" : "§8OFF");
            ClickableWidget main = this.addDrawableChild(new HWButton(x, y, cardW - 24, cardH, Text.literal(lbl), HWButton.SECONDARY, iconKind(e.id),
                b -> { e.enabled = !e.enabled; HWHudManager.save(); rebuildGrid(); }));
            ClickableWidget cfg = this.addDrawableChild(new HWButton(x + cardW - 22, y, 22, cardH, Text.literal(""), HWButton.SECONDARY, 5,
                b -> this.client.setScreen(new HWModSettingsScreen(this, e))));
            gridWidgets.add(main);
            gridWidgets.add(cfg);
        }
    }

    private void initWaypoints() {
        boolean inGame = this.client.world != null && this.client.player != null;
        this.addDrawableChild(new HWButton(mainX, py + 42, 210, 22,
            Text.literal(inGame ? "+ Waypoint a ma position" : "§8+ Waypoint (en jeu seulement)"),
            inGame ? HWButton.PRIMARY : HWButton.SECONDARY, 0,
            b -> { if (this.client.world != null && this.client.player != null) { HWWaypoints.addAtPlayer(this.client); reopen(); } }));

        int y = py + 74;
        int maxRows = Math.max(1, (py + ph - 46 - y) / 26);
        java.util.List<HWWaypoints.WP> list = HWWaypoints.LIST;
        for (int i = 0; i < list.size() && i < maxRows; i++) {
            final HWWaypoints.WP wp = list.get(i);
            this.addDrawableChild(new HWModSettingsScreen.HWSwatch(mainX, y + 2, 18, 18, wp.color, () -> false,
                b -> { HWWaypoints.cycleColor(wp); reopen(); }));
            String d = (inGame && HWWaypoints.curDim(this.client).equals(wp.dim)) ? ("  §b" + HWWaypoints.dist(this.client, wp) + "m") : "";
            String lbl = (wp.visible ? "§f" : "§8") + wp.name + " §7(" + (int) wp.x + ", " + (int) wp.y + ", " + (int) wp.z + ") "
                + HWWaypoints.dimShort(wp.dim) + d + (wp.visible ? "" : "  §8[masque]");
            this.addDrawableChild(new HWButton(mainX + 24, y, mainW - 24 - 28, 22, Text.literal(lbl), HWButton.SECONDARY, 0,
                b -> { wp.visible = !wp.visible; HWWaypoints.save(); reopen(); }));
            this.addDrawableChild(new HWButton(mainX + mainW - 24, y, 24, 22, Text.literal("§cX"), HWButton.SECONDARY, 0,
                b -> { HWWaypoints.remove(wp); reopen(); }));
            y += 26;
        }
    }

    private void initSettings() {
        // sous-onglets
        int sg = 6, scw = Math.min(120, (mainW - (SCATS.length - 1) * sg) / SCATS.length);
        int sx = mainX, sy = py + 42;
        for (int i = 0; i < SCATS.length; i++) {
            final int s = i;
            this.addDrawableChild(new HWButton(sx + i * (scw + sg), sy, scw, 18, Text.literal(SCATS[i]),
                i == scat ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { scat = s; reopen(); }));
        }

        int x = mainX, y = py + 72, w = Math.min(340, mainW);
        switch (scat) {
            case 0: { // General
                this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Options Minecraft"), HWButton.SECONDARY, 0,
                    b -> this.client.setScreen(new OptionsScreen(this, this.client.options)))); y += 28;
                this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Theme du menu"), HWButton.SECONDARY, 0,
                    b -> this.client.setScreen(new HWThemeScreen(this)))); y += 28;
                this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Cosmetiques (vestiaire)"), HWButton.SECONDARY, 0,
                    b -> this.client.setScreen(new HWCosmeticsScreen(this)))); y += 28;
                break;
            }
            case 1: { // Performance
                this.addDrawableChild(toggle(x, y, w, "Particules cosmetiques", HWClientConfig.cosmeticsParticles,
                    () -> { HWClientConfig.cosmeticsParticles = !HWClientConfig.cosmeticsParticles; })); y += 28;
                boolean hud = !(this.client.options != null && this.client.options.hudHidden);
                this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Afficher le HUD : " + (hud ? "§aON" : "§8OFF")), HWButton.SECONDARY, 0,
                    b -> { if (this.client.options != null) this.client.options.hudHidden = !this.client.options.hudHidden; reopen(); })); y += 28;
                break;
            }
            case 2: { // Interface
                this.addDrawableChild(toggle(x, y, w, "Fond des widgets HUD", HWClientConfig.hudBackground,
                    () -> { HWClientConfig.hudBackground = !HWClientConfig.hudBackground; })); y += 28;
                this.addDrawableChild(toggle(x, y, w, "Sons des menus", HWClientConfig.menuSounds,
                    () -> { HWClientConfig.menuSounds = !HWClientConfig.menuSounds; })); y += 28;
                this.addDrawableChild(toggle(x, y, w, "Effets de l'ecran titre", HWClientConfig.titleFx,
                    () -> { HWClientConfig.titleFx = !HWClientConfig.titleFx; })); y += 28;
                this.addDrawableChild(new HWButton(x, y, w, 24, Text.literal("Editer le HUD"), HWButton.SECONDARY, 0,
                    b -> this.client.setScreen(new HWHudEditScreen(this)))); y += 28;
                break;
            }
            case 3: { // Accessibilite
                this.addDrawableChild(toggle(x, y, w, "Reduire les animations (menus)", HWClientConfig.reduceMotion,
                    () -> { HWClientConfig.reduceMotion = !HWClientConfig.reduceMotion; })); y += 28;
                this.addDrawableChild(toggle(x, y, w, "Contraste eleve du HUD", HWClientConfig.highContrast,
                    () -> { HWClientConfig.highContrast = !HWClientConfig.highContrast; })); y += 28;
                break;
            }
        }
    }

    private HWButton toggle(int x, int y, int w, String label, boolean state, Runnable flip) {
        return new HWButton(x, y, w, 24, Text.literal(label + " : " + (state ? "§aON" : "§8OFF")), HWButton.SECONDARY, 0,
            b -> { flip.run(); HWClientConfig.save(); reopen(); });
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) ctx.fill(0, 0, this.width, this.height, OVERLAY); // voile sombre leger uniforme
        else { HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0xB0000000); }

        HWDraw.panel(ctx, px, py, pw, ph, 8, SURF, BORDER);
        ctx.fill(px, py + 32, px + pw, py + 33, 0x33D6B85B);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§6HEROES-WORLD"), px + 14, py + 13, GOLD);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§8v1.1.6"), px + 14 + this.textRenderer.getWidth("HEROES-WORLD") + 8, py + 14, TXT2);

        if (showProfiles) {
            HWDraw.panel(ctx, px + 8, py + 40, 144, ph - 48, 6, RAISED, BORDER);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7PROFILS"), px + 14, py + 48, TXT2);
        }

        if (tab == 2 && HWWaypoints.LIST.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Aucun waypoint pour l'instant."), mainX + mainW / 2, py + ph / 2 - 8, TXT2);
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§8Clique « + Waypoint a ma position » en jeu pour en creer un."), mainX + mainW / 2, py + ph / 2 + 6, TXT2);
        }
        if (tab == 2 && !HWWaypoints.LIST.isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§8pastille = couleur · clic = afficher/masquer · X = supprimer"), mainX, py + ph - 26, TXT2);
        }
        super.render(ctx, mouseX, mouseY, delta);
        if (tab == 0 && searchField != null && searchField.getText().isEmpty() && !searchField.isFocused()) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§8Rechercher un mod..."), mainX + 5, py + 44, TXT2);
        }
    }

    /** Couleur d'accent d'un profil (pastille), pour une colonne plus vivante. */
    private static String profColor(String n) {
        String l = n.toLowerCase(Locale.ROOT);
        if (l.contains("pvp")) return "§c";
        if (l.contains("survie")) return "§a";
        if (l.contains("skyblock")) return "§b";
        if (l.contains("perf")) return "§9";
        if (l.contains("heroes")) return "§6";
        if (l.contains("defaut")) return "§e";
        return "§d";
    }

    private static int iconKind(String id) {
        switch (id) {
            case "fps": return 6; case "coords": return 7; case "direction": return 8; case "time": return 9;
            case "day": return 10; case "session": return 11; case "keystrokes": return 12; case "armor": return 13;
            case "ping": return 14; case "effects": return 15; case "cps": return 16;
            case "speed": return 17; case "xp": return 18; case "itemcount": return 19;
            case "scoreboard": return 20; case "chat": return 21; case "crosshair": return 22; case "waypoints": return 23;
            default: return 0;
        }
    }

    @Override public void close() { back(); }
    @Override public boolean shouldPause() { return false; }
}
