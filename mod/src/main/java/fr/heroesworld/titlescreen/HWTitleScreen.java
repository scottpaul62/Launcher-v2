package fr.heroesworld.titlescreen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Écran titre HEROES-WORLD — épuré façon Lunar, aux couleurs de l'Olympe.
 * Logo NET mis en avant, boutons custom sleek (pas de gris vanilla), structure centrée.
 */
public class HWTitleScreen extends Screen {

    private static final Identifier BANNER = new Identifier("heroworld", "textures/gui/banner.png");
    private static final int GOLD      = 0xFFE8C56A;
    private static final int GOLD_DARK = 0xFFB8912F;
    private static final int BG_TOP    = 0xFF08080E;
    private static final int BG_BOT    = 0xFF14141E;

    public HWTitleScreen() { super(Text.literal("HEROES-WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int by = (int) (this.height * 0.55);

        // bouton principal
        this.addDrawableChild(new HWButton(cx - 120, by, 240, 30, Text.literal("JOUER"), true, b -> this.connect()));
        // secondaires (rangée)
        this.addDrawableChild(new HWButton(cx - 120, by + 38, 117, 22, Text.literal("Options"), false,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(cx + 3, by + 38, 117, 22, Text.literal("Quitter"), false,
            b -> this.client.scheduleStop()));
    }

    private void connect() {
        String target = HWConfig.address();
        ServerInfo info = new ServerInfo("HEROES-WORLD", target, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(target), info, false, null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // fond
        ctx.fillGradient(0, 0, this.width, this.height, BG_TOP, BG_BOT);
        // vignette douce aux bords (le centre reste clair -> le logo ressort)
        ctx.fillGradient(0, 0, this.width, 60, 0x99000000, 0x00000000);
        ctx.fillGradient(0, this.height - 90, this.width, this.height, 0x00000000, 0xBB000000);
        // liserés or haut/bas
        ctx.fill(0, 0, this.width, 1, GOLD_DARK);
        ctx.fill(0, this.height - 1, this.width, this.height, GOLD_DARK);

        int cx = this.width / 2;

        // LOGO net, mis en avant (pleine luminosité, pas assombri)
        int logoW = Math.min(this.width - 160, 520);
        int logoH = logoW / 4;
        int logoX = cx - logoW / 2;
        int logoY = (int) (this.height * 0.13);
        boolean drew = false;
        try {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            ctx.drawTexture(BANNER, logoX, logoY, logoW, logoH, 0f, 0f, 2048, 512, 2048, 512);
            drew = true;
        } catch (Throwable ignored) { drew = false; }
        if (!drew) drawScaled(ctx, "HEROES-WORLD", cx, logoY + logoH / 2 - 10, 2.6f, GOLD);

        // sous-titre
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("§7Survie   ·   Mythologie   ·   Progression"),
            cx, logoY + logoH + 2, 0xFFC8C2D4);

        // barre du bas + infos
        int barY = this.height - 20;
        ctx.fill(0, barY, this.width, barY + 1, 0x33E8C56A);
        ctx.fill(0, barY + 1, this.width, this.height, 0x66000000);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§8HEROES-WORLD  §71.20.6"), 8, barY + 6, 0xFF8A85A0);
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            String tag = "§7Connecté : §e" + who;
            int w = this.textRenderer.getWidth(Text.literal(tag));
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(tag), this.width - w - 8, barY + 6, GOLD);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawScaled(DrawContext ctx, String text, int centerX, int y, float scale, int color) {
        int w = this.textRenderer.getWidth(text);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(centerX - (w * scale) / 2f, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawText(this.textRenderer, text, 0, 0, color, true);
        ctx.getMatrices().pop();
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
