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
    // assets premium (PNG fournis) : cadre central + dock a logements
    private boolean dockAsset, frameOn;
    private int frameX, frameY, frameW, frameH;

    // puce compte + menu deroulant
    private boolean acctOpen = false;
    private int chipX, chipY, chipW, chipH;

    // taille minimale de la fenetre du jeu (une seule fois)
    private static boolean sizeLimitsSet = false;
    // easter egg Konami
    private static final int[] KONAMI = {265, 265, 264, 264, 263, 262, 263, 262, 66, 65};
    private int konamiIdx = 0;

    public HWTitleScreen() { super(Text.literal("HERO WORLD")); }

    @Override
    protected void init() {
        // « Quoi de neuf » : une fois par version
        if (!HWBrand.VERSION.equals(HWClientConfig.lastSeen)) {
            HWClientConfig.lastSeen = HWBrand.VERSION;
            HWClientConfig.save();
            if (this.client != null && this.client.world == null) {
                this.client.setScreen(new HWWhatsNewScreen(this));
                return;
            }
        }
        int cx = this.width / 2;
        compact = this.height < 360 || this.width < 720;

        // ---- dock : image premium (5 logements) si disponible, sinon cluster d'icones ----
        int n = 5;
        int[] dd = HWTex.dims("dock");
        dockAsset = dd != null;
        int[] bx = new int[n];
        int bw2, bh2, by2;
        if (dockAsset) {
            dockW = compact ? 216 : 262;
            dockH = Math.round(dockW * dd[1] / (float) dd[0]);
            dockX = cx - dockW / 2;
            dockY = this.height - dockH - (compact ? 6 : 10);
            bw2 = bh2 = Math.round(dockH * 0.55f);
            by2 = Math.round(dockY + dockH * 0.648f - bh2 / 2f);
            float[] fx = {0.127f, 0.308f, 0.499f, 0.690f, 0.873f}; // centres MESURES des 5 logements
            for (int i = 0; i < n; i++) bx[i] = Math.round(dockX + fx[i] * dockW - bw2 / 2f);
        } else {
            int gap = compact ? 8 : 14;
            dockH = compact ? 26 : 38;
            bw2 = compact ? 34 : 46; bh2 = dockH;
            dockW = n * bw2 + (n - 1) * gap;
            dockX = cx - dockW / 2;
            dockY = this.height - (compact ? 40 : 62);
            by2 = dockY;
            for (int i = 0; i < n; i++) bx[i] = dockX + i * (bw2 + gap);
        }

        // ---- pile centrale : logee dans le cadre premium si disponible ----
        int stackH = 36 + 6 + 22 + 4 + 22 + 4 + 22 + 8 + 20;
        int joinW = Math.min(340, this.width - 60);
        frameOn = false; // cadre central retire a la demande de Scott (18/07) — boutons libres
        ry = Math.max(30, Math.min((int) (this.height * 0.40), dockY - stackH - 10));
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry, joinW, 36, Text.literal("Rejoindre"), HWButton.PRIMARY, 0, b -> this.connect()));
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry + 42, joinW, 22, Text.literal("Multijoueur"), HWButton.SECONDARY, 0,
            b -> this.openMultiplayer()));
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry + 68, joinW, 22, Text.literal("Solo"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWSoonScreen(this, "Solo", "Le mode Solo personnalise HERO WORLD arrivera bientot."))));
        this.addDrawableChild(new HWButton(cx - joinW / 2, ry + 94, joinW, 22, Text.literal("Boutique"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWSoonScreen(this, "Boutique", "La boutique HERO WORLD arrivera bientot."))));
        this.addDrawableChild(new HWButton(cx - 70, ry + 122, 140, 20, Text.literal("Quitter"), HWButton.SECONDARY, 0, b -> this.client.scheduleStop()));

        this.addDrawableChild(new HWButton(bx[0], by2, bw2, bh2, Text.literal("Amis"), HWButton.DOCK, 1,
            b -> this.client.setScreen(new HWSoonScreen(this, "Amis", "La liste d'amis arrivera avec l'hebergement du serveur HEROES-WORLD."))));
        this.addDrawableChild(new HWButton(bx[1], by2, bw2, bh2, Text.literal("Thèmes"), HWButton.DOCK, 2,
            b -> this.client.setScreen(new HWThemeScreen(this))));
        this.addDrawableChild(new HWButton(bx[2], by2, bw2, bh2, Text.literal("Personnalisation"), HWButton.DOCK, 3,
            b -> this.client.setScreen(new HWCosmeticsScreen(this))));
        this.addDrawableChild(new HWButton(bx[3], by2, bw2, bh2, Text.literal("Options"), HWButton.DOCK, 4,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(bx[4], by2, bw2, bh2, Text.literal("Paramètres"), HWButton.DOCK, 5,
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

    /** Ouvre l'ecran Multijoueur vanilla avec le serveur HEROES-WORLD epingle en tete de liste. */
    private void openMultiplayer() {
        try {
            net.minecraft.client.option.ServerList list = new net.minecraft.client.option.ServerList(this.client);
            list.loadFile();
            String addr = HWConfig.address();
            ServerInfo existing = null;
            for (int i = 0; i < list.size(); i++) {
                if (addr.equals(list.get(i).address)) { existing = list.get(i); break; }
            }
            ServerInfo pin = existing != null ? existing : new ServerInfo("\u2605 HEROES-WORLD", addr, ServerInfo.ServerType.OTHER);
            if (existing != null) list.remove(existing);
            list.add(pin, false);
            for (int i = list.size() - 1; i > 0; i--) list.swapEntries(i, i - 1); // remonte en position 1
            list.saveFile();
        } catch (Throwable t) { HWHudManager.LOG.warn("[TITRE] epinglage serveur impossible", t); }
        this.client.setScreen(new net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen(this));
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
        HWScene.draw(ctx, this.width, this.height);
        HWFx.draw(ctx, this.width, this.height);            // effets vivants discrets (braises, etoile filante)
        HWServerStatus.tick();                               // ping serveur asynchrone

        int cx = this.width / 2;

        // fond du dock : image premium (5 logements) sinon pilule translucide
        if (dockAsset) HWTex.drawFit(ctx, "dock", dockX + dockW / 2, dockY + dockH / 2, dockH);
        else HWDraw.panel(ctx, dockX - 12, dockY - 6, dockW + 24, dockH + 12, compact ? 10 : 14, 0x7A0E1118, 0x2CFFFFFF);

        // panneau serveur en direct (haut-gauche)
        {
            String txt = HWServerStatus.online
                ? "§a● §f" + HWServerStatus.playersOn + "§7/" + HWServerStatus.playersMax + " en ligne"
                : "§c● §7hors ligne";
            int tw2 = this.textRenderer.getWidth(txt.replaceAll("§.", "")) + 40;
            if (!HWTex.drawH3(ctx, "status", 10, 10, tw2, 20, 0))
                HWDraw.panel(ctx, 10, 10, tw2, 20, 10, 0x66101318, 0x26FFFFFF);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§6HW"), 18, 16, 0xFFE8C56A);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(txt), 34, 16, 0xFFFFFFFF);
        }

        // astuce rotative : seulement s'il y a VRAIMENT la place (jamais par-dessus les boutons)
        {
            int stackBottom = frameOn ? frameY + frameH : ry + 148;
            int tipY = dockY - 42;
            if (tipY > stackBottom + 4) {
                String tip = HWTips.current();
                int tw2 = this.textRenderer.getWidth(tip);
                if (!HWTex.drawH3(ctx, "tooltip", cx - tw2 / 2 - 10, tipY, tw2 + 20, 16, 0))
                    HWDraw.panel(ctx, cx - tw2 / 2 - 10, tipY, tw2 + 20, 16, 8, 0x8A101318, 0x22FFFFFF);
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7" + tip), cx, tipY + 4, 0xFFB9C2CC);
            }
        }
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6◆ §eL'OLYMPE VOUS ATTEND §6◆"),
            cx, frameOn ? frameY - 12 : ry - 22, 0xFFE8C56A);

        // ---- puce compte (cliquable) ----
        String who = (this.client.getSession() != null) ? this.client.getSession().getUsername() : "";
        if (who != null && !who.isEmpty()) {
            boolean over = acctOpen || (mouseX >= chipX && mouseX <= chipX + chipW && mouseY >= chipY && mouseY <= chipY + chipH);
            // bulle arrondie epuree (pas de traits dores)
            if (HWTex.drawH3(ctx, "chip", chipX, chipY, chipW, chipH, 0)) {
                if (over) HWDraw.roundRect(ctx, chipX + 2, chipY + 2, chipW - 4, chipH - 4, 8, 0x14FFFFFF);
            } else HWDraw.panel(ctx, chipX, chipY, chipW, chipH, 10, over ? 0x9A14121C : 0x66101318, over ? 0x55FFFFFF : 0x26FFFFFF);
            boolean head = false;
            try {
                net.minecraft.util.Identifier skin = HWSkin.texture(this.client);
                ctx.drawTexture(skin, chipX + 6, chipY + 3, 14, 14, 8f, 8f, 8, 8, 64, 64);
                ctx.drawTexture(skin, chipX + 6, chipY + 3, 14, 14, 40f, 8f, 8, 8, 64, 64);
                ctx.fill(chipX + 17, chipY + 13, chipX + 21, chipY + 17, 0xFF0B0F14); // liseret
                ctx.fill(chipX + 18, chipY + 14, chipX + 20, chipY + 16, 0xFF48D36B); // en ligne
                head = true;
            } catch (Throwable ignored) {}
            if (!head) ctx.fill(chipX + 8, chipY + chipH / 2 - 3, chipX + 14, chipY + chipH / 2 + 3, 0xFF7CCB6E);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§f" + who + (acctOpen ? " §7▴" : " §7▾")), chipX + 24, chipY + 6, 0xFFFFFFFF);
            if (acctOpen) {
                int mw = 156, mx = this.width - mw - 12, my = chipY + chipH + 6;
                if (!HWTex.draw9n(ctx, "dropdown", mx - 8, my - 8, mw + 16, 3 * 24 + 12, 130))
                    HWDraw.panel(ctx, mx - 8, my - 8, mw + 16, 3 * 24 + 12, 6, 0xF2111318, 0xFF323844);
            }
        }

        // Resolution reelle + version (bas-gauche)
        try {
            int fw = this.client.getWindow().getFramebufferWidth(), fh = this.client.getWindow().getFramebufferHeight();
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("§8" + fw + " × " + fh), 8, this.height - 24, 0xFF6E7480);
        } catch (Throwable ignored) {}
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§eHERO WORLD §7" + HWBrand.VERSION + " §8· Minecraft " + HWBrand.MC),
            8, this.height - 12, GOLD);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Konami : fleches fleches B A -> tempete d'eclairs
        if (keyCode == KONAMI[konamiIdx]) {
            konamiIdx++;
            if (konamiIdx >= KONAMI.length) { konamiIdx = 0; HWFx.storm(); }
        } else konamiIdx = (keyCode == KONAMI[0]) ? 1 : 0;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
