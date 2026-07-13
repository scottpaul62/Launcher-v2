package fr.heroesworld.titlescreen;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;

/** Effets de particules d'aura (client-side) autour d'un joueur. */
public final class HWAura {
    private HWAura() {}

    public static void spawn(ClientWorld world, Entity e, int aura, long t) {
        if (world == null || e == null || aura <= 0) return;
        double x = e.getX(), y = e.getY(), z = e.getZ();
        double ang = (t * 0.20);
        double r = 0.6;
        double px = x + Math.cos(ang) * r, pz = z + Math.sin(ang) * r;
        switch (aura) {
            case 1 -> {
                world.addParticle(ParticleTypes.ELECTRIC_SPARK, px, y + 1.0, pz, 0, 0.02, 0);
                if (t % 7 == 0) world.addParticle(ParticleTypes.ELECTRIC_SPARK, x + (Math.random() - 0.5), y + 2.3, z + (Math.random() - 0.5), 0, -0.15, 0);
            }
            case 2 -> {
                world.addParticle(ParticleTypes.END_ROD, px, y + 2.15 + Math.sin(ang) * 0.08, pz, 0, 0.001, 0);
                if (t % 5 == 0) world.addParticle(ParticleTypes.WAX_ON, px, y + 1.2, pz, 0, 0.01, 0);
            }
            case 3 -> {
                world.addParticle(ParticleTypes.FLAME, px, y + 0.1, pz, 0, 0.01, 0);
                if (t % 3 == 0) world.addParticle(ParticleTypes.SMOKE, px, y + 0.3, pz, 0, 0.02, 0);
            }
            case 4 -> {
                world.addParticle(ParticleTypes.SOUL, px, y + 0.1, pz, 0, 0.03, 0);
                if (t % 4 == 0) world.addParticle(ParticleTypes.LARGE_SMOKE, px, y + 0.2, pz, 0, 0.01, 0);
            }
            case 5 -> {
                if (t % 2 == 0) world.addParticle(ParticleTypes.CHERRY_LEAVES, x + (Math.random() - 0.5) * 1.3, y + 2.4, z + (Math.random() - 0.5) * 1.3, 0, 0, 0);
            }
        }
    }
}
