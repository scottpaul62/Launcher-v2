package fr.heroesworld.titlescreen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Panneau secondaire "à venir" : décor assombri derrière, panneau net devant. */
public class HWSoonScreen extends Screen {
    private final Screen parent;
    private final String heading, message;

    public HWSoonScreen(Screen parent, String heading, String message) {
        super(Text.literal(heading));
        this.parent = parent; this.heading = heading; this.message = message;
    }

    @Override
    protected void init() {
        this.addDrawableChild(new HWButton(this.width / 2 - 70, (int) (this.height * 0.62), 140, 24,
            Text.literal("Retour"), HWButton.SECONDARY, 0, b -> this.client.setScreen(parent != null ? parent : new HWTitleScreen())));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        HWScene.draw(ctx, this.width, this.height);
        ctx.fill(0, 0, this.width, this.height, 0xA6000000); // assombrissement du décor
        int cx = this.width / 2, cy = this.height / 2;
        ctx.fillGradient(cx - 220, cy - 90, cx + 220, cy + 70, 0xE60A0C16, 0xE6070810);
        ctx.fill(cx - 220, cy - 90, cx + 220, cy - 89, 0x66E8C56A);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§e" + heading), cx, cy - 74, 0xFFE8C56A);
        int mw = this.textRenderer.getWidth(message);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§7" + message), cx, cy - 30, 0xFFCFC9DA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override public boolean shouldPause() { return false; }
}
