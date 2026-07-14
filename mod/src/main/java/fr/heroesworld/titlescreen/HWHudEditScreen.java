package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;

/** Editeur HUD Heroes-World : selection, deplacement sans saut, redimensionnement (echelle),
 *  zone sure + hotbar, guides cyan + aimantation, clavier, annuler. */
public class HWHudEditScreen extends Screen {
    private final Screen parent;
    private HWHudManager.El selected;
    private int mode = 0;               // 0 rien, 1 deplacement, 2 redimensionnement
    private int offX, offY;
    private float startScale, startDist;
    private int guideX = Integer.MIN_VALUE, guideY = Integer.MIN_VALUE;
    private final Deque<float[]> undo = new ArrayDeque<>();

    public HWHudEditScreen(Screen parent) { super(Text.literal("Editeur HUD")); this.parent = parent; }

    @Override
    protected void init() {
        HWHudManager.editorPreview = true; // les widgets sans donnees affichent un apercu (hitbox coherente)
        int bw = 82, g = 4, by = this.height - 22;
        int total = 4 * bw + 3 * g, sx = this.width / 2 - total / 2;
        this.addDrawableChild(new HWButton(sx, by, bw, 16, Text.literal("Annuler"), HWButton.SECONDARY, 0, b -> undo()));
        this.addDrawableChild(new HWButton(sx + (bw + g), by, bw, 16, Text.literal("Reinit. widget"), HWButton.SECONDARY, 0,
            b -> { if (selected != null) { pushUndo(selected); HWHudManager.resetPos(selected); HWHudManager.save(); } }));
        this.addDrawableChild(new HWButton(sx + 2 * (bw + g), by, bw, 16, Text.literal("Reinit. page"), HWButton.SECONDARY, 0,
            b -> { HWHudManager.resetAll(); HWHudManager.save(); }));
        this.addDrawableChild(new HWButton(sx + 3 * (bw + g), by, bw, 16, Text.literal("Termine"), HWButton.PRIMARY, 0, b -> done()));
    }

    private void done() { HWHudManager.save(); this.client.setScreen(parent != null ? parent : new HWModsScreen(null)); }

    private MinecraftClient mc() { return this.client; }
    private int W(HWHudManager.El e) { return HWHudManager.scaledW(mc(), e); }
    private int H(HWHudManager.El e) { return HWHudManager.scaledH(e); }
    private int AX(HWHudManager.El e) { return HWHudManager.actualX(e, this.width, W(e)); }
    private int AY(HWHudManager.El e) { return HWHudManager.actualY(e, this.height, H(e)); }

    private void pushUndo(HWHudManager.El e) {
        undo.addLast(new float[]{HWHudManager.ELEMENTS.indexOf(e), e.anchor, e.ox, e.oy, e.scale, e.enabled ? 1 : 0, e.locked ? 1 : 0});
        while (undo.size() > 20) undo.pollFirst();
    }
    private void undo() {
        if (undo.isEmpty()) return;
        float[] s = undo.pollLast();
        HWHudManager.El e = HWHudManager.ELEMENTS.get((int) s[0]);
        e.anchor = (int) s[1]; e.ox = s[2]; e.oy = s[3]; e.scale = s[4]; e.enabled = s[5] > 0.5f; e.locked = s[6] > 0.5f;
        HWHudManager.save();
    }

