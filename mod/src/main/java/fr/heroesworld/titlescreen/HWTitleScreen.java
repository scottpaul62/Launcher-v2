package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Ecran titre HERO WORLD : image plein ecran, pile centrale bornee, dock adaptatif, puce compte cliquable. */
public class HWTitleScreen extends Screen {

    private static final int GOLD = 0xFFE8C56A;
    private int dockX, dockY, dockW, dockH;
    private int ry; // haut de la pile centrale
    private boolean compact;

    // puce compte + menu deroulant
    private boolean acctOpen = false;
    private int chipX, chipY, chipW, chipH;

    // skin reelle du compte (fetch asynchrone officiel, repli = skin par defaut)
    private static net.minecraft.util.Identifier hwSkin = null;
    private static boolean skinRequested = false;
    // taille minimale de la fenetre du jeu (une seule fois)
    private static boolean sizeLimitsSet = false;

    public HWTitleScreen() { super(Text.literal("HERO WORLD")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        compact = this.height < 360 || this.width < 720;

        // ---- dock adaptatif (compact en petite fenetre) ----
        int gap = 6, n = 5;
        dockH = compact ? 30 : 44;
        int itemW = Math.min(compact ? 88 : 122, (this.width - 24 - (n - 1) * gap) / n);
        dockW = n * itemW + (n - 1) * gap;
        dockX = cx - dockW / 2;
        dockY = this.height - (compact ? 42 : 74);

        // ---- pile centrale bornee : jamais de chevauchement avec le dock ----
        int stackH = 36 + 6 + 22 + 4 + 22 + 8 + 20;
        ry = Math.max(34, Math.min((int) (this.height * 0.42), dockY - stackH - 10));
        int joinW = Math.min(340, this.width - 60);
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry, joinW, 36, Text.literal("Rejoindre"), HWButton.PRIMARY, 0, b -> this.connect()));
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry + 42, joinW, 22, Text.literal("Solo"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWSoonScreen(this, "Solo", "Le mode Solo personnalise HERO WORLD arrivera bientot."))));
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry + 68, joinW, 22, Text.literal("Boutique"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWSoonScreen(this, "Boutique", "La boutique HERO WORLD arrivera bientot."))));
        this.addDrawableChild(new HWButton(cx - 70, ry + 96, 140, 20, Text.literal("Quitter"), HWButton.SECONDARY, 0, b -> this.client.scheduleStop()));

        this.addDrawableChild(new HWButton(dockX, dockY, itemW, dockH, Text.literal("Amis"), HWButton.DOCK, 1,
            b -> this.client.setScreen(new HWSoonScreen(this, "Amis", "La liste d'amis arrivera avec l'hebergement du serveur HEROES-WORLD."))));
        this.addDrawableChild(new HWButton(dockX + (itemW + gap), dockY, itemW, dockH, Text.literal("Thèmes"), HWButton.DOCK, 2,
            b -> this.client.setScreen(new HWThemeScreen(this))));
        this.addDrawableChild(new HWButton(dockX + 2 * (itemW + gap), dockY, itemW, dockH, Text.literal("Personnalisation"), HWButton.DOCK, 3,
            b -> this.client.setScreen(new HWCosmeticsScreen(this))));
        this.addDrawableChild(new HWButton(dockX + 3 * (itemW + gap), dockY, itemW, dockH, Text.literal("Options"), HWButton.DOCK, 4,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(dockX + 4 * (itemW + gap), dockY, itemW, dockH, Text.literal("Paramètres"), HWButton.DOCK, 5,
            b -> this.client.setScreen(new HWModsScreen(this))));

        // ---- puce compte (haut-droite) + menu deroulant ----
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        int tw = this.textRenderer.getWidth(who == null ? "" : who);
        chipW = tw + 38; chipH = 20; chipX = this.width - chipW - 12; chipY = 10;
        if (acctOpen && who != null && !who.isEmpty()) {
            int mw = 156, mx = this.width - mw - 12, my = chipY + chipH + 6;
            this.addDrawableChild(new HWButton(mx, my, mw, 20, Text.literal("Mon vestiaire"), HWButton.SECONDARY, 3,
                b -> this.client.setScreen(new HWCosmeticsScreen(this))));
            this.addDrawableChild(new HWButton(mx, my + 24, mw, 20, Text.literal("Parametres du client"), HWButton.SECONDARY, 5,
                b -> this.client.setScreen(new HWModsScreen(this))));
            this.addDrawableChild(new HWButton(mx, my + 48, mw, 20, Text.literal("Changer de compte"), HWButton.SECONDARY, 0,
                b -> this.client.setScreen(new HWSoonScreen(this, "Compte",
                    "Le changement de compte se fait dans le launcher HeroesWorld (puce compte, en haut a droite)."))));
        }
    }

    private void connect() {
        String target = HWConfig.address();
        ServerInfo info = new ServerInfo("HEROES-WORLD", target, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(target), info, false, null);
    }

    // Supprime le fond vanilla de Minecraft (panorama + flou).
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= chipX && mx <= chipX + chipW && my >= chipY && my <= chipY + chipH) {
            acctOpen = !acctOpen;
            this.clearAndInit();
            return true;
        }
        boolean handled = super.mouseClicked(mx, my, button);
        if (!handled && acctOpen) { acctOpen = false; this.clearAndInit(); return true; }
        return handled;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        try { this.client.getWindow().setTitle(HWBrand.WINDOW_TITLE); } catch (Throwable ignored) {}

        // taille minimale de la fenetre (confort : ~17 pouces / 1280x720)
        if (!sizeLimitsSet) {
            sizeLimitsSet = true;
            try {
                org.lwjgl.glfw.GLFW.glfwSetWindowSizeLimits(this.client.getWindow().getHandle(), 1280, 720,
                    org.lwjgl.glfw.GLFW.GLFW_DONT_CARE, org.lwjgl.glfw.GLFW.GLFW_DONT_CARE);
            } catch (Throwable ignored) {}
        }
        // vraie skin du compte (asynchrone, une seule requete)
        if (!skinRequested) {
            skinRequested = true;
            try {
                this.client.getSkinProvider().fetchSkinTextures(this.client.getGameProfile()).thenAccept(o -> {
                    try {
                        Object v = o;
                        if (v instanceof java.util.Optional<?> opt) v = opt.orElse(null);
                        if (v instanceof net.minecraft.client.util.SkinTextures st) hwSkin = st.texture();
                    } catch (Throwable ignored) {}
                });
            } catch (Throwable ignored) {}
        }

        HWScene.draw(ctx, this.width, this.height);

        int cx = this.width / 2;
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6◆ §eL'OLYMPE VOUS ATTEND §6◆"),
            cx, ry - 22, 0xFFE8C56A);

        // ---- puce compte (cliquable) ----
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            boolean over = acctOpen || (mouseX >= chipX && mouseX <= chipX + chipW && mouseY >= chipY && mouseY <= chipY + chipH);
            // bulle arrondie epuree (pas de traits dores)
            HWDraw.panel(ctx, chipX, chipY, chipW, chipH, 10, over ? 0x9A14121C : 0x66101318, over ? 0x55FFFFFF : 0x26FFFFFF);
            boolean head = false;
            try {
                net.minecraft.util.Identifier skin = hwSkin != null ? hwSkin
                    : this.client.getSkinProvider().getSkinTextures(this.client.getGameProfile()).texture();
                ctx.drawTexture(skin, chipX + 6, chipY + 3, 14, 14, 8f, 8f, 8, 8, 64, 64);
                ctx.drawTexture(skin, chipX + 6, chipY + 3, 14, 14, 40f, 8f, 8, 8, 64, 64);
                head = true;
            } catch (Throwable ignored) {}
            if (!head) ctx.fill(chipX + 8, chipY + chipH / 2 - 3, chipX + 14, chipY + chipH / 2 + 3, 0xFF7CCB6E);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§f" + who + (acctOpen ? " §7▴" : " §7▾")), chipX + 24, chipY + 6, 0xFFFFFFFF);
            if (acctOpen) {
                int mw = 156, mx = this.width - mw - 12, my = chipY + chipH + 6;
                HWDraw.panel(ctx, mx - 4, my - 4, mw + 8, 3 * 24 + 4, 6, 0xF2111318, 0xFF323844);
            }
        }

        // Version discrete (bas-gauche)
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§eHERO WORLD §7" + HWBrand.VERSION + " §8· Minecraft " + HWBrand.MC),
            8, this.height - 12, GOLD);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
