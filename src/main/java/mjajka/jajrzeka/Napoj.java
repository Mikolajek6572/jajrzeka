package mjajka.jajrzeka;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Tworzy item napoju i rozpoznaje go po znaczniku w PDC */
public final class Napoj {

    private final NamespacedKey znacznik;

    public Napoj(Jajrzeka plugin) {
        this.znacznik = new NamespacedKey(plugin, "napoj");
    }

    public ItemStack stworz(int ilosc) {
        ItemStack it = new ItemStack(Material.POTION, ilosc);
        PotionMeta m = (PotionMeta) it.getItemMeta();
        m.setColor(Color.fromRGB(255, 140, 80));
        m.setItemModel(new NamespacedKey("jajrzeka", "napoj"));
        m.itemName(Component.text("Jajrzeka", TextColor.color(0xFF9A5A)).decorate(TextDecoration.BOLD));
        m.lore(List.of(
                linia(Component.text("Smak: ", NamedTextColor.GRAY)
                        .append(Component.text("Jabłko", TextColor.color(0xE84A3A)))
                        .append(Component.text(" Brzoskwinia", TextColor.color(0xFFB070)))),
                linia(Component.text("Napój z jajecznej rzeki", NamedTextColor.DARK_GRAY)),
                linia(Component.empty()),
                linia(Component.text("Po wypiciu:", NamedTextColor.GRAY)),
                linia(Component.text(" + Regeneracja, Szybkość", NamedTextColor.GREEN)),
                linia(Component.text(" + Wyskok, Absorpcja", NamedTextColor.GREEN)),
                linia(Component.text(" + Najedzenie", NamedTextColor.GOLD))
        ));
        m.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        m.getPersistentDataContainer().set(znacznik, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    public boolean czyNapoj(ItemStack it) {
        if (it == null || it.getType() != Material.POTION || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(znacznik, PersistentDataType.BYTE);
    }

    private static Component linia(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }
}
