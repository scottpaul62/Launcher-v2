package fr.heroesworld.titlescreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

/** Personnage 3D anime rendu SANS entite (modele joueur en unites blocs ~2.0 de haut)
 *  + ailes 3D en volumes (vestiaire). Tete qui suit la souris, rotation utilisateur (glisser). */
public final class HWPlayerPreview {
    private static PlayerEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity> wide, slim;
    private static ModelPart wingsModel;
    private static Identifier whiteTex;
    private static boolean loggedOk = false, loggedErr = false;
    private HWPlayerPreview() {}

    /** Dessine le joueur centre en (cx, cy) sur une hauteur donnee (px). userYaw = rotation par glisser. */
    public static boolean draw(DrawContext ctx, MinecraftClient mc, int cx, int cy, int height,
                               int mouseX, int mouseY, float userYaw, int wingsKind) {
        try {
            boolean useSlim = HWSkin.slim(mc);
            PlayerEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity> model = model(mc, useSlim);
            Identifier tex = HWSkin.texture(mc);
            if (model == null || tex == null) return false;

            float t = (System.currentTimeMillis() % 600000L) / 1000f;
            float headYaw = clamp((float) Math.atan((mouseX - cx) / 70f), -0.7f, 0.7f);
            float headPitch = clamp((float) Math.atan((mouseY - (cy - height * 0.32f)) / 70f), -0.5f, 0.6f);
            model.head.yaw = headYaw; model.head.pitch = headPitch; model.head.roll = 0;
            model.hat.copyTransform(model.head);
            model.body.yaw = headYaw * 0.20f; model.body.pitch = 0; model.body.roll = 0;
            model.jacket.copyTransform(model.body);
            float sway = (float) Math.sin(t * 1.1f);
            model.rightArm.pitch = sway * 0.05f; model.rightArm.yaw = 0; model.rightArm.roll = 0.06f + (float) Math.cos(t * 1.4f) * 0.02f;
            model.leftArm.pitch = -sway * 0.05f; model.leftArm.yaw = 0; model.leftArm.roll = -0.06f - (float) Math.cos(t * 1.4f) * 0.02f;
            model.rightSleeve.copyTransform(model.rightArm);
            model.leftSleeve.copyTransform(model.leftArm);
            model.rightLeg.pitch = sway * 0.03f; model.rightLeg.yaw = 0; model.rightLeg.roll = 0;
            model.leftLeg.pitch = -sway * 0.03f; model.leftLeg.yaw = 0; model.leftLeg.roll = 0;
            model.rightPants.copyTransform(model.rightLeg);
            model.leftPants.copyTransform(model.leftLeg);

            // Le modele s'etend d'environ y=-0.5 (haut de tete) a y=+1.5 (pieds), en BLOCS.
            float scale = height / 2.2f;
            MatrixStack ms = ctx.getMatrices();
            ms.push();
            ms.translate(cx, cy - 0.5f * scale, 400);
            ms.scale(scale, scale, scale);
            ms.multiply(new Quaternionf().rotationY((float) Math.PI + userYaw + (float) Math.sin(t * 0.25f) * 0.08f));
            DiffuseLighting.disableGuiDepthLighting();
            VertexConsumer vc = ctx.getVertexConsumers().getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            model.render(ms, vc, 0xF000F0, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
            if (wingsKind > 0) renderWings(ctx, ms, t, wingsKind);
            ctx.draw();
            DiffuseLighting.enableGuiDepthLighting();
            ms.pop();
            if (!loggedOk) { loggedOk = true; HWHudManager.LOG.info("[PREVIEW] rendu 3D OK (scale {})", scale); }
            return true;
        } catch (Throwable t) {
            if (!loggedErr) { loggedErr = true; HWHudManager.LOG.warn("[PREVIEW] echec rendu 3D", t); }
            return false;
        }
    }

    /** Ailes 3D : cosmetique gek data-driven si le catalogue en declare un, sinon volumes teintes. */
    private static void renderWings(DrawContext ctx, MatrixStack ms, float t, int kind) {
        try {
            fr.heroesworld.titlescreen.gek.HWGekRegistry.Item item = fr.heroesworld.titlescreen.gek.HWGekRegistry.wingsByIndex(kind);
            if (item != null && item.renderer.equals("gek")) {
                fr.heroesworld.titlescreen.gek.HWGekCosmetic gek = fr.heroesworld.titlescreen.gek.HWGekRegistry.gek(item.key);
                if (gek != null) {
                    fr.heroesworld.titlescreen.gek.HWGekCond.Ctx q = new fr.heroesworld.titlescreen.gek.HWGekCond.Ctx();
                    q.inGui = true;
                    q.lifeTime = t;
                    gek.render(ctx, ms, q, 0xF000F0);
                    return;
                }
            }
            ensureWings();
            if (wingsModel == null || whiteTex == null) return;
            int col = wingColor(kind);
            float r = ((col >> 16) & 0xFF) / 255f, g = ((col >> 8) & 0xFF) / 255f, b = (col & 0xFF) / 255f;
            float flap = (float) Math.sin(t * 2.2f) * 0.18f;
            ModelPart right = wingsModel.getChild("r"), left = wingsModel.getChild("l");
            right.roll = 0.55f + flap; right.yaw = 0.65f;
            left.roll = -0.55f - flap; left.yaw = -0.65f;
            VertexConsumer vc = ctx.getVertexConsumers().getBuffer(RenderLayer.getEntityCutoutNoCull(whiteTex));
            wingsModel.render(ms, vc, 0xF000F0, OverlayTexture.DEFAULT_UV, r, g, b, 0.92f);
        } catch (Throwable ignored) {}
    }

    private static int wingColor(int kind) {
        switch (kind) {
            case 2: return 0xBFE8FF;   // foudre
            case 3: return 0xFF9A3C;   // flammes
            case 4: return 0x7FB8FF;   // Hades
            case 5: return 0xF3D889;   // Apollon
            case 6: return 0xF2A9C4;   // celestes
            case 7: return 0xB07CE8;   // Vide
            default: return 0xF4F5F7;  // Hermes
        }
    }

    private static void ensureWings() {
        if (whiteTex == null) {
            try {
                NativeImage img = new NativeImage(8, 8, false);
                for (int x = 0; x < 8; x++) for (int y = 0; y < 8; y++) img.setColor(x, y, 0xFFFFFFFF);
                NativeImageBackedTexture tx = new NativeImageBackedTexture(img);
                Identifier id = new Identifier("heroworld", "white_dyn");
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, tx);
                whiteTex = id;
            } catch (Throwable ignored) {}
        }
        if (wingsModel == null) {
            try {
                ModelData data = new ModelData();
                var root = data.getRoot();
                for (int s = 0; s < 2; s++) {
                    boolean rightSide = s == 0;
                    float sign = rightSide ? 1f : -1f;
                    var w = root.addChild(rightSide ? "r" : "l", ModelPartBuilder.create(),
                        ModelTransform.pivot(sign * 2f, 3f, 2.6f));
                    // 4 plumes en eventail (cuboids fins), longueurs decroissantes
                    for (int f = 0; f < 4; f++) {
                        float len = 12f - f * 2.2f;
                        var fb = ModelPartBuilder.create().cuboid(rightSide ? 0f : -len, -0.8f + f * 1.5f, 0f, len, 1.4f, 0.6f);
                        w.addChild((rightSide ? "rf" : "lf") + f, fb, ModelTransform.rotation(0f, 0f, sign * (0.12f + f * 0.16f)));
                    }
                }
                wingsModel = TexturedModelData.of(data, 16, 16).createModel();
            } catch (Throwable ignored) { wingsModel = null; }
        }
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    private static PlayerEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity> model(MinecraftClient mc, boolean s) {
        try {
            if (s) {
                if (slim == null) slim = new PlayerEntityModel<>(mc.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_SLIM), true);
                return slim;
            }
            if (wide == null) wide = new PlayerEntityModel<>(mc.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER), false);
            return wide;
        } catch (Throwable t) { return null; }
    }
}
