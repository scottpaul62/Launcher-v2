package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Écran titre HERO WORLD : image plein écran + Rejoindre au centre + barre d'outils en bas. */
public class HWTitleScreen extends Screen {

    private static final int GOLD = 0xFFE8C56A;

    public HWTitleScreen() { super(Text.literal("HERO WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // --- Fenêtre centrale : Rejoindre (+ Quitter dessous) ---
        int ry = (int) (this.height * 0.46);
        this.addDrawableChild(new HWButton(cx - 160, ry, 320, 32, Text.literal("REJOINDRE HEROES-WORLD"), true, b -> this.connect()));
        this.addDrawableChild(new HWButton(cx - 100, ry + 40, 200, 22, Text.literal("Quitter"), false, b -> this.client.scheduleStop()));

        // --- Barre d'outils en bas ---
        int bw = 142, gap = 8, n = 5;
        int total = n * bw + (n - 1) * gap;
        int sx = cx - total / 2;
        int by = this.height - 54;

        this.addDrawableChild(new HWButton(sx, by, bw, 22, Text.literal("Amis"), false,
            b -> this.client.setScreen(new HWSoonScreen(this, "Amis", "La liste d'amis arrivera avec l'hébergement du serveur HEROES-WORLD."))));
        this.addDrawableChild(new HWButton(sx + (bw + gap), by, bw, 22, Text.literal("Thèmes"), false,
            b -> this.client.setScreen(new HWThemeScreen(this))));
        this.addDrawableChild(new HWButton(sx + 2 * (bw + gap), by, bw, 22, Text.literal("Personnalisation"), false,
            b -> this.client.setScreen(new HWSoonScreen(this, "Personnalisation", "Cosmétiques (capes, auras, compagnons, emotes…) — bientôt avec la boutique HEROES-WORLD."))));
        this.addDrawableChild(new HWButton(sx + 3 * (bw + gap), by, bw, 22, Text.literal("Options"), false,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(sx + 4 * (bw + gap), by, bw, 22, Text.literal("Paramètres du client"), false,
            b -> this.client.setScreen(new HWSoonScreen(this, "Paramètres du client", "Réglages du client HERO WORLD (performance, HUD, raccourcis) — bientôt."))));
    }

    private void connect() {
        String target = HWConfig.address();
        ServerInfo info = new ServerInfo("HEROES-WORLD", target, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(target), info, false, null);
    }

    // Supprime le fond vanilla de Minecraft (panorama + flou) : fond 100% custom.
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        try { this.client.getWindow().setTitle(HWBrand.WINDOW_TITLE); } catch (Throwable ignored) {}
        HWScene.draw(ctx, this.width, this.height);

        // Barre du bas (fine, lisible)
        int barTop = this.height - 18;
        ctx.fillGradient(0, barTop, this.width, this.height, 0xB00A0A12, 0xDC0A0A12);
        ctx.fill(0, barTop, this.width, barTop + 1, 0x44E8C56A);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("§e" + HWBrand.NAME + " §8" + HWBrand.VERSION + " §8· Minecraft " + HWBrand.MC),
            8, barTop + 5, GOLD);
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            String tag = "§7Connecté : §f" + who;
            int tw = this.textRenderer.getWidth(Text.literal(tag));
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(tag), this.width - tw - 8, barTop + 5, 0xFFFFFFFF);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
