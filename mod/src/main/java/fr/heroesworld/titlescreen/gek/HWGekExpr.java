package fr.heroesworld.titlescreen.gek;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Canal de valeur d'animation gek-lite : constante, ou expression sin/cos du sous-ensemble Molang.
 *  Formes supportees : nombre | "math.sin(q.life_time*F)*A+B" | "math.cos(...)" (F, A, B optionnels signes). */
public final class HWGekExpr {
    private static final Pattern P = Pattern.compile(
        "math\\.(sin|cos)\\(\\s*q\\.life_time\\s*\\*\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:\\+\\s*(-?\\d+(?:\\.\\d+)?))?\\s*\\)" +
        "(?:\\s*\\*\\s*(-?\\d+(?:\\.\\d+)?))?(?:\\s*(-?\\d+(?:\\.\\d+)?))?");

    private final boolean cos;
    private final float freq, phase, amp, off;
    private final float constant;
    private final boolean isConst;

    private HWGekExpr(float constant) { this.constant = constant; this.isConst = true; this.cos = false; this.freq = this.phase = this.amp = this.off = 0; }
    private HWGekExpr(boolean cos, float freq, float phase, float amp, float off) {
        this.cos = cos; this.freq = freq; this.phase = phase; this.amp = amp; this.off = off;
        this.constant = 0; this.isConst = false;
    }

    public float eval(float lifeTime) {
        if (isConst) return constant;
        double v = cos ? Math.cos(lifeTime * freq + phase) : Math.sin(lifeTime * freq + phase);
        return (float) (v * amp + off);
    }

    /** Parse un element de canal (nombre ou chaine). Repli silencieux sur 0 + warning. */
    public static HWGekExpr parse(JsonElement e) {
        try {
            if (e == null) return new HWGekExpr(0f);
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) return new HWGekExpr(e.getAsFloat());
            String s = e.getAsString().trim();
            try { return new HWGekExpr(Float.parseFloat(s)); } catch (NumberFormatException ignored) {}
            Matcher m = P.matcher(s);
            if (m.find()) {
                boolean cos = m.group(1).equals("cos");
                float f = Float.parseFloat(m.group(2));
                float p = m.group(3) != null ? Float.parseFloat(m.group(3)) : 0f;
                float a = m.group(4) != null ? Float.parseFloat(m.group(4)) : 1f;
                float o = m.group(5) != null ? Float.parseFloat(m.group(5)) : 0f;
                return new HWGekExpr(cos, f, p, a, o);
            }
            HWGekRegistry.LOG.warn("[GEK] expression non supportee, repli 0 : {}", s);
            return new HWGekExpr(0f);
        } catch (Throwable t) { return new HWGekExpr(0f); }
    }

    /** Triplet de canaux depuis un JsonArray [x, y, z]. */
    public static HWGekExpr[] triple(JsonElement e) {
        HWGekExpr[] out = { new HWGekExpr(0f), new HWGekExpr(0f), new HWGekExpr(0f) };
        try {
            if (e != null && e.isJsonArray()) {
                JsonArray a = e.getAsJsonArray();
                for (int i = 0; i < 3 && i < a.size(); i++) out[i] = parse(a.get(i));
            }
        } catch (Throwable ignored) {}
        return out;
    }
}
