package fr.heroesworld.titlescreen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Écran titre HEROES-WORLD — épuré, façon Lunar/Badlion mais aux couleurs de l'Olympe.
 * Logo mis en avant, un bouton principal JOUER, version en bas. Défensif : repli texte
 * si la texture ne se charge pas (jamais de crash sur l'écran titre).
 */
public class HWTitleScreen extends Screen {

    private static final Identifier BANNER = new Identifier("heroworld", "textures/gui/banner.png");
    private static final int GOLD       = 0xFFE8C56A;
    private static final int GOLD_DARK  = 0xFFB8912F;
    private static final int BG_TOP     = 0xFF0A0A10;
    private static final int BG_BOT     = 0xFF171722;

    public HWTitleScreen() { super(Text.literal("HEROES-WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int by = (int) (this.height * 0.58);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§l§eJOUER"), b -> this.connect())
            .dimensions(cx - 100, by, 200, 26).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Options"),
                b -> this.client.setScreen(new OptionsScreen(this, this.client.options)))
            .dimensions(cx - 100, by + 32, 96, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Quitter"),
                b -> this.client.scheduleStop())
            .dimensions(cx + 4, by + 32, 96, 20).build());
    }

    private void connect() {
        MinecraftClient client = this.client;
        String target = HWConfig.address();
        ServerInfo info = new ServerInfo("HEROES-WORLD", target, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, client, ServerAddress.parse(target), info, false, null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, BG_TOP, BG_BOT);
        // vignette douce
        ctx.fillGradient(0, 0, this.width, 48, 0x88000000, 0x00000000);
        ctx.fillGradient(0, this.height - 70, this.width, this.height, 0x00000000, 0xAA000000);
        // liserés or
        ctx.fill(0, 0, this.width, 1, GOLD_DARK);
        ctx.fill(0, this.height - 1, this.width, this.height, GOLD_DARK);

        int cx = this.width / 2;
        int logoW = Math.min(this.width - 120, 420);
        int logoH = logoW / 4;
        int logoX = cx - logoW / 2;
        int logoY = (int) (this.height * 0.16);

        boolean drew = false;
        try {
            RenderSystem.enableBlend();
            ctx.drawTexture(BANNER, logoX, logoY, logoW, logoH, 0f, 0f, 2048, 512, 2048, 512);
            drew = true;
        } catch (Throwable ignored) { drew = false; }
        if (!drew) drawScaled(ctx, "HEROES-WORLD", cx, logoY + logoH / 2 - 10, 2.4f, GOLD);

        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("§7Survie  ·  Mythologie  ·  Progression"),
            cx, logoY + logoH + 8, 0xFFB9B2C4);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§8HEROES-WORLD §71.20.6"), 6, this.height - 12, 0xFF8A85A0);
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            int w = this.textRenderer.getWidth(who);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§e" + who), this.width - w - 6, this.height - 12, GOLD);
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
