package fr.heroesworld.titlescreen;

/** Themes d'arriere-plan du menu (palette de la scene de l'Olympe). */
public final class HWTheme {
    public static final class Theme {
        public final String name, desc;
        public final int skyTop, skyBottom, nebula, star, fireHot, fireCool, glow, accent;
        public final boolean lightning, available;
        public Theme(String name, String desc, int skyTop, int skyBottom, int nebula, int star,
                     int fireHot, int fireCool, int glow, int accent, boolean lightning, boolean available) {
            this.name = name; this.desc = desc; this.skyTop = skyTop; this.skyBottom = skyBottom;
            this.nebula = nebula; this.star = star; this.fireHot = fireHot; this.fireCool = fireCool;
            this.glow = glow; this.accent = accent; this.lightning = lightning; this.available = available;
        }
    }

    public static final Theme[] THEMES = {
        new Theme("Panthéon nocturne", "Temple sous les étoiles", 0xFF0B1226, 0xFF05070E, 0x556A8CFF, 0xFFBFD8FF, 0xFFFF9A3C, 0xFFFFE08A, 0xFFFF9A3C, 0xFFE8C56A, false, true),
        new Theme("Classique",         "Sombre & épuré",          0xFF0C0C12, 0xFF050507, 0x55606070, 0xFFC8C8D8, 0xFFFF9A3C, 0xFFFFE08A, 0xFF808090, 0xFFE8C56A, false, true),
        new Theme("Olympe",            "Foudre dorée",            0xFF1A1408, 0xFF080604, 0x55F0C86A, 0xFFF3E4B0, 0xFFFFB24C, 0xFFFFEEA0, 0xFFFFC24C, 0xFFE8C56A, true, false),
        new Theme("Jupiter",           "Géante gazeuse",          0xFF0A1420, 0xFF04070C, 0x554C7AA8, 0xFFCFE4FF, 0xFFFF9A3C, 0xFFFFE08A, 0xFF4C7AA8, 0xFF7FB0E6, false, false),
        new Theme("Nuit de Zeus",      "Ciel d'orage violet",     0xFF120F22, 0xFF05040A, 0x557A6CFF, 0xFFD8D2FF, 0xFFB98CFF, 0xFFE6D8FF, 0xFF7A6CFF, 0xFFB59CFF, true, false)
    };

    public static int index = 0;
    public static Theme current() { int n = THEMES.length; return THEMES[((index % n) + n) % n]; }
    private HWTheme() {}
}
