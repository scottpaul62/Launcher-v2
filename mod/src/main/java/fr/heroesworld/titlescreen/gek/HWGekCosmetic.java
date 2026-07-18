package fr.heroesworld.titlescreen.gek;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cosmetique gek-lite charge : modele + clips + machine a etats + rendu (crossfade lineaire). */
public final class HWGekCosmetic {
    public final String key, type, attachedBone;
    public final float scale;
    public final float[] offset;
    private final HWGekModel model;
    private final Map<String, Map<String, HWGekExpr[][]>> clips = new HashMap<>(); // clip -> bone -> [rot, pos, scl][xyz]
    private final List<State> states = new ArrayList<>();
    private final int transitionTicks;

    private String curState = null, prevState = null;
    private long switchAt = 0;

    private static final class State {
        final String anim; final HWGekCond when;
        State(String a, HWGekCond w) { anim = a; when = w; }
    }

    HWGekCosmetic(String key, JsonObject hw, HWGekModel model, JsonObject anim) {
        this.key = key;
        this.model = model;
        this.type = hw.has("type") ? hw.get("type").getAsString() : "wings";
        this.attachedBone = hw.has("attached_bone") ? hw.get("attached_bone").getAsString() : "body";
        this.scale = hw.has("scale") ? hw.get("scale").getAsFloat() : 1f;
        JsonArray off = hw.has("offset") ? hw.getAsJsonArray("offset") : null;
        this.offset = new float[]{0, 0, 0};
        if (off != null) for (int i = 0; i < 3 && i < off.size(); i++) this.offset[i] = off.get(i).getAsFloat();
        this.transitionTicks = hw.has("transition_ticks") ? hw.get("transition_ticks").getAsInt() : 8;

        // clips
        JsonObject anims = anim.getAsJsonObject("animations");
        for (String clipName : anims.keySet()) {
            JsonObject clip = anims.getAsJsonObject(clipName);
            JsonObject bones = clip.has("bones") ? clip.getAsJsonObject("bones") : new JsonObject();
            Map<String, HWGekExpr[][]> per = new HashMap<>();
            for (String bone : bones.keySet()) {
                JsonObject ch = bones.getAsJsonObject(bone);
                HWGekExpr[][] trio = new HWGekExpr[3][];
                trio[0] = HWGekExpr.triple(ch.get("rotation"));
                trio[1] = HWGekExpr.triple(ch.get("position"));
                trio[2] = HWGekExpr.triple(ch.get("scale"));
                per.put(bone, trio);
            }
            clips.put(clipName, per);
        }
        // etats (ordre = priorite, dernier = repli conseille)
        if (hw.has("states")) {
            JsonArray st = hw.getAsJsonArray("states");
            for (int i = 0; i < st.size(); i++) {
                JsonObject s = st.get(i).getAsJsonObject();
                states.add(new State(s.get("anim").getAsString(), HWGekCond.parse(s.get("when").getAsString())));
            }
        }
        if (states.isEmpty()) states.add(new State(clips.keySet().stream().findFirst().orElse("idle"), HWGekCond.parse("always")));
    }

    private String pickState(HWGekCond.Ctx c) {
        for (State s : states) if (s.when.eval(c)) return s.anim;
        return states.get(states.size() - 1).anim;
    }

    /** Applique la machine a etats + les canaux au squelette, puis rend le modele dans la matrice courante. */
    public void render(DrawContext ctx, MatrixStack ms, HWGekCond.Ctx qctx, int light) {
        render(ctx, ms, qctx, light, 1f, 1f, 1f);
    }

    /** Variante teintee (preview des ailes non-gek : modele generique colore). */
    public void render(DrawContext ctx, MatrixStack ms, HWGekCond.Ctx qctx, int light, float r, float g, float b) {
        try {
            String target = pickState(qctx);
            long now = System.currentTimeMillis();
            if (!target.equals(curState)) { prevState = curState; curState = target; switchAt = now; }
            float alpha = transitionTicks <= 0 ? 1f
                : Math.min(1f, (now - switchAt) / (transitionTicks * 50f));

            applyClip(curState, prevState, alpha, qctx.lifeTime);

            ms.push();
            ms.translate(offset[0] / 16f, offset[1] / 16f, offset[2] / 16f);
            if (scale != 1f) ms.scale(scale, scale, scale);
            VertexConsumer vc = ctx.getVertexConsumers().getBuffer(RenderLayer.getEntityCutoutNoCull(model.texture));
            model.root.render(ms, vc, light, OverlayTexture.DEFAULT_UV, r, g, b, 1f);
            ctx.draw();
            ms.pop();
        } catch (Throwable t) {
            HWGekRegistry.LOG.warn("[GEK] rendu '{}' en echec", key, t);
        }
    }

    private void applyClip(String cur, String prev, float alpha, float lifeTime) {
        Map<String, HWGekExpr[][]> a = clips.getOrDefault(cur, Map.of());
        Map<String, HWGekExpr[][]> b = (prev != null && alpha < 1f) ? clips.getOrDefault(prev, Map.of()) : null;
        java.util.HashSet<String> bones = new java.util.HashSet<>(a.keySet());
        if (b != null) bones.addAll(b.keySet());
        for (String bone : bones) {
            var part = model.bone(bone);
            if (part == null) continue;
            float[] rot = channel(a.get(bone), b, bone, 0, alpha, lifeTime);
            // degres bedrock -> radians java (mapping -x, -y, z)
            part.pitch = (float) Math.toRadians(-rot[0]);
            part.yaw = (float) Math.toRadians(-rot[1]);
            part.roll = (float) Math.toRadians(rot[2]);
            // v1 : canal position non applique (les clips pilotes n'en utilisent pas) — rotations seulement.
        }
    }

    private static float[] channel(HWGekExpr[][] curTrio, Map<String, HWGekExpr[][]> prevMap, String bone,
                                   int idx, float alpha, float lifeTime) {
        float[] cur = evalTrio(curTrio, idx, lifeTime);
        if (prevMap == null || alpha >= 1f) return cur;
        float[] prev = evalTrio(prevMap.get(bone), idx, lifeTime);
        return new float[]{
            prev[0] + (cur[0] - prev[0]) * alpha,
            prev[1] + (cur[1] - prev[1]) * alpha,
            prev[2] + (cur[2] - prev[2]) * alpha };
    }
    private static float[] evalTrio(HWGekExpr[][] trio, int idx, float t) {
        if (trio == null || trio[idx] == null) return new float[]{0, 0, 0};
        return new float[]{ trio[idx][0].eval(t), trio[idx][1].eval(t), trio[idx][2].eval(t) };
    }
}
