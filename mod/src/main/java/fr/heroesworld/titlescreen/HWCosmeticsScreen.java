package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Vestiaire HERO WORLD : liste des cosmetiques, personnage 3D anime, apercu du rendu AVANT d'equiper. */
public class HWCosmeticsScreen extends Screen {
    private final Screen parent;
    private static int cat = 0;   // 0 = auras, 1 = ailes
    private static int sel = -1;  // element selectionne (apercu) ; -1 = l'equipe courant

    private static final String[] AURA_DESC = {
        "Aucun cosmetique equipe.",
        "La foudre du maitre de l'Olympe crepite autour de toi.",
        "Un halo de lumiere divine flotte au-dessus de ta tete.",
        "Les flammes de la forge dansent a tes pieds.",
        "Des ames sombres tourbillonnent autour de toi.",
        "Des petales celestes tombent doucement du ciel."
    };
    private static final String[] WING_DESC = { "Aucun cosmetique equipe." };

    private int px, py, pw, ph;
    private boolean wide;
    private int vx, vy, vw, vh;             // zone du viewport 3D
    private float userYaw = 0f;             // rotation par glisser (axe vertical uniquement)
    private boolean rotDrag = false;
    private double lastDragX = 0;

    public HWCosmeticsScreen(Screen parent) { super(Text.literal("Vestiaire")); this.parent = parent; }
    private void reopen() { this.client.setScreen(new HWCosmeticsScreen(parent)); }

    private String[] names() { return cat == 0 ? HWCosmetics.AURA_NAMES : HWCosmetics.WING_NAMES; }
    private String[] descs() { return cat == 0 ? AURA_DESC : WING_DESC; }
    private int equipped() { return cat == 0 ? HWCosmetics.aura : HWCosmetics.wings; }
    private int selected() { int s = sel >= 0 ? sel : equipped(); return Math.min(s, names().length - 1); }

    @Override
    protected void init() {
        pw = Math.min(720, this.width - 30);
        ph = Math.min(430, this.height - 30);
        px = (this.width - pw) / 2; py = (this.height - ph) / 2;
        wide = pw >= 520;

        // onglets categorie (haut-gauche du panneau)
        this.addDrawableChild(new HWButton(px + 12, py + 34, 86, 20, Text.literal("Auras"),
            cat == 0 ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { cat = 0; sel = -1; reopen(); }));
        this.addDrawableChild(new HWButton(px + 102, py + 34, 86, 20, Text.literal("Ailes"),
            cat == 1 ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { cat = 1; sel = -1; reopen(); }));

        // liste (colonne gauche) : clic = SELECTIONNER (apercu), pas equiper
        String[] names = names();
        int y = py + 62;
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            boolean eq = equipped() == i, se = selected() == i;
            String label = (eq ? "§a✔ " : "") + (se ? "§f" : "§7") + names[i];
            this.addDrawableChild(new HWButton(px + 12, y + i * 25, 176, 21, Text.literal(label),
                se ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { sel = idx; reopen(); }));
        }

        // fiche : bouton Equiper / Retirer
        int s = selected();
        boolean isEq = equipped() == s;
        String btn = s == 0 ? "Tout retirer" : (isEq ? "Retirer" : "Equiper");
        int bx = wide ? px + pw - 178 : px + 12, bw = wide ? 166 : pw - 24, by = py + ph - 58;
        this.addDrawableChild(new HWButton(bx, by, bw, 22, Text.literal(btn), isEq && s != 0 ? HWButton.SECONDARY : HWButton.PRIMARY, 0, b -> {
            int target = (isEq && s != 0) ? 0 : s;
            if (cat == 0) HWCosmetics.setAura(target); else HWCosmetics.setWings(target);
            reopen();
        }));

