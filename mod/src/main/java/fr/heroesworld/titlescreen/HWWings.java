package fr.heroesworld.titlescreen;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/** Ailes de particules (client-side) qui battent derrière le joueur. */
public final class HWWings {
    private HWWings() {}

    public static void spawn(ClientWorld world, Entity e, int wings, long t) {
        if (world == null || e == null || wings <= 0) return;
        if (!HWClientConfig.cosmeticsParticles) return;
        ParticleEffect p;
        switch (wings) {
            case 1 -> p = ParticleTypes.END_ROD;
            case 2 -> p = ParticleTypes.ELECTRIC_SPARK;
            case 3 -> p = ParticleTypes.FLAME;
            case 4 -> p = ParticleTypes.SOUL_FIRE_FLAME;
            default -> { return; }
        }
        double yaw = Math.toRadians(e.getYaw());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw); // avant
        double rx = fz, rz = -fx;                        // côté
        double x = e.getX(), y = e.getY(), z = e.getZ();
        double flap = Math.sin(t * 0.25) * 0.35;         // battement
        int seg = 6;
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 1; i <= seg; i++) {
                double f = i / (double) seg;
                double side = s * (0.15 + f * 1.05);
                double up = 1.9 - f * 0.5 + Math.sin(f * Math.PI) * 0.35 + flap * f;
                double back = 0.30 + f * 0.15;
                double px = x + side * rx - back * fx;
                double py = y + up;
                double pz = z + side * rz - back * fz;
                if ((t + i) % 2 == 0) world.addParticle(p, px, py, pz, 0, 0, 0);
            }
        }
    }
}
