package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.Random;

/**
 * Écran titre HEROES-WORLD — 100% dessiné en code (aucune texture -> jamais de "damier").
 * Fond spatial + étoiles, logo net en avant, bouton unique JOUER, sélecteur de thème,
 * barre du bas lisible. Style épuré façon Lunar, couleurs Olympe.
 */
public class HWTitleScreen extends Screen {

    private static final int GOLD      = 0xFFE8C56A;
    private static final int GOLD_SOFT = 0xFFF0D488;
    private static final int GOLD_DARK = 0xFFB8912F;

    // Thèmes de fond : {haut, bas, teinte étoile}
    private static final int[][] THEMES = {
        { 0xFF0B1222, 0xFF05070E, 0xFFBFD8FF }, // Nuit étoilée
        { 0xFF17130A, 0xFF080604, 0xFFF3E4B0 }, // Olympe (doré)
        { 0xFF0A0A10, 0xFF050507, 0xFFC8C8D8 }  // Sombre
    };
    private static final String[] THEME_NAMES = { "Nuit étoilée", "Olympe", "Sombre" };
    private static int theme = 0;

    public HWTitleScreen() { super(Text.literal("HEROES-WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int by = (int) (this.height * 0.56);

        this.addDrawableChild(new HWButton(cx - 130, by, 260, 30, Text.literal("JOUER"), true, b -> this.connect()));
        this.addDrawableChild(new HWButton(cx - 130, by + 38, 126, 22, Text.literal("Options"), false,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(cx + 4, by + 38, 126, 22, Text.literal("Quitter"), false,
            b -> this.client.scheduleStop()));

        // Sélecteur de thème (change le fond, en jeu)
        this.addDrawableChild(new HWButton(cx - 130, by + 66, 260, 18,
            Text.literal("Thème : " + THEME_NAMES[theme]), false, b -> {
                theme = (theme + 1) % THEMES.length;
                this.client.setScreen(new HWTitleScreen());
            }));
    }

    private void connect() {
        String target = HWConfig.address();
        ServerInfo info = new ServerInfo("HEROES-WORLD", target, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(target), info, false, null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int[] th = THEMES[theme];
        // Fond dégradé
        ctx.fillGradient(0, 0, this.width, this.height, th[0], th[1]);
        // Étoiles (positions stables via seed fixe)
        Random rnd = new Random(20260713L);
        for (int i = 0; i < 80; i++) {
            int sx = (int) (rnd.nextFloat() * this.width);
            int sy = (int) (rnd.nextFloat() * this.height);
            int a = 0x30 + rnd.nextInt(0xB0);
            int base = th[2] & 0xFFFFFF;
            ctx.fill(sx, sy, sx + 1, sy + 1, (a << 24) | base);
        }
        // Vignette + liserés or
        ctx.fillGradient(0, 0, this.width, 70, 0x88000000, 0x00000000);
        ctx.fillGradient(0, this.height - 90, this.width, this.height, 0x00000000, 0xB0000000);
        ctx.fill(0, 0, this.width, 1, GOLD_DARK);

        int cx = this.width / 2;

        // ----- Panneau logo (encadré, net, au premier plan) -----
        int pw = Math.min(this.width - 100, 540);
        int ph = 96;
        int px = cx - pw / 2;
        int py = (int) (this.height * 0.15);
        ctx.fillGradient(px, py, px + pw, py + ph, 0xB0000913, 0xB00A0A14);
        drawFrame(ctx, px, py, pw, ph, 0x66E8C56A);
        // barre d'accent supérieure
        ctx.fill(px + 14, py + 12, px + pw - 14, py + 13, 0x33E8C56A);

        // Titre "HEROES-WORLD" en gros, net, doré (double passe = relief)
        drawCentered(ctx, "HEROES-WORLD", cx, py + 26, 3.0f, 0xFF3A2E10);
        drawCentered(ctx, "HEROES-WORLD", cx, py + 24, 3.0f, GOLD);
        // Sous-titre
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("§7Survie   ·   Mythologie   ·   Progression"),
            cx, py + 66, 0xFFCFC9DA);

        // ----- Barre du bas lisible (opaque) -----
        int barTop = this.height - 20;
        ctx.fill(0, barTop, this.width, this.height, 0xF00A0A12);
        ctx.fill(0, barTop, this.width, barTop + 1, 0x55E8C56A);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§eHEROES-WORLD §8· §71.20.6"), 8, barTop + 6, GOLD);
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            String tag = "§7Connecté : §f" + who;
            int w = this.textRenderer.getWidth(Text.literal(tag));
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(tag), this.width - w - 8, barTop + 6, 0xFFFFFFFF);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // Cadre 1px
    private void drawFrame(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    private void drawCentered(DrawContext ctx, String text, int centerX, int y, float scale, int color) {
        int w = this.textRenderer.getWidth(text);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX - (w * scale) / 2f, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawText(this.textRenderer, text, 0, 0, color, false);
        ctx.getMatrices().pop();
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
