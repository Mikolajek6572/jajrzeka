package mjajka.jajrzeka;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Sklada paczke z tekstura napoju, hostuje ja po HTTP i wysyla graczom przy wejsciu */
public final class Paczka implements Listener {

    private static final UUID ID_PACZKI = UUID.nameUUIDFromBytes("Jajrzeka-paczka".getBytes(StandardCharsets.UTF_8));
    private static final String PREFIKS = "paczka/";

    private final Jajrzeka plugin;
    private volatile byte[] zip;
    private byte[] sha1;
    private String sha1Hex;
    private String url;
    private HttpServer serwer;
    private ExecutorService watki;

    public Paczka(Jajrzeka plugin) {
        this.plugin = plugin;
    }

    public void start() {
        var cfg = plugin.getConfig();
        try {
            zbuduj();
        } catch (Exception e) {
            plugin.getLogger().severe("Nie udalo sie zbudowac paczki: " + e);
            return;
        }
        if (!cfg.getBoolean("paczka.wlaczona", true)) return;
        String zew = cfg.getString("paczka.url-zewnetrzny", "");
        if (zew != null && !zew.isBlank()) {
            url = zew.trim();
            plugin.getLogger().info("Paczka z zewnetrznego linku: " + url + " (sha1 " + sha1Hex + ")");
            return;
        }
        int port = cfg.getInt("paczka.port", 8766);
        try {
            serwer = HttpServer.create(new InetSocketAddress(port), 0);
            serwer.createContext("/", this::obsluz);
            watki = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "Jajrzeka-paczka");
                t.setDaemon(true);
                return t;
            });
            serwer.setExecutor(watki);
            serwer.start();
            url = "http://" + adres(cfg.getString("paczka.adres", "auto")) + ":" + port + "/Jajrzeka-" + sha1Hex.substring(0, 8) + ".zip";
            plugin.getLogger().info("Paczka hostowana pod " + url);
        } catch (IOException e) {
            plugin.getLogger().warning("Nie moge otworzyc portu " + port + " na paczke (" + e.getMessage()
                    + "). Zmien paczka.port albo ustaw paczka.url-zewnetrzny w configu");
        }
    }

    public void stop() {
        if (serwer != null) serwer.stop(0);
        if (watki != null) watki.shutdownNow();
        serwer = null;
        watki = null;
    }

    private void obsluz(HttpExchange ex) throws IOException {
        try (ex) {
            byte[] dane = zip;
            if (dane == null || !"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
            ex.getResponseHeaders().add("Content-Type", "application/zip");
            ex.sendResponseHeaders(200, dane.length);
            try (OutputStream o = ex.getResponseBody()) {
                o.write(dane);
            }
        }
    }

    private static String adres(String zConfigu) {
        if (zConfigu != null && !zConfigu.isBlank() && !zConfigu.equalsIgnoreCase("auto")) return zConfigu.trim();
        String ip = Bukkit.getIp();
        if (ip != null && !ip.isBlank() && !ip.equals("0.0.0.0")) return ip;
        try {
            Enumeration<NetworkInterface> it = NetworkInterface.getNetworkInterfaces();
            while (it.hasMoreElements()) {
                NetworkInterface ni = it.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> adresy = ni.getInetAddresses();
                while (adresy.hasMoreElements()) {
                    InetAddress a = adresy.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    public void wyslij(Player p) {
        if (url == null || sha1 == null) return;
        p.setResourcePack(ID_PACZKI, url, sha1,
                Component.text("Tekstura napoju Jajrzeka", NamedTextColor.GOLD),
                plugin.getConfig().getBoolean("paczka.wymagana", false));
    }

    @EventHandler
    public void wejscie(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) wyslij(p);
        }, 20L);
    }

    private void zbuduj() throws Exception {
        Map<String, byte[]> pliki = new TreeMap<>();
        try (JarFile jar = new JarFile(plugin.plikPluginu())) {
            var it = jar.entries();
            while (it.hasMoreElements()) {
                JarEntry je = it.nextElement();
                if (je.isDirectory() || !je.getName().startsWith(PREFIKS)) continue;
                pliki.put(je.getName().substring(PREFIKS.length()), jar.getInputStream(je).readAllBytes());
            }
        }
        // wlasna tekstura z plugins/Jajrzeka/napoj.png nadpisuje te z jara
        File wlasna = new File(plugin.getDataFolder(), "napoj.png");
        if (wlasna.isFile()) {
            pliki.put("assets/jajrzeka/textures/item/napoj.png", Files.readAllBytes(wlasna.toPath()));
        }
        pliki.put("pack.mcmeta", utf("{\"pack\":{\"description\":\"§6Jajrzeka §7- Jabłko Brzoskwinia\",\"min_format\":75,\"max_format\":75}}"));
        pliki.put("assets/jajrzeka/items/napoj.json",
                utf("{\"model\":{\"type\":\"minecraft:model\",\"model\":\"jajrzeka:item/napoj\"}}"));
        pliki.put("assets/jajrzeka/models/item/napoj.json",
                utf("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"jajrzeka:item/napoj\"}}"));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        LocalDateTime staly = LocalDateTime.of(2026, 1, 1, 0, 0);
        try (ZipOutputStream z = new ZipOutputStream(bout)) {
            for (var e : pliki.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                ze.setTimeLocal(staly);
                z.putNextEntry(ze);
                z.write(e.getValue());
                z.closeEntry();
            }
        }
        byte[] dane = bout.toByteArray();
        sha1 = MessageDigest.getInstance("SHA-1").digest(dane);
        sha1Hex = HexFormat.of().formatHex(sha1);
        zip = dane;
        plugin.getDataFolder().mkdirs();
        Files.write(new File(plugin.getDataFolder(), "Jajrzeka-paczka.zip").toPath(), dane);
        plugin.getLogger().info("Paczka zbudowana, sha1 " + sha1Hex);
    }

    private static byte[] utf(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
