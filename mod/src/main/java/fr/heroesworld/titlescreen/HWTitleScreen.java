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
    private int ry; // haut de la pile centrale (borne pour ne jamais chevaucher le dock)

    public HWTitleScreen() { super(Text.literal("HERO WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;

        // pile centrale bornee : jamais de chevauchement avec le dock en petite fenetre
        int dockTop = this.height - 74;
        int stackH = 36 + 6 + 22 + 4 + 22 + 8 + 20; // Rejoindre + Solo + Boutique + Quitter + marges
        ry = Math.max(34, Math.min((int) (this.height * 0.42), dockTop - stackH - 8));
        int joinW = Math.min(340, this.width - 60);
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry, joinW, 36, Text.literal("Rejoindre"), HWButton.PRIMARY, 0, b -> this.connect()));
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry + 42, joinW, 22, Text.literal("Solo"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWSoonScreen(this, "Solo", "Le mode Solo personnalise HERO WORLD arrivera bientot."))));
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry + 68, joinW, 22, Text.literal("Boutique"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWSoonScreen(this, "Boutique", "La boutique HERO WORLD arrivera bientot."))));
        this.addDrawableChild(new HWButton(cx - 70, ry + 96, 140, 20, Text.literal("Quitter"), HWButton.SECONDARY, 0, b -> this.client.scheduleStop()));

        int gap = 6, n = 5;
        int itemW = Math.min(122, (this.width - 24 - (n - 1) * gap) / n);
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
            b -> this.client.setScreen(new HWModsScreen(this))));
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

        // En-tete : le wordmark HEROES-WORLD est deja dans l'image de fond
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6◆ §eL'OLYMPE VOUS ATTEND §6◆"),
            cx, ry - 22, 0xFFE8C56A);

        // Puce compte (haut-droite)
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            int tw = this.textRenderer.getWidth(who);
            int pw = tw + 32, ph = 20, px = this.width - pw - 12, py = 10;
            ctx.fill(px, py, px + pw, py + ph, 0x55000000);
            boolean head = false;
            try {
                // vraie tete du skin du joueur (visage + calque chapeau)
                net.minecraft.util.Identifier skin = this.client.getSkinProvider()
                    .getSkinTextures(this.client.getGameProfile()).texture();
                ctx.drawTexture(skin, px + 4, py + 3, 14, 14, 8f, 8f, 8, 8, 64, 64);
                ctx.drawTexture(skin, px + 4, py + 3, 14, 14, 40f, 8f, 8, 8, 64, 64);
                head = true;
            } catch (Throwable ignored) {}
            if (!head) ctx.fill(px + 6, py + ph / 2 - 3, px + 12, py + ph / 2 + 3, 0xFF7CCB6E);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§f" + who), px + 22, py + 6, 0xFFFFFFFF);
        }

        // Version discrète (bas-gauche)
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§eHERO WORLD §7" + HWBrand.VERSION + " §8· Minecraft " + HWBrand.MC),
            8, this.height - 12, GOLD);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
