package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Editeur de HUD : glisser-deposer chaque element, sauvegarde des positions. */
public class HWHudEditScreen extends Screen {
    private final Screen parent;
    private HWHudManager.El dragging;
    private int offX, offY;

    public HWHudEditScreen(Screen parent) { super(Text.literal("Editeur de HUD")); this.parent = parent; }

    @Override
    protected void init() {
        this.addDrawableChild(new HWButton(this.width / 2 - 70, this.height - 28, 140, 22, Text.literal("Termine"), HWButton.PRIMARY, 0,
            b -> done()));
        this.addDrawableChild(new HWButton(this.width / 2 - 230, this.height - 28, 150, 22, Text.literal("Tout reinitialiser"), HWButton.SECONDARY, 0,
            b -> { HWHudManager.resetAll(); HWHudManager.save(); }));
    }

    private void done() {
        HWHudManager.save();
        this.client.setScreen(parent != null ? parent : new HWModsScreen(null));
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = this.client;
        if (mc.world != null) ctx.fill(0, 0, this.width, this.height, 0x60000000);
        else { HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0x90000000); }

        for (int gx = 0; gx < this.width; gx += 20) ctx.fill(gx, 0, gx + 1, this.height, 0x10FFFFFF);
        for (int gy = 0; gy < this.height; gy += 20) ctx.fill(0, gy, this.width, gy + 1, 0x10FFFFFF);

        for (HWHudManager.El e : HWHudManager.ELEMENTS) {
            if (!e.enabled) continue;
            int w = HWHudManager.width(mc, e), h = HWHudManager.height(e);
            boolean sel = (e == dragging) || (mouseX >= e.x - 2 && mouseX <= e.x + w + 2 && mouseY >= e.y - 2 && mouseY <= e.y + h + 2);
            ctx.fill(e.x - 2, e.y - 2, e.x + w + 2, e.y + h + 2, sel ? 0x40E8C56A : 0x20FFFFFF);
            drawBorder(ctx, e.x - 2, e.y - 2, w + 4, h + 4, sel ? 0xFFE8C56A : 0x66FFFFFF);
            try { HWHudManager.renderOne(ctx, mc, e); } catch (Throwable ignored) {}
            if (sel) {
                int ly = (e.y - 11 < 0) ? e.y + h + 3 : e.y - 11;
                ctx.drawTextWithShadow(this.textRenderer, Text.literal("§e" + e.name + " §7(" + e.x + "," + e.y + ")"), e.x, ly, 0xFFFFFFFF);
            }
        }
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eEditeur de HUD §7- glisse les elements, Echap pour sauver"), this.width / 2, 8, 0xFFE8C56A);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0) {
            for (int i = HWHudManager.ELEMENTS.size() - 1; i >= 0; i--) {
                HWHudManager.El e = HWHudManager.ELEMENTS.get(i);
                if (!e.enabled) continue;
                int w = HWHudManager.width(this.client, e), h = HWHudManager.height(e);
                if (mx >= e.x - 2 && mx <= e.x + w + 2 && my >= e.y - 2 && my <= e.y + h + 2) {
                    dragging = e; offX = (int) mx - e.x; offY = (int) my - e.y; return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            int w = HWHudManager.width(this.client, dragging), h = HWHudManager.height(dragging);
            int nx = Math.max(0, Math.min(this.width - w, (int) mx - offX));
            int ny = Math.max(0, Math.min(this.height - h, (int) my - offY));
            dragging.x = nx; dragging.y = ny;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; HWHudManager.save(); return true; }
        return super.mouseReleased(mx, my, button);
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x, y, x + w, y + 1, col);
        ctx.fill(x, y + h - 1, x + w, y + h, col);
        ctx.fill(x, y, x + 1, y + h, col);
        ctx.fill(x + w - 1, y, x + w, y + h, col);
    }

    @Override public void close() { done(); }
    @Override public boolean shouldPause() { return false; }
}
