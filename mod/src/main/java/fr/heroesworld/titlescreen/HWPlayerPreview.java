package fr.heroesworld.titlescreen;

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

/** Personnage 3D anime rendu SANS entite : modele joueur + skin reelle, idle + tete qui suit la souris. */
public final class HWPlayerPreview {
    private static PlayerEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity> wide, slim;
    private HWPlayerPreview() {}

    /** Dessine le joueur centre en (cx, cy) sur une hauteur donnee. Renvoie false si indisponible. */
    public static boolean draw(DrawContext ctx, MinecraftClient mc, int cx, int cy, int height, int mouseX, int mouseY) {
        try {
            boolean useSlim = HWSkin.slim(mc);
            PlayerEntityModel<net.minecraft.client.network.AbstractClientPlayerEntity> model = model(mc, useSlim);
            Identifier tex = HWSkin.texture(mc);
            if (model == null || tex == null) return false;

            float t = (System.currentTimeMillis() % 600000L) / 1000f;
            float headYaw = clamp((float) Math.atan((mouseX - cx) / 70f), -0.7f, 0.7f);
            float headPitch = clamp((float) Math.atan((mouseY - (cy - height * 0.30f)) / 70f), -0.6f, 0.6f);
            model.head.yaw = headYaw; model.head.pitch = headPitch; model.head.roll = 0;
            model.hat.copyTransform(model.head);
            model.body.yaw = headYaw * 0.22f; model.body.pitch = 0; model.body.roll = 0;
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

            float scale = height / 36f; // modele : y de -8 (haut de tete) a 24 (pieds)
            MatrixStack ms = ctx.getMatrices();
            ms.push();
            ms.translate(cx, cy - 8f * scale, 150);
            ms.scale(scale, scale, scale);
            ms.multiply(new Quaternionf().rotationY((float) Math.PI + (float) Math.sin(t * 0.30f) * 0.35f));
            DiffuseLighting.disableGuiDepthLighting();
            VertexConsumer vc = ctx.getVertexConsumers().getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            model.render(ms, vc, 0xF000F0, OverlayTexture.DEFAULT_UV);
            ctx.draw();
            DiffuseLighting.enableGuiDepthLighting();
            ms.pop();
            return true;
        } catch (Throwable t) { return false; }
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
