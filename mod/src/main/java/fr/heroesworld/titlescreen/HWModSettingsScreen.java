package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;

/** Reglages profonds d'un mod : echelle, couleur + opacite, fond, touches, apercu live. */
public class HWModSettingsScreen extends Screen {
    private final Screen parent;
    private final HWHudManager.El el;
    private boolean capturingKey = false;

    private static final int[] PALETTE = {
        0, 0xFFE8C56A, 0xFF49BDF2, 0xFF48C78E, 0xFFE45B68, 0xFFB07CE8, 0xFF5AA9E6, 0xFFE7B84B, 0xFFA9AFBA
    };

    public HWModSettingsScreen(Screen parent, HWHudManager.El el) {
        super(Text.literal("Reglages du mod")); this.parent = parent; this.el = el;
    }
    private void reopen() { this.client.setScreen(new HWModSettingsScreen(parent, el)); }

    @Override
    protected void init() {
        int cx = this.width / 2, w = 300, x = cx - w / 2;
        int y = Math.max(56, (int) (this.height * 0.20));

        if (el.id.equals("chat")) {
            y = buildChatControls(x, y, w);
            this.addDrawableChild(new HWButton(cx - 70, y + 4, 140, 22, Text.literal("Retour"), HWButton.SECONDARY, 0,
                b -> this.client.setScreen(parent)));
            return;
        }

        this.addDrawableChild(new HWButton(x, y, w, 22, Text.literal("Active : " + (el.enabled ? "§aON" : "§8OFF")), HWButton.SECONDARY, 0,
            b -> { el.enabled = !el.enabled; HWHudManager.save(); reopen(); })); y += 26;

        if (el.id.equals("crosshair")) {
            String style = el.opt % 3 == 1 ? "POINT" : (el.opt % 3 == 2 ? "CERCLE" : "CROIX");
            this.addDrawableChild(new HWButton(x, y, w, 22, Text.literal("Style : §b" + style), HWButton.SECONDARY, 0,
                b -> { el.opt = (el.opt + 1) % 3; HWHudManager.save(); reopen(); })); y += 26;
        }

        if (el.id.equals("zoom")) {
            String key = keyName(HWClientConfig.zoomKey);
            this.addDrawableChild(new HWButton(x, y, w, 22,
                Text.literal(capturingKey ? "§eAppuie sur une touche..." : "Touche du zoom : §b" + key), HWButton.SECONDARY, 0,
                b -> { capturingKey = true; b.setMessage(Text.literal("§eAppuie sur une touche... (Echap : annuler)")); })); y += 26;
            this.addDrawableChild(new HWSlider(x, y, w, 22, (HWZoom.clampFactor(HWClientConfig.zoomFactor) - 2f) / 6f,
                v -> "Facteur : x" + String.format("%.1f", 2f + v * 6f),
                v -> { HWClientConfig.zoomFactor = (float) (2f + v * 6f); HWClientConfig.save(); })); y += 26;
        } else if (el.id.equals("togglesprint")) {
            boolean sp = sprintToggled(), sn = sneakToggled();
            this.addDrawableChild(new HWButton(x, y, w, 22, Text.literal("Sprint : " + (sp ? "§aTOGGLE" : "§fMAINTENU")), HWButton.SECONDARY, 0,
                b -> { setSprintToggled(!sprintToggled()); reopen(); })); y += 26;
            this.addDrawableChild(new HWButton(x, y, w, 22, Text.literal("Sneak : " + (sn ? "§aTOGGLE" : "§fMAINTENU")), HWButton.SECONDARY, 0,
                b -> { setSneakToggled(!sneakToggled()); reopen(); })); y += 26;
        }

        this.addDrawableChild(new HWSlider(x, y, w, 22, (HWHudManager.clampScale(el.scale) - 0.5f) / 1.5f,
            v -> "Echelle : " + Math.round((0.5f + v * 1.5f) * 100) + "%",
            v -> { el.scale = HWHudManager.clampScale((float) (0.5f + v * 1.5f)); HWHudManager.save(); })); y += 26;

        if (isTextWidget() || el.id.equals("crosshair")) {
            int sw = (w - (PALETTE.length - 1) * 3) / PALETTE.length;
            for (int i = 0; i < PALETTE.length; i++) {
                final int col = PALETTE[i];
                final int rgb = col & 0xFFFFFF;
                BooleanSupplier sel = () -> (col == 0 && el.color == 0) || (col != 0 && el.color != 0 && (el.color & 0xFFFFFF) == rgb);
                this.addDrawableChild(new HWSwatch(x + i * (sw + 3), y, sw, 16, col, sel,
                    b -> { el.color = (col == 0) ? 0 : ((el.color == 0 ? 0xFF000000 : (el.color & 0xFF000000)) | rgb); HWHudManager.save(); }));
            }
            y += 20;
            int alpha = el.color == 0 ? 255 : (el.color >>> 24);
            this.addDrawableChild(new HWSlider(x, y, w, 22, Math.max(0f, (alpha / 255f - 0.3f) / 0.7f),
                v -> "Opacite : " + Math.round((0.3f + v * 0.7f) * 100) + "%",
                v -> {
                    int a = Math.round((0.3f + (float) v * 0.7f) * 255f);
                    int rgb2 = el.color == 0 ? 0xFFFFFF : (el.color & 0xFFFFFF);
                    el.color = (a << 24) | rgb2;
                    HWHudManager.save();
                })); y += 26;
            if (isTextWidget()) {
                String bg = el.bgMode == 1 ? "§aACTIF" : (el.bgMode == 2 ? "§8DESACTIVE" : "§7GLOBAL");
                this.addDrawableChild(new HWButton(x, y, w, 22, Text.literal("Fond du widget : " + bg), HWButton.SECONDARY, 0,
                    b -> { el.bgMode = (el.bgMode + 1) % 3; HWHudManager.save(); reopen(); })); y += 26;
            }
        }

        this.addDrawableChild(new HWButton(x, y, w, 22, Text.literal("Verrouille : " + (el.locked ? "§aOUI" : "§8NON")), HWButton.SECONDARY, 0,
            b -> { el.locked = !el.locked; HWHudManager.save(); reopen(); })); y += 26;
        this.addDrawableChild(new HWButton(x, y, 147, 22, Text.literal("Deplacer (editeur)"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(new HWHudEditScreen(parent))));
        this.addDrawableChild(new HWButton(x + 153, y, 147, 22, Text.literal("Reinitialiser"), HWButton.SECONDARY, 0,
            b -> { HWHudManager.resetPos(el); el.color = 0; el.bgMode = 0; HWHudManager.save(); reopen(); })); y += 30;
        this.addDrawableChild(new HWButton(cx - 70, y, 140, 22, Text.literal("Retour"), HWButton.SECONDARY, 0,
            b -> this.client.setScreen(parent)));
    }

    private boolean isTextWidget() {
        switch (el.id) {
            case "keystrokes": case "armor": case "togglesprint":
            case "chat": case "scoreboard": case "waypoints": case "crosshair":
                return false;
            default: return true;
        }
    }

    /** Reglages du tchat vanilla (opacites, taille, largeur) — le comportement natif est conserve. */
    private int buildChatControls(int x, int y, int w) {
        y = vanillaSlider(x, y, w, "Opacite du texte", () -> this.client.options.getChatOpacity().getValue(),
            v -> this.client.options.getChatOpacity().setValue(v));
        y = vanillaSlider(x, y, w, "Opacite du fond", () -> this.client.options.getTextBackgroundOpacity().getValue(),
            v -> this.client.options.getTextBackgroundOpacity().setValue(v));
        y = vanillaSlider(x, y, w, "Taille du texte", () -> this.client.options.getChatScale().getValue(),
            v -> this.client.options.getChatScale().setValue(v));
        y = vanillaSlider(x, y, w, "Largeur du tchat", () -> this.client.options.getChatWidth().getValue(),
            v -> this.client.options.getChatWidth().setValue(v));
        return y;
    }
    private interface DGet { double get() throws Throwable; }
    private interface DSet { void set(double v) throws Throwable; }
    private int vanillaSlider(int x, int y, int w, String label, DGet get, DSet set) {
        double v0 = 1.0;
        try { v0 = get.get(); } catch (Throwable ignored) {}
        this.addDrawableChild(new HWSlider(x, y, w, 22, v0,
            v -> label + " : " + Math.round(v * 100) + "%",
            v -> { try { set.set(v); this.client.options.write(); } catch (Throwable ignored) {} }));
        return y + 26;
    }

    private static String keyName(int code) {
        try { return InputUtil.fromKeyCode(code, 0).getLocalizedText().getString().toUpperCase(); }
        catch (Throwable t) { return "#" + code; }
    }

    private boolean sprintToggled() { try { return this.client.options.getSprintToggled().getValue(); } catch (Throwable t) { return false; } }
    private boolean sneakToggled() { try { return this.client.options.getSneakToggled().getValue(); } catch (Throwable t) { return false; } }
    private void setSprintToggled(boolean v) { try { this.client.options.getSprintToggled().setValue(v); this.client.options.write(); } catch (Throwable ignored) {} }
    private void setSneakToggled(boolean v) { try { this.client.options.getSneakToggled().setValue(v); this.client.options.write(); } catch (Throwable ignored) {} }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingKey) {
            if (keyCode != 256) { HWClientConfig.zoomKey = keyCode; HWClientConfig.save(); } // 256 = Echap : annuler
            capturingKey = false;
            reopen();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (this.client.world != null) super.renderBackground(ctx, mouseX, mouseY, delta);
        else { HWScene.draw(ctx, this.width, this.height); ctx.fill(0, 0, this.width, this.height, 0xB0000000); }
        int y0 = Math.max(56, (int) (this.height * 0.20));
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§e" + el.name), this.width / 2, y0 - 40, 0xFFE8C56A);
        super.render(ctx, mouseX, mouseY, delta);

        // Apercu live (en jeu uniquement : donnees reelles)
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null && (isTextWidget() || el.id.equals("crosshair")) && !el.id.equals("zoom")) {
            try {
                String s = HWHudManager.text(mc, el);
                if (!s.isEmpty()) {
                    int tw = mc.textRenderer.getWidth(s);
                    int axc = this.width / 2 - Math.round(tw * el.scale) / 2, ayc = y0 - 16;
                    MatrixStack ms = ctx.getMatrices();
                    ms.push();
                    ms.translate(axc, ayc, 0);
                    ms.scale(el.scale, el.scale, 1f);
                    HWHudManager.renderAt(ctx, mc, el, 0, 0);
                    ms.pop();
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override public void close() { this.client.setScreen(parent); }
    @Override public boolean shouldPause() { return false; }

    /** Slider generique HERO WORLD (libelle + application en direct). */
    static class HWSlider extends SliderWidget {
        interface Fmt { String label(double v); }
        interface Apply { void apply(double v); }
        private final Fmt fmt; private final Apply apply;
        HWSlider(int x, int y, int w, int h, double v0, Fmt fmt, Apply apply) {
            super(x, y, w, h, Text.literal(fmt.label(Math.max(0, Math.min(1, v0)))), Math.max(0, Math.min(1, v0)));
            this.fmt = fmt; this.apply = apply;
        }
        @Override protected void updateMessage() { setMessage(Text.literal(fmt.label(this.value))); }
        @Override protected void applyValue() { apply.apply(this.value); }
        @Override
        public void renderWidget(DrawContext ctx, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            boolean over = isHovered() || isFocused();
            HWDraw.panel(ctx, x, y, w, h, 4, 0x9014121C, over ? 0xB049BDF2 : 0x33FFFFFF);
            int track = (int) ((w - 12) * this.value);
            ctx.fill(x + 3, y + h - 5, x + 3 + Math.max(0, track), y + h - 3, 0xFFE8C56A);
            int kx = x + 3 + track;
            ctx.fill(kx, y + 3, kx + 6, y + h - 3, 0xFFE8C56A);
            ctx.fill(kx + 1, y + 4, kx + 5, y + h - 4, 0xFFF3DFA0);
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), x + w / 2, y + (h - 8) / 2, 0xFFF4F5F7);
        }
    }

    /** Pastille de couleur (0 = auto/defaut). */
    static class HWSwatch extends ButtonWidget {
        private final int color; private final BooleanSupplier selected;
        HWSwatch(int x, int y, int w, int h, int color, BooleanSupplier selected, PressAction a) {
            super(x, y, w, h, Text.empty(), a, DEFAULT_NARRATION_SUPPLIER);
            this.color = color; this.selected = selected;
        }
        @Override
        protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            ctx.fill(x, y, x + w, y + h, 0xFF15181F);
            ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, color == 0 ? 0xFF3A3F4A : (color | 0xFF000000));
            int b = selected.getAsBoolean() ? 0xFFE8C56A : (isHovered() ? 0xFF49BDF2 : 0xFF323844);
            ctx.fill(x, y, x + w, y + 1, b); ctx.fill(x, y + h - 1, x + w, y + h, b);
            ctx.fill(x, y, x + 1, y + h, b); ctx.fill(x + w - 1, y, x + w, y + h, b);
            if (color == 0) ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal("A"), x + w / 2, y + (h - 8) / 2, 0xFFCFC9DA);
        }
    }
}
