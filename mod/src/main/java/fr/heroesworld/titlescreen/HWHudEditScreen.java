package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Editeur de HUD : glisser-deposer chaque element, re-ancrage au coin le plus proche, sauvegarde. */
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

    private void setPos(HWHudManager.El e, int nx, int ny, int w, int h) {
        int sw = this.width, sh = this.height;
        nx = Math.max(0, Math.min(sw - w, nx));
        ny = Math.max(0, Math.min(sh - h, ny));
        if (e.anchor == 1 || e.anchor == 3) e.ox = sw - w - nx;
        else if (e.anchor == 4 || e.anchor == 5) e.ox = nx - (sw - w) / 2;
        else e.ox = nx;
        if (e.anchor == 2 || e.anchor == 3 || e.anchor == 5) e.oy = sh - h - ny;
        else e.oy = ny;
        if (e.anchor != 4 && e.anchor != 5 && e.ox < 0) e.ox = 0;
        if (e.oy < 0) e.oy = 0;
    }

    private void reanchor(HWHudManager.El e, int w, int h) {
        int ax = HWHudManager.actualX(e, this.width, w), ay = HWHudManager.actualY(e, this.height, h);
        int cx = ax + w / 2, cy = ay + h / 2;
        boolean bottom = cy > this.height / 2;
        int col = cx < this.width / 3 ? 0 : (cx < 2 * this.width / 3 ? 1 : 2);
        if (col == 1) e.anchor = bottom ? 5 : 4;
        else if (col == 2) e.anchor = bottom ? 3 : 1;
        else e.anchor = bottom ? 2 : 0;
        setPos(e, ax, ay, w, h);
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
            int ax = HWHudManager.actualX(e, this.width, w), ay = HWHudManager.actualY(e, this.height, h);
            boolean sel = (e == dragging) || (mouseX >= ax - 2 && mouseX <= ax + w + 2 && mouseY >= ay - 2 && mouseY <= ay + h + 2);
            ctx.fill(ax - 2, ay - 2, ax + w + 2, ay + h + 2, sel ? 0x40E8C56A : 0x20FFFFFF);
            drawBorder(ctx, ax - 2, ay - 2, w + 4, h + 4, sel ? 0xFFE8C56A : 0x66FFFFFF);
            try { HWHudManager.renderAt(ctx, mc, e, ax, ay); } catch (Throwable ignored) {}
            if (sel) {
                int ly = (ay - 11 < 0) ? ay + h + 3 : ay - 11;
                ctx.drawTextWithShadow(this.textRenderer, Text.literal("§e" + e.name + " §7[" + HWHudManager.anchorName(e.anchor) + "]"), ax, ly, 0xFFFFFFFF);
            }
        }
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§eEditeur de HUD §7- glisse les elements, ils s'ancrent au coin le plus proche - Echap pour sauver"), this.width / 2, 8, 0xFFE8C56A);
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
                int ax = HWHudManager.actualX(e, this.width, w), ay = HWHudManager.actualY(e, this.height, h);
                if (mx >= ax - 2 && mx <= ax + w + 2 && my >= ay - 2 && my <= ay + h + 2) {
                    dragging = e; offX = (int) mx - ax; offY = (int) my - ay; return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            int w = HWHudManager.width(this.client, dragging), h = HWHudManager.height(dragging);
            setPos(dragging, (int) mx - offX, (int) my - offY, w, h);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            int w = HWHudManager.width(this.client, dragging), h = HWHudManager.height(dragging);
            reanchor(dragging, w, h);
            dragging = null; HWHudManager.save(); return true;
        }
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
