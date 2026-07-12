package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Écran titre HERO WORLD : image de fond nette + menu organisé (aucun flou global). */
public class HWTitleScreen extends Screen {

    private static final int GOLD = 0xFFE8C56A;
    private int clusterTop, clusterBottom;

    public HWTitleScreen() { super(Text.literal("HERO WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int py = (int) (this.height * 0.44);
        int gridTop = py + 40;
        int lx = cx - 150, rx = cx + 4, cwd = 146;
        clusterTop = py - 16;
        clusterBottom = gridTop + 60 + 24 + 14;

        this.addDrawableChild(new HWButton(cx - 150, py, 300, 30, Text.literal("REJOINDRE HEROES-WORLD"), true, b -> this.connect()));

        this.addDrawableChild(new HWButton(lx, gridTop, cwd, 24, Text.literal("Amis"), false,
            b -> this.client.setScreen(new HWSoonScreen(this, "Amis", "La liste d'amis arrivera avec l'hébergement du serveur HEROES-WORLD."))));
        this.addDrawableChild(new HWButton(rx, gridTop, cwd, 24, Text.literal("Personnalisation"), false,
            b -> this.client.setScreen(new HWSoonScreen(this, "Personnalisation", "Cosmétiques (capes, auras, compagnons, emotes…) — bientôt avec la boutique HEROES-WORLD."))));
        this.addDrawableChild(new HWButton(lx, gridTop + 30, cwd, 24, Text.literal("Thèmes"), false,
            b -> this.client.setScreen(new HWThemeScreen(this))));
        this.addDrawableChild(new HWButton(rx, gridTop + 30, cwd, 24, Text.literal("Options"), false,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(lx, gridTop + 60, cwd, 24, Text.literal("Paramètres du client"), false,
            b -> this.client.setScreen(new HWSoonScreen(this, "Paramètres du client", "Réglages du client HERO WORLD (performance, HUD, raccourcis) — bientôt."))));
        this.addDrawableChild(new HWButton(rx, gridTop + 60, cwd, 24, Text.literal("Quitter"), false,
            b -> this.client.scheduleStop()));
    }

    private void connect() {
        String target = HWConfig.address();
        ServerInfo info = new ServerInfo("HEROES-WORLD", target, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(target), info, false, null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        try { this.client.getWindow().setTitle(HWBrand.WINDOW_TITLE); } catch (Throwable ignored) {}
        HWScene.draw(ctx, this.width, this.height);

        // Scrim CIBLÉ derrière le menu (lisibilité) — le reste du décor reste net
        int cx = this.width / 2;
        ctx.fillGradient(cx - 168, clusterTop, cx + 168, clusterBottom, 0xB0060810, 0xB0060810);
        ctx.fill(cx - 168, clusterTop, cx + 168, clusterTop + 1, 0x55E8C56A);
        ctx.fill(cx - 168, clusterBottom - 1, cx + 168, clusterBottom, 0x33000000);

        HWLogo.draw(ctx, this.textRenderer, this.width, (int) (this.height * 0.07));

        // Barre du bas (semi-transparente, lisible)
        int barTop = this.height - 20;
        ctx.fillGradient(0, barTop, this.width, this.height, 0xC00A0A12, 0xE00A0A12);
        ctx.fill(0, barTop, this.width, barTop + 1, 0x55E8C56A);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("§e" + HWBrand.NAME + " §8" + HWBrand.VERSION + " §8· Minecraft " + HWBrand.MC),
            8, barTop + 6, GOLD);
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            String tag = "§7Connecté : §f" + who;
            int tw = this.textRenderer.getWidth(Text.literal(tag));
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(tag), this.width - tw - 8, barTop + 6, 0xFFFFFFFF);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
