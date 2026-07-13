package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Vestiaire HERO WORLD (Étape 1 : sélection de l'aura). Décor assombri derrière. */
public class HWCosmeticsScreen extends Screen {
    private final Screen parent;

    public HWCosmeticsScreen(Screen parent) { super(Text.literal("Cosmétiques")); this.parent = parent; }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = (int) (this.height * 0.30);
        for (int i = 0; i < HWCosmetics.AURA_NAMES.length; i++) {
            final int idx = i;
            boolean eq = HWCosmetics.aura == i;
            String label = (eq ? "§a✔ " : "") + "Aura : " + HWCosmetics.AURA_NAMES[i];
            this.addDrawableChild(new HWButton(cx - 150, y + i * 28, 300, 24, Text.literal(label),
                eq ? HWButton.PRIMARY : HWButton.SECONDARY, 0,
                b -> { HWCosmetics.setAura(idx); this.client.setScreen(new HWCosmeticsScreen(parent)); }));
        }
        int by = y + HWCosmetics.AURA_NAMES.length * 28 + 14;
        this.addDrawableChild(new HWButton(cx - 70, by, 140, 22, Text.literal("Retour"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(parent != null ? parent : new HWTitleScreen())));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        HWScene.draw(ctx, this.width, this.height);
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
        int cx = this.width / 2, top = (int) (this.height * 0.30);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eVESTIAIRE — AURAS"), cx, top - 34, 0xFFE8C56A);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("§7Ton aura te suit en jeu (visible en 3e personne, F5). D'autres cosmétiques arrivent."),
            cx, top - 18, 0xFFCFC9DA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() { this.client.setScreen(parent != null ? parent : new HWTitleScreen()); }

    @Override public boolean shouldPause() { return false; }
}
