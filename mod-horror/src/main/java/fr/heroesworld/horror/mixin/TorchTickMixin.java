package fr.heroesworld.horror.mixin;

import fr.heroesworld.horror.HorrorConfig;
import fr.heroesworld.horror.Torch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ThreadLocalRandom;

/** Touche toggle de la lampe torche + poussière dans le faisceau, chaque tick (client-side). */
@Mixin(ClientPlayerEntity.class)
public class TorchTickMixin {
    @Unique private boolean heroworld$prevKey = false;
    @Unique private long heroworld$t = 0L;

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void heroworld$torch(CallbackInfo ci) {
        heroworld$t++;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
            if (mc == null || mc.player != self) return; // uniquement le joueur local

            boolean holding = Torch.holdingTorch(self);
            if (!holding && Torch.on) Torch.forceOff(); // lampe lâchée → extinction locale (le serveur suit)

            boolean pressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), HorrorConfig.torchKey);
            if (pressed && !heroworld$prevKey && mc.currentScreen == null && holding) Torch.toggle(mc);
            heroworld$prevKey = pressed;

            // grains de poussière flottant dans le faisceau (discret : 2 particules / 2 ticks)
            if (Torch.on && holding && HorrorConfig.particles && heroworld$t % 2 == 0
                    && self.getWorld() instanceof ClientWorld cw) {
                Vec3d eye = self.getEyePos();
                Vec3d look = self.getRotationVec(1.0f);
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 2; i++) {
                    double d = 1.5 + r.nextDouble() * 6.0;          // distance le long du faisceau
                    double spread = 0.06 * d;                        // cône : s'élargit avec la distance
                    double ox = (r.nextDouble() - 0.5) * spread;
                    double oy = (r.nextDouble() - 0.5) * spread;
                    double oz = (r.nextDouble() - 0.5) * spread;
                    cw.addParticle(ParticleTypes.END_ROD,
                            eye.x + look.x * d + ox,
                            eye.y + look.y * d + oy - 0.05,
                            eye.z + look.z * d + oz,
                            look.x * 0.01, look.y * 0.01, look.z * 0.01);
                }
            }
        } catch (Throwable ignored) {}
    }
}