    // --- aimantation : renvoie la position collee + memorise le guide ---
    private int[] snap(int nx, int ny, int w, int h) {
        guideX = Integer.MIN_VALUE; guideY = Integer.MIN_VALUE;
        if (Screen.hasAltDown()) return new int[]{nx, ny};
        int sw = this.width, sh = this.height, T = HWHudManager.SNAP;
        // candidats X : {nx cible, position du guide}
        java.util.List<int[]> xc = new java.util.ArrayList<>();
        xc.add(new int[]{HWHudManager.SAFE, HWHudManager.SAFE});
        xc.add(new int[]{sw - w - HWHudManager.SAFE, sw - HWHudManager.SAFE});
        xc.add(new int[]{(sw - w) / 2, sw / 2});
        java.util.List<int[]> yc = new java.util.ArrayList<>();
        yc.add(new int[]{HWHudManager.SAFE, HWHudManager.SAFE});
        yc.add(new int[]{sh - h - HWHudManager.SAFE, sh - HWHudManager.SAFE});
        yc.add(new int[]{(sh - h) / 2, sh / 2});
        int hotbarTop = sh - 39;
        yc.add(new int[]{hotbarTop - h, hotbarTop});
        for (HWHudManager.El o : HWHudManager.ELEMENTS) {
            if (o == selected || !o.enabled) continue;
            int ow = W(o), oh = H(o), oax = AX(o), oay = AY(o);
            xc.add(new int[]{oax, oax}); xc.add(new int[]{oax + ow - w, oax + ow}); xc.add(new int[]{oax + ow / 2 - w / 2, oax + ow / 2});
            yc.add(new int[]{oay, oay}); yc.add(new int[]{oay + oh - h, oay + oh}); yc.add(new int[]{oay + oh / 2 - h / 2, oay + oh / 2});
        }
        int bestX = nx, bd = T + 1;
        for (int[] c : xc) { int d = Math.abs(nx - c[0]); if (d < bd) { bd = d; bestX = c[0]; guideX = c[1]; } }
        if (bd > T) { bestX = nx; guideX = Integer.MIN_VALUE; }
        int bestY = ny; bd = T + 1;
        for (int[] c : yc) { int d = Math.abs(ny - c[0]); if (d < bd) { bd = d; bestY = c[0]; guideY = c[1]; } }
        if (bd > T) { bestY = ny; guideY = Integer.MIN_VALUE; }
        return new int[]{bestX, bestY};
    }

