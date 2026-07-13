package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Vestiaire HERO WORLD : onglets Auras / Ailes. Décor assombri derrière. */
public class HWCosmeticsScreen extends Screen {
    private final Screen parent;
    private static int cat = 0; // 0 = auras, 1 = ailes

    public HWCosmeticsScreen(Screen parent) { super(Text.literal("Cosmétiques")); this.parent = parent; }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int tabY = (int) (this.height * 0.24);
        this.addDrawableChild(new HWButton(cx - 152, tabY, 148, 22, Text.literal((cat == 0 ? "§e" : "") + "Auras"),
            cat == 0 ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { cat = 0; this.client.setScreen(new HWCosmeticsScreen(parent)); }));
        this.addDrawableChild(new HWButton(cx + 4, tabY, 148, 22, Text.literal((cat == 1 ? "§e" : "") + "Ailes"),
            cat == 1 ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { cat = 1; this.client.setScreen(new HWCosmeticsScreen(parent)); }));

        String[] names = (cat == 0) ? HWCosmetics.AURA_NAMES : HWCosmetics.WING_NAMES;
        int cur = (cat == 0) ? HWCosmetics.aura : HWCosmetics.wings;
        int y = tabY + 34;
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            boolean eq = cur == i;
            String label = (eq ? "§a✔ " : "") + names[i];
            this.addDrawableChild(new HWButton(cx - 150, y + i * 26, 300, 22, Text.literal(label),
                eq ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> {
                    if (cat == 0) HWCosmetics.setAura(idx); else HWCosmetics.setWings(idx);
                    this.client.setScreen(new HWCosmeticsScreen(parent));
                }));
        }
        int by = y + names.length * 26 + 12;
        this.addDrawableChild(new HWButton(cx - 70, by, 140, 22, Text.literal("Retour"),
            HWButton.SECONDARY, 0, b -> this.client.setScreen(parent != null ? parent : new HWTitleScreen())));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        HWScene.draw(ctx, this.width, this.height);
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
        int cx = this.width / 2, top = (int) (this.height * 0.24);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eVESTIAIRE HERO WORLD"), cx, top - 30, 0xFFE8C56A);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Équipe tes cosmétiques — visibles en 3e personne (F5)."), cx, top - 16, 0xFFCFC9DA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public void close() { this.client.setScreen(parent != null ? parent : new HWTitleScreen()); }
    @Override public boolean shouldPause() { return false; }
}
