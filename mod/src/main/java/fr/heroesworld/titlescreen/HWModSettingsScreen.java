package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Reglages d'un widget HUD : activer, verrouiller, echelle, deplacer, reinitialiser. */
public class HWModSettingsScreen extends Screen {
    private final Screen parent;
    private final HWHudManager.El el;

    public HWModSettingsScreen(Screen parent, HWHudManager.El el) {
        super(Text.literal("Reglages du widget")); this.parent = parent; this.el = el;
    }
    private void reopen() { this.client.setScreen(new HWModSettingsScreen(parent, el)); }

    @Override
    protected void init() {
        int cx = this.width / 2, y = (int) (this.height * 0.30);
        this.addDrawableChild(new HWButton(cx - 150, y, 300, 22, Text.literal("Active : " + (el.enabled ? "§aON" : "§8OFF")), HWButton.SECONDARY, 0,
            b -> { el.enabled = !el.enabled; HWHudManager.save(); reopen(); }));
        this.addDrawableChild(new HWButton(cx - 150, y + 26, 300, 22, Text.literal("Verrouille : " + (el.locked ? "§aOUI" : "§8NON")), HWButton.SECONDARY, 0,
            b -> { el.locked = !el.locked; HWHudManager.save(); reopen(); }));
        this.addDrawableChild(new HWButton(cx - 150, y + 52, 92, 22, Text.literal("Echelle -"), HWButton.SECONDARY, 0,
            b -> { el.scale = HWHudManager.clampScale(el.scale - 0.1f); HWHudManager.save(); reopen(); }));
        this.addDrawableChild(new HWButton(cx - 54, y + 52, 108, 22, Text.literal("§f" + Math.round(el.scale * 100) + "%"), HWButton.SECONDARY, 0, b -> {}));
        this.addDrawableChild(new HWButton(cx + 58, y + 52, 92, 22, Text.literal("Echelle +"), HWButton.SECONDARY, 0,
            b -> { el.scale = HWHudManager.clampScale(el.scale + 0.1f); HWHudManager.save(); reopen(); }));
        this.addDrawableChild(new HWButton(cx - 150, y + 78, 300, 22, Text.literal("Deplacer dans l'editeur de HUD"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWHudEditScreen(parent))));
        this.addDrawableChild(new HWButton(cx - 150, y + 104, 300, 22, Text.literal("Reinitialiser (position + echelle)"), HWButton.SECONDARY, 0,
            b -> { HWHudManager.resetPos(el); HWHudManager.save(); reopen(); }));
        this.addDrawableChild(new HWButton(cx - 70, y + 134, 140, 22, Text.literal("Retour"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(parent)));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) super.renderBackground(ctx, mouseX, mouseY, delta);
        else { HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0xB0000000); }
        int y = (int) (this.height * 0.30);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§e" + el.name), this.width / 2, y - 26, 0xFFE8C56A);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§8Categorie : " + el.category + "   Ancrage : " + HWHudManager.anchorName(el.anchor)), this.width / 2, y - 14, 0xFF888888);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public void close() { this.client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }
}
