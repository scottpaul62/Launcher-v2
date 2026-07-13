package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;

/** Hub des réglages du client HERO WORLD (thème, cosmétiques, options Minecraft). */
public class HWClientSettings extends Screen {
    private final Screen parent;

    public HWClientSettings(Screen parent) { super(Text.literal("Paramètres du client")); this.parent = parent; }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = (int) (this.height * 0.34);
        this.addDrawableChild(new HWButton(cx - 150, y, 300, 26, Text.literal("Thème du menu"), HWButton.PRIMARY, 0,
            b -> this.client.setScreen(new HWThemeScreen(this))));
        this.addDrawableChild(new HWButton(cx - 150, y + 34, 300, 24, Text.literal("Cosmétiques (vestiaire)"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWCosmeticsScreen(this))));
        this.addDrawableChild(new HWButton(cx - 150, y + 62, 300, 24, Text.literal("Options Minecraft"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(cx - 70, y + 100, 140, 22, Text.literal("Retour"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(parent != null ? parent : new HWTitleScreen())));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        HWScene.draw(ctx, this.width, this.height);
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
        int cx = this.width / 2, top = (int) (this.height * 0.34);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§ePARAMÈTRES DU CLIENT"), cx, top - 34, 0xFFE8C56A);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7Réglages du client HERO WORLD " + HWBrand.VERSION), cx, top - 18, 0xFFCFC9DA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public void close() { this.client.setScreen(parent != null ? parent : new HWTitleScreen()); }
    @Override public boolean shouldPause() { return false; }
}
