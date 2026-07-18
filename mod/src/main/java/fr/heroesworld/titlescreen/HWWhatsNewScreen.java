package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** « Quoi de neuf » : affiche les nouveautes de la version, une seule fois. */
public class HWWhatsNewScreen extends Screen {
    private final Screen parent;
    private static final String[] NEWS = {
        "§6◆ §fInterface refaite : fonds en images lisses partout, nouvelles icones",
        "§6◆ §fCosmetiques 3D rendus EN JEU sur ton personnage (F5)",
        "§6◆ §fContenu dynamique : astuces et cosmetiques mis a jour sans reinstaller",
        "§6◆ §fFond du menu plus net en grand ecran",
        "§6◆ §fBouton Multijoueur : serveur ★ HEROES-WORLD epingle en tete",
        "§6◆ §fZoom (C), Toggle Sprint/Sneak, HUD deplaçable, waypoints",
    };

    public HWWhatsNewScreen(Screen parent) { super(Text.literal("Quoi de neuf")); this.parent = parent; }

    @Override
    protected void init() {
        int ph = 120 + NEWS.length * 14;
        int py = (this.height - ph) / 2;
        this.addDrawableChild(new HWButton(this.width / 2 - 80, py + ph - 34, 160, 22, Text.literal("C'est parti !"),
            HWButton.PRIMARY, 0, b -> this.client.setScreen(parent)));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) super.renderBackground(ctx, mouseX, mouseY, delta);
        else { if (!HWBg2.draw(ctx, this.width, this.height)) HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0x70000000); }
        int pw = Math.min(420, this.width - 40), ph = 120 + NEWS.length * 14;
        int px = (this.width - pw) / 2, py = (this.height - ph) / 2;
        HWDraw.panel(ctx, px, py, pw, ph, 8, 0xF2111318, 0xFF323844);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6QUOI DE NEUF §8— §eHERO WORLD " + HWBrand.VERSION),
            this.width / 2, py + 14, 0xFFE8C56A);
        ctx.fill(px + 14, py + 30, px + pw - 14, py + 31, 0x33D6B85B);
        for (int i = 0; i < NEWS.length; i++) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(NEWS[i]), px + 20, py + 42 + i * 14, 0xFFF4F5F7);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public void close() { this.client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
