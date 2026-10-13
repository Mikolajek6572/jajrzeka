package mjajka.jajrzeka;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sprawdza najnowsze wydanie na GitHubie i pobiera nowy jar do plugins/update.
 * Paper podmienia go przy nastepnym starcie serwera.
 */
public final class Aktualizacje implements Listener {

    private static final String API = "https://api.github.com/repos/Mikolajek6572/jajrzeka/releases/latest";
    private static final Pattern JAR = Pattern.compile("^Jajrzeka-(\\d+(?:\\.\\d+)*)\\.jar$");

    private final Jajrzeka plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private volatile String pobrana;

    public Aktualizacje(Jajrzeka plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("aktualizacje.auto", true)) return;
        long co = Math.max(1, plugin.getConfig().getLong("aktualizacje.co-ile-godzin", 6)) * 20L * 60 * 60;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sprawdz, 20L * 10, co);
    }

    private void sprawdz() {
        try {
            HttpResponse<String> odp = http.send(HttpRequest.newBuilder(URI.create(API))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Jajrzeka-plugin")
                    .timeout(Duration.ofSeconds(20))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            if (odp.statusCode() != 200) {
                plugin.getLogger().warning("Nie moge sprawdzic aktualizacji (GitHub " + odp.statusCode() + ")");
                return;
            }
            JsonObject wydanie = JsonParser.parseString(odp.body()).getAsJsonObject();
            String teraz = plugin.getPluginMeta().getVersion();
            for (JsonElement el : wydanie.getAsJsonArray("assets")) {
                JsonObject a = el.getAsJsonObject();
                Matcher m = JAR.matcher(a.get("name").getAsString());
                if (!m.matches()) continue;
                String nowa = m.group(1);
                if (porownaj(nowa, teraz) <= 0 || nowa.equals(pobrana)) return;
                pobierz(a.get("browser_download_url").getAsString(), nowa);
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Nie moge sprawdzic aktualizacji: " + e.getMessage());
        }
    }

    private void pobierz(String url, String wersja) throws Exception {
        File folder = plugin.getServer().getUpdateFolderFile();
        folder.mkdirs();
        // Paper podmienia jar z update/ ktory ma taka sama nazwe jak obecny
        Path cel = new File(folder, plugin.plikPluginu().getName()).toPath();
        Path tmp = new File(folder, "Jajrzeka-pobieranie.tmp").toPath();
        HttpResponse<Path> odp = http.send(HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Jajrzeka-plugin")
                .timeout(Duration.ofMinutes(2))
                .GET().build(), HttpResponse.BodyHandlers.ofFile(tmp));
        if (odp.statusCode() != 200 || Files.size(tmp) < 1000) {
            Files.deleteIfExists(tmp);
            plugin.getLogger().warning("Nie udalo sie pobrac Jajrzeka " + wersja + " (" + odp.statusCode() + ")");
            return;
        }
        Files.move(tmp, cel, StandardCopyOption.REPLACE_EXISTING);
        pobrana = wersja;
        plugin.getLogger().info("Pobrano Jajrzeka " + wersja + " - wejdzie po restarcie serwera");
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) powiadom(p);
        });
    }

    private void powiadom(Player p) {
        if (pobrana != null && p.hasPermission("jajrzeka.daj")) {
            p.sendMessage("§6Jajrzeka §7» pobrano nowa wersje §f" + pobrana + "§7, wejdzie po restarcie serwera");
        }
    }

    @EventHandler
    public void wejscie(PlayerJoinEvent e) {
        powiadom(e.getPlayer());
    }

    /** porownuje wersje typu 1.0.10 i 1.0.9 po liczbach */
    static int porownaj(String a, String b) {
        String[] x = a.split("\\."), y = b.split("[.-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? liczba(x[i]) : 0;
            int q = i < y.length ? liczba(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int liczba(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
