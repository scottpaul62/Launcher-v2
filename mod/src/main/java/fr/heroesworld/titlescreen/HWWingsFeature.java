package fr.heroesworld.titlescreen;

import fr.heroesworld.titlescreen.gek.HWGekCond;
import fr.heroesworld.titlescreen.gek.HWGekCosmetic;
import fr.heroesworld.titlescreen.gek.HWGekRegistry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;

/**
 * PHASE 2 : rendu des cosmetiques gek EN JEU, sur le joueur (visible en F5).
 * S'accroche au buste (suit l'inclinaison sneak/nage), machine a etats alimentee
 * par l'etat REEL du joueur (vol, elytra, vitesse, sol...). 100 % crash-safe.
 */
public class HWWingsFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public HWWingsFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
        super(ctx);
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        try {
            int idx = HWCosmetics.wings;
            if (idx <= 0 || player.isInvisible()) return;
            if (!player.isMainPlayer()) return; // v1 : joueur local (les autres via le serveur, phase 4)
            HWGekRegistry.Item it = HWGekRegistry.wingsByIndex(idx);
            if (it == null || !"gek".equals(it.renderer)) return;
            HWGekCosmetic gek = HWGekRegistry.gek(it.key);
            if (gek == null) return;

            HWGekCond.Ctx q = new HWGekCond.Ctx();
            q.inGui = false;
            q.isFlying = player.getAbilities().flying;
            q.isElytra = player.isFallFlying();
            q.isSwimming = player.isSwimming();
            q.isSneaking = player.isSneaking();
            q.onGround = player.isOnGround();
            var v = player.getVelocity();
            q.speed = (float) Math.sqrt(v.x * v.x + v.z * v.z);
            q.yVelocity = (float) v.y;
            q.lifeTime = (player.age + tickDelta) / 20f;

            ms.push();
            this.getContextModel().body.rotate(ms); // colle au buste
            gek.renderWorld(ms, vcp, q, light);
            ms.pop();
        } catch (Throwable ignored) {}
    }
}
