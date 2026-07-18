package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/**
 * PHASE 3 : contenu dynamique depuis Cloudflare R2 (systeme de manifest par hash, inspire Lunar).
 * Au demarrage, un thread compare <url>/manifest.json au cache local (config/heroworld-remote/)
 * et ne telecharge QUE les fichiers modifies (sha1). Le contenu deja charge s'applique
 * a la session suivante — jamais de rechargement a chaud risque. Hors-ligne : silencieux.
 * Ajouter/mettre a jour un cosmetique ou une astuce = deposer les fichiers dans le bucket,
 * AUCUNE mise a jour du mod necessaire.
 * Override possible : config/heroworld_remote.json {"url": "https://..."} ("off" pour couper).
 */
public final class HWRemote {
    private static final String DEFAULT_URL = "https://pub-fe9bb829f03342db8feb2852f5d1fda1.r2.dev";
    private static final Gson GSON = new Gson();
    private static volatile boolean started = false;
    private HWRemote() {}

    private static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("heroworld-remote"); }

    private static String baseUrl() {
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("heroworld_remote.json");
            if (Files.exists(f)) {
                JsonObject j = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
                if (j != null && j.has("url")) return j.get("url").getAsString();
            }
        } catch (Throwable ignored) {}
        return DEFAULT_URL;
    }

    /** Ouvre un fichier du cache distant, ou null s'il n'existe pas (repli : ressources embarquees). */
    public static InputStream open(String rel) {
        try {
            if (rel == null || rel.contains("..")) return null;
            Path p = dir().resolve(rel.replace('\\', '/'));
            if (Files.isRegularFile(p)) return Files.newInputStream(p);
        } catch (Throwable ignored) {}
        return null;
    }

    /** Lance la synchronisation en arriere-plan (jamais bloquant, jamais de crash). */
    public static void start() {
        if (started) return;
        started = true;
        String url = baseUrl();
        if (url == null || url.isBlank() || url.equalsIgnoreCase("off")) return;
        Thread t = new Thread(() -> sync(url), "HW-Remote-Sync");
        t.setDaemon(true);
        t.start();
    }

    private static void sync(String base) {
        try {
            JsonObject man = fetchJson(base + "/manifest.json");
            if (man == null || !man.has("files")) { HWHudManager.LOG.info("[REMOTE] pas de manifest (hors-ligne ?)"); return; }
            JsonArray files = man.getAsJsonArray("files");
            int fresh = 0, kept = 0, failed = 0;
            for (int i = 0; i < files.size(); i++) {
                JsonObject f = files.get(i).getAsJsonObject();
                String path = f.get("path").getAsString();
                String sha1 = f.get("sha1").getAsString();
                if (path.contains("..") || path.startsWith("/")) continue;
                Path local = dir().resolve(path);
                try {
                    if (Files.isRegularFile(local) && sha1.equalsIgnoreCase(sha1Of(local))) { kept++; continue; }
                    Files.createDirectories(local.getParent());
                    Path tmp = local.resolveSibling(local.getFileName() + ".tmp");
                    download(base + "/" + path, tmp);
                    if (!sha1.equalsIgnoreCase(sha1Of(tmp))) { Files.deleteIfExists(tmp); failed++; continue; }
                    Files.move(tmp, local, StandardCopyOption.REPLACE_EXISTING);
                    fresh++;
                } catch (Throwable e) { failed++; }
            }
            HWHudManager.LOG.info("[REMOTE] contenu synchronise : {} a jour, {} telecharges, {} echecs{}",
                kept, fresh, failed, fresh > 0 ? " (appliques au prochain demarrage)" : "");
        } catch (Throwable t) {
            HWHudManager.LOG.info("[REMOTE] synchronisation ignoree : {}", t.toString());
        }
    }

    private static JsonObject fetchJson(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
            c.setConnectTimeout(6000); c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "HeroWorld/" + HWBrand.VERSION);
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream()) {
                return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            }
        } catch (Throwable t) { return null; }
    }

    private static void download(String url, Path to) throws Exception {
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setConnectTimeout(6000); c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "HeroWorld/" + HWBrand.VERSION);
        if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
        try (InputStream in = c.getInputStream()) {
            Files.copy(in, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha1Of(Path p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream in = Files.newInputStream(p)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
