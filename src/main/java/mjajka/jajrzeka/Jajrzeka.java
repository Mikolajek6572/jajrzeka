package mjajka.jajrzeka;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.io.File;

public final class Jajrzeka extends JavaPlugin {

    private Napoj napoj;
    private Paczka paczka;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        napoj = new Napoj(this);
        paczka = new Paczka(this);
        paczka.start();
        getServer().getPluginManager().registerEvents(paczka, this);
        getServer().getPluginManager().registerEvents(new PicieListener(this), this);
        var kom = new KomendaJajrzeka(this);
        getCommand("jajrzeka").setExecutor(kom);
        getCommand("jajrzeka").setTabCompleter(kom);
        dodajCrafting();
        Aktualizacje akt = new Aktualizacje(this);
        getServer().getPluginManager().registerEvents(akt, this);
        akt.start();
    }

    @Override
    public void onDisable() {
        if (paczka != null) paczka.stop();
        Bukkit.removeRecipe(new NamespacedKey(this, "napoj"));
    }

    /*
     * diament  diament          diament
     * zloto    butelka z woda   wiadro wody
     *          jablko
     */
    private void dodajCrafting() {
        ShapedRecipe r = new ShapedRecipe(new NamespacedKey(this, "napoj"), napoj.stworz(1));
        r.shape("DDD", "ZBW", " J ");
        r.setIngredient('D', Material.DIAMOND);
        r.setIngredient('Z', Material.GOLD_INGOT);
        ItemStack woda = new ItemStack(Material.POTION);
        PotionMeta pm = (PotionMeta) woda.getItemMeta();
        pm.setBasePotionType(PotionType.WATER);
        woda.setItemMeta(pm);
        r.setIngredient('B', new RecipeChoice.ExactChoice(woda));
        r.setIngredient('W', Material.WATER_BUCKET);
        r.setIngredient('J', Material.APPLE);
        Bukkit.addRecipe(r);
    }

    public Napoj napoj() {
        return napoj;
    }

    public Paczka paczka() {
        return paczka;
    }

    public File plikPluginu() {
        return getFile();
    }
}