    private void setPos(HWHudManager.El e, int nx, int ny, int w, int h) {
        int sw = this.width, sh = this.height;
        nx = Math.max(HWHudManager.SAFE, Math.min(Math.max(HWHudManager.SAFE, sw - w - HWHudManager.SAFE), nx));
        ny = Math.max(HWHudManager.SAFE, Math.min(Math.max(HWHudManager.SAFE, sh - h - HWHudManager.SAFE), ny));
        e.ox = (nx - HWHudManager.baseX(e.anchor, sw, w)) / (float) sw;
        e.oy = (ny - HWHudManager.baseY(e.anchor, sh, h)) / (float) sh;
    }
    private void reanchor(HWHudManager.El e, int w, int h) {
        int ax = AX(e), ay = AY(e), cx = ax + w / 2, cy = ay + h / 2;
        int col = cx < this.width / 3 ? 0 : (cx < 2 * this.width / 3 ? 1 : 2);
        int row = cy < this.height / 3 ? 0 : (cy < 2 * this.height / 3 ? 1 : 2);
        e.anchor = row * 3 + col;
        setPos(e, ax, ay, w, h);
    }


    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = this.client;
        if (mc.world != null) ctx.fill(0, 0, this.width, this.height, 0x66000000); // voile sombre leger et uniforme
        else { HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0x90000000); }

        // zone sure + zone hotbar
        drawBorder(ctx, HWHudManager.SAFE, HWHudManager.SAFE, this.width - 2 * HWHudManager.SAFE, this.height - 2 * HWHudManager.SAFE, 0x2249BDF2);
        int hbW = 186, hbX = this.width / 2 - hbW / 2, hbY = this.height - 40;
        ctx.fill(hbX, hbY, hbX + hbW, this.height, 0x22E45B68);

        // widgets (a l'echelle)
        for (HWHudManager.El e : HWHudManager.ELEMENTS) {
            if (!e.enabled || e.id.equals("chat")) continue;
            int w = W(e), h = H(e), ax = AX(e), ay = AY(e);
            MatrixStack ms = ctx.getMatrices();
            ms.push(); ms.translate((double) ax, (double) ay, 0.0); ms.scale(e.scale, e.scale, 1.0f);
            try { HWHudManager.renderAt(ctx, mc, e, 0, 0); } catch (Throwable ignored) {}
            ms.pop();
            boolean hover = mouseX >= ax && mouseX <= ax + w && mouseY >= ay && mouseY <= ay + h;
            if (e == selected) {
                drawBorder(ctx, ax - 1, ay - 1, w + 2, h + 2, HWHudManager.SEL);            // contour cyan ajuste
                ctx.fill(ax + w - 4, ay + h - 4, ax + w + 2, ay + h + 2, HWHudManager.SEL);  // poignee de taille
            } else if (hover) {
                drawBorder(ctx, ax - 1, ay - 1, w + 2, h + 2, 0x66FFFFFF);
            }
        }

        // guides d'aimantation (pendant le geste)
        if (mode == 1) {
            if (guideX != Integer.MIN_VALUE) ctx.fill(guideX, 0, guideX + 1, this.height, HWHudManager.GUIDE);
            if (guideY != Integer.MIN_VALUE) ctx.fill(0, guideY, this.width, guideY + 1, HWHudManager.GUIDE);
        }
        // cadre de la fenetre + aide
        drawBorder(ctx, 0, 0, this.width, this.height, 0x5549BDF2);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal("§7glisser: deplacer · poignee: taille · fleches 1px / Maj 10px · Alt: sans aimant · Echap: sortir"),
            this.width / 2, 7, 0xFFB9C2CC);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        // poignee de redimensionnement du widget selectionne
        if (selected != null) {
            int w = W(selected), h = H(selected), ax = AX(selected), ay = AY(selected);
            if (!selected.locked && mx >= ax + w - 4 && mx <= ax + w + 2 && my >= ay + h - 4 && my <= ay + h + 2) {
                mode = 2; pushUndo(selected); startScale = selected.scale;
                startDist = (float) Math.hypot(mx - ax, my - ay);
                return true;
            }
        }
        // selection du widget sous le curseur (le plus haut)
        for (int i = HWHudManager.ELEMENTS.size() - 1; i >= 0; i--) {
            HWHudManager.El e = HWHudManager.ELEMENTS.get(i);
            if (!e.enabled || e.id.equals("chat")) continue;
            int w = W(e), h = H(e), ax = AX(e), ay = AY(e);
            if (mx >= ax && mx <= ax + w && my >= ay && my <= ay + h) {
                selected = e;
                if (!e.locked) { mode = 1; offX = (int) mx - ax; offY = (int) my - ay; pushUndo(e); }
                return true;
            }
        }
        selected = null;
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (mode == 1 && selected != null && !selected.locked) {
            int w = W(selected), h = H(selected);
            int[] p = snap((int) mx - offX, (int) my - offY, w, h);
            setPos(selected, p[0], p[1], w, h);
            return true;
        }
        if (mode == 2 && selected != null && !selected.locked) {
            int ax = AX(selected), ay = AY(selected);
            float base = (float) Math.hypot(HWHudManager.width(mc(), selected), HWHudManager.height(selected));
            float d = (float) Math.hypot(mx - ax, my - ay);
            selected.scale = HWHudManager.clampScale(startScale + (d - startDist) / Math.max(8f, base));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (mode == 1 && selected != null) { reanchor(selected, W(selected), H(selected)); mode = 0; guideX = guideY = Integer.MIN_VALUE; HWHudManager.save(); return true; }
        if (mode == 2) { mode = 0; HWHudManager.save(); return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { if (mode != 0) { undo(); mode = 0; return true; } done(); return true; } // Echap
        if (keyCode == 90 && Screen.hasControlDown()) { undo(); return true; }                          // Ctrl+Z
        if (selected != null && !selected.locked) {
            int step = Screen.hasShiftDown() ? 10 : 1;
            boolean moved = true;
            if (keyCode == 263) { pushUndo(selected); selected.ox -= step / (float) this.width; }
            else if (keyCode == 262) { pushUndo(selected); selected.ox += step / (float) this.width; }
            else if (keyCode == 265) { pushUndo(selected); selected.oy -= step / (float) this.height; }
            else if (keyCode == 264) { pushUndo(selected); selected.oy += step / (float) this.height; }
            else moved = false;
            if (moved) { HWHudManager.save(); return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override public void removed() { HWHudManager.editorPreview = false; super.removed(); }
    @Override public void close() { done(); }
    @Override public boolean shouldPause() { return false; }
}