        this.addDrawableChild(new HWButton(px + pw - 110, py + 8, 96, 20, Text.literal("Retour"),
            HWButton.SECONDARY, 0, b -> this.client.setScreen(parent != null ? parent : new HWTitleScreen())));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = this.client;
        if (mc.world != null) super.renderBackground(ctx, mouseX, mouseY, delta);
        else { if (!HWBg2.draw(ctx, this.width, this.height)) HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0x70000000); }

        // panneau principal
        HWDraw.panel(ctx, px, py, pw, ph, 8, 0xF2111318, 0xFF323844);
        ctx.fill(px, py + 28, px + pw, py + 29, 0x33D6B85B);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§6VESTIAIRE §eHERO WORLD"), px + 12, py + 10, 0xFFE8C56A);

        // zone 3D (centre)
        vx = px + 200; vy = py + 40;
        vw = (wide ? px + pw - 190 : px + pw - 12) - vx; vh = py + ph - 70 - vy;
        HWDraw.panel(ctx, vx, vy, vw, vh, 6, 0xFF181B22, 0xFF323844);

        int ccx = vx + vw / 2, ccy = vy + vh / 2;
        long t = System.currentTimeMillis();
        int s = selected();

        // personnage 3D : modele anime (glisser gauche/droite pour tourner), toujours DECOUPE au viewport
        int wings3d = (cat == 1) ? s : HWCosmetics.wings;
        int previewH = Math.min((int) (vh * 0.86f), (int) (vw * 1.35f)); // jamais plus large que la vitrine
        ctx.enableScissor(vx + 1, vy + 1, vx + vw - 1, vy + vh - 1);
        boolean entity = HWPlayerPreview.draw(ctx, mc, ccx, ccy + vh / 24, previewH, mouseX, mouseY, userYaw, wings3d);
        ctx.disableScissor();
        if (!entity) {
            try {
                net.minecraft.util.Identifier skin = HWSkin.texture(mc);
                int hs = Math.min(64, vh / 3);
                ctx.drawTexture(skin, ccx - hs / 2, ccy - hs / 2 - 8, hs, hs, 8f, 8f, 8, 8, 64, 64);
                ctx.drawTexture(skin, ccx - hs / 2, ccy - hs / 2 - 8, hs, hs, 40f, 8f, 8, 8, 64, 64);
            } catch (Throwable ignored) {}
        }

        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§8glisser pour tourner"), ccx, vy + vh - 14, 0xFF6E7480);

        // onglet Ailes : les modeles factices ont ete retires, les vrais modeles 3D arrivent
        if (cat == 1) {
            ctx.drawTextWrapped(this.textRenderer,
                Text.literal("§6◆ §7De vraies ailes 3D (modeles Blockbench) sont en cours d'import. Elles arriveront ici tres bientot."),
                px + 12, py + 96, 176, 0xFFA9AFBA);
        }

        // fiche (droite ou bas)
        String[] names = names(); String[] descs = descs();
        if (wide) {
            int fx = px + pw - 178, fw = 166, fy = py + 40;
            HWDraw.panel(ctx, fx, fy, fw, ph - 110, 6, 0xFF181B22, 0xFF323844);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§e" + names[s]), fx + 8, fy + 8, 0xFFE8C56A);
            ctx.drawTextWrapped(this.textRenderer, Text.literal("§7" + descs[s]), fx + 8, fy + 24, fw - 16, 0xFFA9AFBA);
            if (s > 0) { // apercu du cosmetique SEUL (encart anime)
                int ax2 = fx + 8, ay2 = fy + 64, aw2 = fw - 16, ah2 = 74;
                HWDraw.panel(ctx, ax2, ay2, aw2, ah2, 5, 0xFF10131A, 0xFF2A2F3A);
                int acx = ax2 + aw2 / 2, acy = ay2 + ah2 / 2;
                if (cat == 0) {
                    drawAuraPreview(ctx, s, acx, acy, aw2 - 12, ah2 + 30, t, true);
                    drawAuraPreview(ctx, s, acx, acy, aw2 - 12, ah2 + 30, t, false);
                } else {
                    drawWingsPreview(ctx, s, acx, acy + 12, ah2 + 40, t);
                }
            }
            int fb = fy + (ph - 110); // bas du panneau fiche
            if (equipped() == s && s != 0)
                ctx.drawTextWithShadow(this.textRenderer, Text.literal("§a✔ Equipe"), fx + 8, fb - 50, 0xFF48C78E);
            ctx.drawTextWrapped(this.textRenderer, Text.literal("§8Visible en 3e personne (F5)."), fx + 8, fb - 36, fw - 16, 0xFF8B8B98);
        } else {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§e" + names[s] + "  §7" + descs[s]), px + 12, py + ph - 76, 0xFFE8C56A);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ==================== apercus animes (representatifs des particules en jeu) ====================

    private static void dot(DrawContext ctx, int x, int y, int r, int col) {
        ctx.fill(x - r, y - r, x + r + 1, y + r + 1, col);
    }

    /** Aura : anneau de particules orbitantes autour du personnage. front=false dessine la moitie arriere. */
    private void drawAuraPreview(DrawContext ctx, int kind, int ccx, int ccy, int vw, int vh, long t, boolean back) {
        int rx = Math.min(vw / 3, 58), feetY = ccy + vh / 4;
        for (int i = 0; i < 14; i++) {
            double a = t * 0.0016 + i * (Math.PI * 2 / 14);
            double sx = Math.cos(a), sz = Math.sin(a);
            boolean isBack = sz < 0;
            if (isBack != back) continue;
            int gx = ccx + (int) (sx * rx);
            switch (kind) {
                case 1: { // eclairs de Zeus : etincelles or/blanches qui orbitent
                    int gy = ccy + (int) (Math.sin(a * 3) * 8);
                    dot(ctx, gx, gy, 1, (i % 3 == 0) ? 0xFFFFF3A0 : 0xFFE8C56A);
                    if (i % 4 == 0) dot(ctx, gx, gy - 6, 0, 0xFFFFFFFF);
                    break;
                }
                case 2: { // halo d'Apollon : anneau dore au-dessus de la tete
                    int hy = ccy - vh / 4 - 6 + (int) (Math.sin(t * 0.002) * 2);
                    dot(ctx, ccx + (int) (sx * 16), hy + (int) (sz * 3), 1, 0xFFFFEFC6);
                    break;
                }
                case 3: { // flammes d'Hephaistos : flammes qui montent des pieds
                    int fy2 = feetY - (int) ((t / 40 + i * 13) % 34);
                    int col = ((t / 40 + i * 13) % 34) > 22 ? 0x88E45B24 : 0xFFFF9A3C;
                    dot(ctx, ccx + (int) (sx * rx * 0.7), fy2, 1, col);
                    break;
                }
                case 4: { // ombre d'Hades : ames sombres lentes
                    int gy = ccy + (int) (Math.sin(a * 2 + 1) * 14);
                    dot(ctx, gx, gy, 1, (i % 2 == 0) ? 0xFF6C5BAF : 0xFF3E3566);
                    break;
                }
                case 5: { // petales d'Olympe : petales roses qui tombent
                    int fy2 = ccy - vh / 4 + (int) ((t / 30 + i * 17) % (vh / 2));
                    dot(ctx, ccx + (int) (sx * rx * 0.9) + (int) (Math.sin((t + i * 300) * 0.004) * 5), fy2, 1, 0xFFF2A9C4);
                    break;
                }
            }
        }
    }

    /** Ailes : deux arcs de particules qui battent derriere les epaules. */
    private void drawWingsPreview(DrawContext ctx, int kind, int ccx, int ccy, int vh, long t) {
        int col;
        switch (kind) {
            case 2: col = 0xFFBFE8FF; break;   // foudre
            case 3: col = 0xFFFF9A3C; break;   // flammes
            case 4: col = 0xFF7FB8FF; break;   // Hades
            case 5: col = 0xFFF3D889; break;   // Apollon (or)
            case 6: col = 0xFFF2A9C4; break;   // celestes (petales)
            case 7: col = 0xFFB07CE8; break;   // Vide (violet)
            case 8: col = 0xFFF3D889; break;   // Zeus 3D (encart : or)
            default: col = 0xFFF4F5F7;         // Hermes
        }
        double flap = Math.sin(t * 0.004) * 0.35;
        int seg = 7;
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            for (int i = 1; i <= seg; i++) {
                double f = i / (double) seg;
                int wx = ccx + sgn * (int) (10 + f * Math.min(46, vh / 5));
                int wy = ccy - (int) (Math.sin(f * Math.PI) * (16 + flap * 22) - f * 14);
                dot(ctx, wx, wy, 1, col);
                if (i % 2 == 0) dot(ctx, wx - sgn * 2, wy + 4, 0, (col & 0xFFFFFF) | 0x88000000);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && mx >= vx && mx <= vx + vw && my >= vy && my <= vy + vh) {
            rotDrag = true; lastDragX = mx;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (rotDrag) {
            userYaw += (float) ((mx - lastDragX) * 0.02); // rotation uniquement gauche/droite (axe vertical)
            lastDragX = mx;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (rotDrag) { rotDrag = false; return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override public void close() { this.client.setScreen(parent != null ? parent : new HWTitleScreen()); }
    @Override public boolean shouldPause() { return false; }
}
