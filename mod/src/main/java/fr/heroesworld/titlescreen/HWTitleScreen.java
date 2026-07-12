package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Écran titre HERO WORLD : scène animée de l'Olympe + logo net + boutons composés. */
public class HWTitleScreen extends Screen {

    private static final int GOLD = 0xFFE8C56A;

    public HWTitleScreen() { super(Text.literal("HERO WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = (int) (this.height * 0.50);
        this.addDrawableChild(new HWButton(cx - 160, y, 320, 34, Text.literal("REJOINDRE HEROES-WORLD"), true, b -> this.connect()));
        this.addDrawableChild(new HWButton(cx - 160, y + 42, 156, 24, Text.literal("Options"), false,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(cx + 4, y + 42, 156, 24, Text.literal("Personnalisation"), false,
            b -> this.client.setScreen(new HWSoonScreen(this, "Personnalisation",
                "Cosmétiques (capes, auras, compagnons, emotes…) — bientôt avec la boutique HEROES-WORLD."))));
        this.addDrawableChild(new HWButton(cx - 160, y + 70, 156, 24, Text.literal("Thèmes"), false,
            b -> this.client.setScreen(new HWThemeScreen(this))));
        this.addDrawableChild(new HWButton(cx + 4, y + 70, 156, 24, Text.literal("Quitter"), false,
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
        HWLogo.draw(ctx, this.textRenderer, this.width, (int) (this.height * 0.14));

        // Barre du bas lisible
        int barTop = this.height - 20;
        ctx.fill(0, barTop, this.width, this.height, 0xF00A0A12);
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
