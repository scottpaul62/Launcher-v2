package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.text.Text;

/** Menu client HERO WORLD en jeu (Maj-droite) : thème, cosmétiques, HUD, options MC. */
public class HWClientSettings extends Screen {
    private final Screen parent;

    public HWClientSettings(Screen parent) { super(Text.literal("Paramètres du client")); this.parent = parent; }

    private void back() {
        if (parent != null) this.client.setScreen(parent);
        else if (this.client.world != null) this.client.setScreen(null); // en jeu -> retour au jeu
        else this.client.setScreen(new HWTitleScreen());
    }
    private void reopen() { this.client.setScreen(new HWClientSettings(parent)); }
    private static String onoff(boolean b) { return b ? "§aON" : "§cOFF"; }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = (int) (this.height * 0.24);
        this.addDrawableChild(new HWButton(cx - 150, y, 300, 26, Text.literal("Thème du menu"), HWButton.PRIMARY, 0,
            b -> this.client.setScreen(new HWThemeScreen(this))));
        this.addDrawableChild(new HWButton(cx - 150, y + 34, 300, 24, Text.literal("Cosmétiques (vestiaire)"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWCosmeticsScreen(this))));
        this.addDrawableChild(new HWButton(cx - 150, y + 66, 300, 22, Text.literal("Filigrane HERO WORLD : " + onoff(HWHud.watermark)),
            HWHud.watermark ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { HWHud.watermark = !HWHud.watermark; HWHud.save(); reopen(); }));
        this.addDrawableChild(new HWButton(cx - 150, y + 92, 300, 22, Text.literal("Compteur FPS : " + onoff(HWHud.fps)),
            HWHud.fps ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { HWHud.fps = !HWHud.fps; HWHud.save(); reopen(); }));
        this.addDrawableChild(new HWButton(cx - 150, y + 118, 300, 22, Text.literal("Coordonnées : " + onoff(HWHud.coords)),
            HWHud.coords ? HWButton.PRIMARY : HWButton.SECONDARY, 0, b -> { HWHud.coords = !HWHud.coords; HWHud.save(); reopen(); }));
        this.addDrawableChild(new HWButton(cx - 150, y + 150, 300, 22, Text.literal("Options Minecraft"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.addDrawableChild(new HWButton(cx - 70, y + 182, 140, 22, Text.literal("Retour"), HWButton.SECONDARY, 0, b -> back()));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        HWScene.draw(ctx, this.width, this.height);
        ctx.fill(0, 0, this.width, this.height, 0xB0000000);
        int cx = this.width / 2, top = (int) (this.height * 0.24);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§ePARAMÈTRES DU CLIENT"), cx, top - 34, 0xFFE8C56A);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("§7HERO WORLD " + HWBrand.VERSION + " · §8Maj-droite pour ouvrir ce menu en jeu"), cx, top - 18, 0xFFCFC9DA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public void close() { back(); }
    @Override public boolean shouldPause() { return false; }
}
