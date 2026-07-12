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
 * Écran titre HEROES-WORLD — thème Olympe, façon Lunar.
 * Le logo (bannière) est mis en avant ; un seul chemin : rejoindre l'Olympe.
 */
public class HWTitleScreen extends Screen {

    private static final Identifier BANNER = new Identifier("heroworld", "textures/gui/banner.png");
    private static final int GOLD_DARK  = 0xFFD4AF37;
    private static final int MARBLE_TOP = 0xFF0D0D12;
    private static final int MARBLE_BOT = 0xFF1B1B26;

    public HWTitleScreen() {
        super(Text.literal("HEROES-WORLD"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int joinY = (int) (this.height * 0.56);

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("§lREJOINDRE HEROES-WORLD"), b -> this.connect())
            .dimensions(cx - 155, joinY, 310, 24).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Options"), b -> this.client.setScreen(new OptionsScreen(this, this.client.options)))
            .dimensions(cx - 155, joinY + 32, 151, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Quitter"), b -> this.client.scheduleStop())
            .dimensions(cx + 4, joinY + 32, 151, 20).build());
    }

    private void connect() {
        MinecraftClient client = this.client;
        String target = HWConfig.address();
        ServerInfo info = new ServerInfo("HEROES-WORLD", target, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, client, ServerAddress.parse(target), info, false, null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Fond marbre + liserés or
        ctx.fillGradient(0, 0, this.width, this.height, MARBLE_TOP, MARBLE_BOT);
        ctx.fill(0, 0, this.width, 2, GOLD_DARK);
        ctx.fill(0, this.height - 2, this.width, this.height, GOLD_DARK);

        int cx = this.width / 2;

        // ----- LOGO (bannière) mis en avant -----
        int logoW = Math.min(this.width - 80, 460);
        int logoH = logoW / 4; // ratio 2048x512 = 4:1
        int logoX = cx - logoW / 2;
        int logoY = (int) (this.height * 0.14);
        RenderSystem.enableBlend();
        ctx.drawTexture(BANNER, logoX, logoY, logoW, logoH, 0f, 0f, 2048, 512, 2048, 512);

        // Sous-titre
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("§7Survie  ·  Mythologie  ·  Progression"),
            cx, logoY + logoH + 8, 0xFFCFC7B2);

        // ----- Version en bas à gauche (façon Lunar) -----
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("HEROES-WORLD  1.20.6  ·  Fabric"), 4, this.height - 12, 0xFFB0AAC0);

        // Pseudo connecté en bas à droite
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            int w = this.textRenderer.getWidth(who);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(who), this.width - w - 6, this.height - 12, 0xFFFFD700);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
