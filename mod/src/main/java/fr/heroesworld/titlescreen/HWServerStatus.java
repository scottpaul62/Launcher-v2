package fr.heroesworld.titlescreen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Statut du serveur HEROES-WORLD en direct (Server List Ping, asynchrone, toutes les 10 s). */
public final class HWServerStatus {
    public static volatile boolean online = false;
    public static volatile int playersOn = 0, playersMax = 0;
    private static volatile long lastPing = 0;
    private static volatile boolean running = false;
    private HWServerStatus() {}

    /** A appeler depuis render() : lance un ping en arriere-plan si le dernier date de plus de 10 s. */
    public static void tick() {
        long now = System.currentTimeMillis();
        if (running || now - lastPing < 10000) return;
        running = true;
        Thread th = new Thread(() -> {
            try { ping(); } catch (Throwable t) { online = false; }
            finally { lastPing = System.currentTimeMillis(); running = false; }
        }, "HW-ServerPing");
        th.setDaemon(true);
        th.start();
    }

    private static void ping() throws Exception {
        String addr = HWConfig.address();
        String host = addr; int port = 25565;
        int i = addr.lastIndexOf(':');
        if (i > 0) { host = addr.substring(0, i); try { port = Integer.parseInt(addr.substring(i + 1)); } catch (Exception ignored) {} }
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 1500);
            s.setSoTimeout(1500);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            DataInputStream in = new DataInputStream(s.getInputStream());
            byte[] hb = host.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream pb = new ByteArrayOutputStream();
            DataOutputStream pd = new DataOutputStream(pb);
            pd.write(0x00); writeVarInt(pd, 765); writeVarInt(pd, hb.length); pd.write(hb); pd.writeShort(port); writeVarInt(pd, 1);
            writeVarInt(out, pb.size()); out.write(pb.toByteArray());
            out.write(new byte[]{1, 0}); // status request
            readVarInt(in); readVarInt(in);
            int len = readVarInt(in);
            if (len <= 0 || len > 1048576) throw new Exception("taille invalide");
            byte[] data = new byte[len];
            in.readFully(data);
            JsonObject o = new Gson().fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject pl = o != null ? o.getAsJsonObject("players") : null;
            playersOn = (pl != null && pl.has("online")) ? pl.get("online").getAsInt() : 0;
            playersMax = (pl != null && pl.has("max")) ? pl.get("max").getAsInt() : 0;
            online = true;
        }
    }

    private static void writeVarInt(DataOutputStream out, int v) throws Exception {
        while ((v & ~0x7F) != 0) { out.write((v & 0x7F) | 0x80); v >>>= 7; }
        out.write(v);
    }
    private static int readVarInt(DataInputStream in) throws Exception {
        int r = 0, n = 0;
        while (true) {
            int b = in.readUnsignedByte();
            r |= (b & 0x7F) << (7 * n);
            if ((b & 0x80) == 0) return r;
            if (++n > 5) throw new Exception("varint");
        }
    }
}
