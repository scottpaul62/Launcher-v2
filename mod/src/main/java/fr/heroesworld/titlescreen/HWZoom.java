package fr.heroesworld.titlescreen;

/** Zoom HERO WORLD : maintien d'une touche (config) -> FOV divise, transition lissee. */
public final class HWZoom {
    /** Etat "touche maintenue" (mis a jour chaque tick par AuraTickMixin). */
    public static volatile boolean active = false;
    private static float smooth = 1f;

    private HWZoom() {}

    /** Multiplie le FOV calcule par le facteur lisse. Appele chaque frame par GameRendererMixin. */
    public static double applyFov(double fov) {
        float target = active ? (1f / clampFactor(HWClientConfig.zoomFactor)) : 1f;
        smooth += (target - smooth) * 0.30f;
        if (Math.abs(smooth - target) < 0.002f) smooth = target;
        return fov * smooth;
    }

    /** Vrai tant que le zoom est actif OU en cours de transition (pour continuer a lisser). */
    public static boolean zooming() { return active || smooth < 0.999f; }

    public static float clampFactor(float f) { return Math.max(2f, Math.min(8f, f)); }
}
