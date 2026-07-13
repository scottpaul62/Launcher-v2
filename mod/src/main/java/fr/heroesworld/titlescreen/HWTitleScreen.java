package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Écran titre HERO WORLD : image plein écran + Rejoindre au centre + dock premium en bas. */
public class HWTitleScreen extends Screen {

    private static final int GOLD = 0xFFE8C56A;
    private int dockX, dockY, dockW, dockH;

    public HWTitleScreen() { super(Text.literal("HERO WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;

        int ry = (int) (this.height * 0.44);
        this.addDrawableChild(new HWButton(cx - 170, ry, 340, 40, Text.literal("REJOINDRE HEROES-WORLD"), HWButton.PRIMARY, 0, b -> this.connect()));
        this.addDrawableChild(new HWButton(cx - 100, ry + 50, 200, 22, Text.literal("Quitter"), HWButton.SECONDARY, 0, b -> this.client.scheduleStop()));

        int itemW = 122, gap = 6, n = 5;
        dockW = n * itemW + (n - 1) * gap;
        dockH = 44;
        dockX = cx - dockW / 2;
        dockY = this.height - 74;

        this.addDrawableChild(new HWButton(dockX, dockY, itemW, dockH, Text.literal("Amis"), HWButton.DOCK, 1,
            b -> this.client.setScreen(new HWSoonScreen(this, "Amis", "La liste d'amis arrivera avec l'hébergement du serveur HEROES-WORLD."))));
        this.addDrawableChild(new HWButton(dockX + (itemW + gap), dockY, itemW, dockH, Text.literal("Thèmes"), HWButton.DOCK, 2,
            b -> this.client.setScreen(new HWThemeScreen(this))));
        this.addDrawableChild(new HWButton(dockX + 2 * (itemW + gap), dockY, itemW, dockH, Text.literal("Personnalisation"), HWButton.DOCK, 3,
            b -> this.client.setScreen(new HWCosmeticsScreen(this))));
        this.addDrawableChild(new HWButton(dockX + 3 * (itemW + gap), dockY, itemW, dockH, Text.literal("Options"), HWButton.DOCK, 4,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(dockX + 4 * (itemW + gap), dockY, itemW, dockH, Text.literal("Paramètres"), HWButton.DOCK, 5,
            b -> this.client.setScreen(new HWClientSettings(this))));
    }

    private void connect() {
        String target = HWConfig.address();
        ServerInfo info = new ServerInfo("HEROES-WORLD", target, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(target), info, false, null);
    }

    // Supprime le fond vanilla de Minecraft (panorama + flou).
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        try { this.client.getWindow().setTitle(HWBrand.WINDOW_TITLE); } catch (Throwable ignored) {}
        HWScene.draw(ctx, this.width, this.height);

        int cx = this.width / 2;

        // Statut sous le logo (l'image contient déjà HEROES-WORLD)
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6◆ §eL'OLYMPE VOUS ATTEND §6◆"),
            cx, (int) (this.height * 0.44) - 22, 0xFFE8C56A);

        // Fond du dock (barre premium)
        int x1 = dockX - 12, y1 = dockY - 8, x2 = dockX + dockW + 12, y2 = dockY + dockH + 8;
        ctx.fillGradient(x1, y1, x2, y2, 0x99070912, 0xC0070912);
        ctx.fill(x1, y1, x2, y1 + 1, 0x55E8C56A);
        ctx.fill(x1, y2 - 1, x2, y2, 0x33000000);

        // Puce compte (haut-droite)
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            int tw = this.textRenderer.getWidth(who);
            int pw = tw + 40, ph = 22, px = this.width - pw - 12, py = 10;
            ctx.fillGradient(px, py, px + pw, py + ph, 0xB00A0C14, 0xB00A0C14);
            ctx.fill(px, py, px + pw, py + 1, 0x55E8C56A);
            ctx.fill(px, py + ph - 1, px + pw, py + ph, 0x33000000);
            ctx.fill(px + 12, py + ph / 2 - 3, px + 18, py + ph / 2 + 3, 0xFF7CCB6E); // pastille en ligne
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§f" + who), px + 24, py + 7, 0xFFFFFFFF);
        }

        // Version discrète (bas-gauche)
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§eHERO WORLD §7" + HWBrand.VERSION + " §8· Minecraft " + HWBrand.MC),
            8, this.height - 12, GOLD);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
