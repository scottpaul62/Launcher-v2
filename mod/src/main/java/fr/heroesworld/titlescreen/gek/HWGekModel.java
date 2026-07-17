package fr.heroesworld.titlescreen.gek;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Chargeur de geometrie Bedrock (sous-ensemble Blockbench) -> ModelPart Yarn + texture PNG. */
public final class HWGekModel {
    public final ModelPart root;
    public final Identifier texture;
    private final Map<String, ModelPart> bones = new HashMap<>();

    private HWGekModel(ModelPart root, Identifier texture) {
        this.root = root; this.texture = texture;
        index(root);
    }
    private void index(ModelPart p) {
        // ModelPart n'expose pas la liste des enfants par nom de maniere simple : on retient via getChild en aval.
    }

    public ModelPart bone(String name) {
        ModelPart p = bones.get(name);
        if (p != null) return p;
        try { p = root.getChild(name); } catch (Throwable ignored) {}
        if (p == null) p = findDeep(root, name, 0);
        if (p != null) bones.put(name, p);
        return p;
    }
    private static ModelPart findDeep(ModelPart p, String name, int depth) {
        if (depth > 6) return null;
        try { ModelPart c = p.getChild(name); if (c != null) return c; } catch (Throwable ignored) {}
        // pas d'API de liste des enfants -> on tente via les noms connus au chargement (bones sont plats sous root ou 1 niveau)
        return null;
    }

    private static float[] arr3(JsonArray a, float dx, float dy, float dz) {
        float[] r = { dx, dy, dz };
        if (a != null) for (int i = 0; i < 3 && i < a.size(); i++) r[i] = a.get(i).getAsFloat();
        return r;
    }

    /** Construit le modele depuis un geo.json (chemin resource absolu) + texture. */
    public static HWGekModel load(String geoPath, String texPath, String key) throws Exception {
        JsonObject root = HWGekRegistry.readJson(geoPath);
        JsonArray geos = root.getAsJsonArray("minecraft:geometry");
        JsonObject g = geos.get(0).getAsJsonObject();
        JsonObject desc = g.getAsJsonObject("description");
        int tw = desc.has("texture_width") ? desc.get("texture_width").getAsInt() : 32;
        int th = desc.has("texture_height") ? desc.get("texture_height").getAsInt() : 32;

        ModelData data = new ModelData();
        ModelPartData rootData = data.getRoot();
        Map<String, ModelPartData> byName = new HashMap<>();
        Map<String, float[]> pivots = new HashMap<>();

        JsonArray bones = g.getAsJsonArray("bones");
        for (int i = 0; i < bones.size(); i++) {
            JsonObject b = bones.get(i).getAsJsonObject();
            String name = b.get("name").getAsString();
            String parent = b.has("parent") ? b.get("parent").getAsString() : null;
            float[] piv = arr3(b.has("pivot") ? b.getAsJsonArray("pivot") : null, 0, 24, 0);
            float[] rot = arr3(b.has("rotation") ? b.getAsJsonArray("rotation") : null, 0, 0, 0);
            pivots.put(name, piv);

            ModelPartData parentData = rootData;
            float px = piv[0], py = 24f - piv[1], pz = piv[2]; // bedrock (y-up, sol=0) -> java (y-down, origine 24)
            if (parent != null && byName.containsKey(parent)) {
                parentData = byName.get(parent);
                float[] pp = pivots.get(parent);
                px = piv[0] - pp[0];
                py = -(piv[1] - pp[1]);
                pz = piv[2] - pp[2];
            }

            ModelPartBuilder builder = ModelPartBuilder.create();
            if (b.has("cubes")) {
                JsonArray cubes = b.getAsJsonArray("cubes");
                for (int cIdx = 0; cIdx < cubes.size(); cIdx++) {
                    JsonObject cu = cubes.get(cIdx).getAsJsonObject();
                    float[] org = arr3(cu.getAsJsonArray("origin"), 0, 0, 0);
                    float[] siz = arr3(cu.getAsJsonArray("size"), 1, 1, 1);
                    float[] uv = arr3(cu.has("uv") ? cu.getAsJsonArray("uv") : null, 0, 0, 0);
                    float infl = cu.has("inflate") ? cu.get("inflate").getAsFloat() : 0f;
                    // coordonnees relatives au pivot du bone (conversion y-up -> y-down)
                    float rx = org[0] - piv[0];
                    float ry = -((org[1] - piv[1]) + siz[1]);
                    float rz = org[2] - piv[2];
                    builder.uv((int) uv[0], (int) uv[1]).cuboid(rx, ry, rz, siz[0], siz[1], siz[2], new Dilation(infl));
                }
            }
            ModelTransform tr = (rot[0] != 0 || rot[1] != 0 || rot[2] != 0)
                ? ModelTransform.of(px, py, pz,
                    (float) Math.toRadians(-rot[0]), (float) Math.toRadians(-rot[1]), (float) Math.toRadians(rot[2]))
                : ModelTransform.pivot(px, py, pz);
            ModelPartData mpd = parentData.addChild(name, builder, tr);
            byName.put(name, mpd);
        }

        ModelPart part = TexturedModelData.of(data, tw, th).createModel();

        // texture embarquee -> texture dynamique sure (meme technique que HWIcons)
        Identifier texId = new Identifier("heroworld", "gek_" + key.toLowerCase());
        try (InputStream in = HWGekRegistry.stream(texPath)) {
            NativeImage img = NativeImage.read(in);
            MinecraftClient.getInstance().getTextureManager().registerTexture(texId, new NativeImageBackedTexture(img));
        }

        HWGekModel m = new HWGekModel(part, texId);
        // pre-indexe les bones plats + 1 niveau
        for (String n : byName.keySet()) {
            try {
                ModelPart direct = null;
                try { direct = part.getChild(n); } catch (Throwable ignored) {}
                if (direct == null) {
                    for (String pn : byName.keySet()) {
                        try { direct = part.getChild(pn).getChild(n); } catch (Throwable ignored) {}
                        if (direct != null) break;
                    }
                }
                if (direct != null) m.bones.put(n, direct);
            } catch (Throwable ignored) {}
        }
        return m;
    }
}
