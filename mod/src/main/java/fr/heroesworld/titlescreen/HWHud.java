package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** HUD du client HERO WORLD affiché en jeu (filigrane, FPS, coordonnées). */
public final class HWHud {
    public static boolean watermark = true;
    public static boolean fps = true;
    public static boolean coords = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private HWHud() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld_hud.json"); }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                JsonObject j = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
                if (j != null) {
                    if (j.has("watermark")) watermark = j.get("watermark").getAsBoolean();
                    if (j.has("fps")) fps = j.get("fps").getAsBoolean();
                    if (j.has("coords")) coords = j.get("coords").getAsBoolean();
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            JsonObject j = new JsonObject();
            j.addProperty("watermark", watermark);
            j.addProperty("fps", fps);
            j.addProperty("coords", coords);
            Files.writeString(file(), GSON.toJson(j), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        try {
            if (mc == null || mc.player == null || ctx == null) return;
            if (mc.options != null && mc.options.hudHidden) return;
            if (mc.currentScreen != null) return;
            TextRenderer tr = mc.textRenderer;
            int x = 4, y = 4;
            if (watermark) { ctx.drawTextWithShadow(tr, Text.literal("§6⚡ §eHERO WORLD"), x, y, 0xFFE8C56A); y += 12; }
            if (fps) { ctx.drawTextWithShadow(tr, Text.literal("§7" + mc.getCurrentFps() + " §8FPS"), x, y, 0xFFFFFFFF); y += 10; }
            if (coords) {
                int px = (int) Math.floor(mc.player.getX());
                int py = (int) Math.floor(mc.player.getY());
                int pz = (int) Math.floor(mc.player.getZ());
                ctx.drawTextWithShadow(tr, Text.literal("§7XYZ §f" + px + " " + py + " " + pz), x, y, 0xFFFFFFFF);
            }
        } catch (Throwable ignored) {}
    }
}
