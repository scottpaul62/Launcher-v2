package fr.heroesworld.titlescreen.mixin;

import fr.heroesworld.titlescreen.HWWingsFeature;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ajoute la couche cosmetiques HERO WORLD au renderer du joueur (crash-safe, require=0). */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerFeatureMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    protected PlayerFeatureMixin(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V",
            at = @At("TAIL"), require = 0)
    private void heroworld$addFeatures(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        try { this.addFeature(new HWWingsFeature(this)); } catch (Throwable ignored) {}
    }
}
