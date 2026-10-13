package mjajka.jajrzeka;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class KomendaJajrzeka implements TabExecutor {

    private final Jajrzeka plugin;

    public KomendaJajrzeka(Jajrzeka plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length >= 1 && a[0].equalsIgnoreCase("paczka") && s instanceof Player p) {
            plugin.paczka().wyslij(p);
            s.sendMessage("§6Jajrzeka §7» wysylam paczke z tekstura");
            return true;
        }
        if (a.length < 1 || !a[0].equalsIgnoreCase("daj")) {
            s.sendMessage("§6Jajrzeka §7» §f/jajrzeka daj [gracz] [ilosc]§7, §f/jajrzeka paczka");
            return true;
        }
        Player cel;
        if (a.length >= 2) {
            cel = Bukkit.getPlayerExact(a[1]);
            if (cel == null) {
                s.sendMessage("§cNie ma takiego gracza");
                return true;
            }
        } else if (s instanceof Player p) {
            cel = p;
        } else {
            s.sendMessage("§cPodaj gracza");
            return true;
        }
        int ile = 1;
        if (a.length >= 3) {
            try {
                ile = Math.max(1, Math.min(64, Integer.parseInt(a[2])));
            } catch (NumberFormatException e) {
                s.sendMessage("§cZla ilosc");
                return true;
            }
        }
        for (int i = 0; i < ile; i++) {
            cel.getInventory().addItem(plugin.napoj().stworz(1)).values()
                    .forEach(x -> cel.getWorld().dropItem(cel.getLocation(), x));
        }
        s.sendMessage("§6Jajrzeka §7» dano §f" + ile + "x §6Jajrzeka §7graczowi §f" + cel.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> w = new ArrayList<>();
        if (a.length == 1) {
            for (String x : List.of("daj", "paczka")) if (x.startsWith(a[0].toLowerCase())) w.add(x);
        } else if (a.length == 2 && a[0].equalsIgnoreCase("daj")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(a[1].toLowerCase())) w.add(p.getName());
            }
        }
        return w;
    }
}
