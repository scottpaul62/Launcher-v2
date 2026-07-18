package fr.heroesworld.titlescreen;

import fr.heroesworld.titlescreen.gek.HWGekCond;
import fr.heroesworld.titlescreen.gek.HWGekCosmetic;
import fr.heroesworld.titlescreen.gek.HWGekRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

/** Personnage 3D anime rendu SANS entite (modele joueur en unites blocs ~2.0 de haut).
 *  Ailes rendues par le moteur gek data-driven (modele dedie ou modele generique teinte).
 *  Tete qui suit doucement la souris, rotation utilisateur au glisser (axe vertical). */
public final class HWPlayerPreview {
    private static PlayerEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity> wide, slim;
    private static boolean loggedOk = false, loggedErr = false;
    private HWPlayerPreview() {}

    public static boolean draw(DrawContext ctx, MinecraftClient mc, int cx, int cy, int height,
                               int mouseX, int mouseY, float userYaw, int wingsKind) {
        try {
            boolean useSlim = HWSkin.slim(mc);
            PlayerEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity> model = model(mc, useSlim);
            Identifier tex = HWSkin.texture(mc);
            if (model == null || tex == null) return false;

            float t = (System.currentTimeMillis() % 600000L) / 1000f;
            // suivi de la souris : doux et borne (fini la tete qui pique du nez)
            float headYaw = clamp((float) Math.atan((mouseX - cx) / 110f), -0.45f, 0.45f);
            float headPitch = clamp((float) Math.atan((mouseY - (cy - height * 0.33f)) / 150f), -0.25f, 0.30f);
            model.head.yaw = headYaw; model.head.pitch = headPitch; model.head.roll = 0;
            model.hat.copyTransform(model.head);
            model.body.yaw = headYaw * 0.15f; model.body.pitch = 0; model.body.roll = 0;
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
            ms.multiply(new Quaternionf().rotationY((float) Math.PI + userYaw + (float) Math.sin(t * 0.25f) * 0.06f));
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

    /** Toutes les ailes en 3D : modele gek dedie si l'item en a un, sinon modele generique teinte. */
    private static void renderWings(DrawContext ctx, MatrixStack ms, float t, int kind) {
        try {
            HWGekRegistry.Item item = HWGekRegistry.wingsByIndex(kind);
            HWGekCond.Ctx q = new HWGekCond.Ctx();
            q.inGui = true;
            q.lifeTime = t;
            if (item != null && item.renderer.equals("gek")) {
                HWGekCosmetic gek = HWGekRegistry.gek(item.key);
                if (gek != null) gek.render(ctx, ms, q, 0xF000F0);
                return;
            }
            // ailes "particules" : apercu 3D via le modele generique, teinte a leur couleur
            HWGekCosmetic generic = HWGekRegistry.gek("ailes_zeus");
            if (generic != null) {
                int col = wingColor(kind);
                generic.render(ctx, ms, q, 0xF000F0,
                    ((col >> 16) & 0xFF) / 255f, ((col >> 8) & 0xFF) / 255f, (col & 0xFF) / 255f);
            }
        } catch (Throwable ignored) {}
    }

    public static int wingColor(int kind) {
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
