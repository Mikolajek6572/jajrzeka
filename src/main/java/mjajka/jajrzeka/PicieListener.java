package mjajka.jajrzeka;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public final class PicieListener implements Listener {

    private final Jajrzeka plugin;

    public PicieListener(Jajrzeka plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void picie(PlayerItemConsumeEvent e) {
        if (!plugin.napoj().czyNapoj(e.getItem())) return;
        Player p = e.getPlayer();
        var cfg = plugin.getConfig();
        ConfigurationSection ef = cfg.getConfigurationSection("efekty");
        if (ef != null) {
            for (String nazwa : ef.getKeys(false)) {
                PotionEffectType typ = Registry.EFFECT.get(NamespacedKey.minecraft(nazwa.toLowerCase()));
                List<Integer> w = ef.getIntegerList(nazwa);
                if (typ == null || w.size() < 2) {
                    plugin.getLogger().warning("Zly efekt w configu: " + nazwa);
                    continue;
                }
                p.addPotionEffect(new PotionEffect(typ, w.get(0) * 20, Math.max(0, w.get(1) - 1)));
            }
        }
        p.setFoodLevel(Math.min(20, p.getFoodLevel() + cfg.getInt("jedzenie", 6)));
        p.setSaturation((float) Math.min(p.getFoodLevel(), p.getSaturation() + cfg.getDouble("nasycenie", 8.0)));
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1.2, 0), 12, 0.4, 0.4, 0.4);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1.2f);
    }

    // zeby nikt nie przerobil napoju w statywie
    @EventHandler(ignoreCancelled = true)
    public void warzenie(BrewEvent e) {
        for (int i = 0; i < 3; i++) {
            if (plugin.napoj().czyNapoj(e.getContents().getItem(i))) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
