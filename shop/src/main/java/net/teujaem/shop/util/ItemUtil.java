package net.teujaem.shop.util;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {

    private ItemUtil() {
    }

    public static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        rename(item, name);
        return item;
    }

    public static void rename(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.color(name));
            item.setItemMeta(meta);
        }
    }

    public static void setLore(ItemStack item, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(MessageUtil.color(line));
        }
        meta.setLore(colored);
        item.setItemMeta(meta);
    }

    public static List<String> getLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return new ArrayList<>();
        return new ArrayList<>(meta.getLore());
    }

    public static ItemStack glass(Material material, String name) {
        return named(material, name);
    }

    public static ItemStack playerHead(OfflinePlayer player, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(MessageUtil.color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(MessageUtil.color(line));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String displayName(ItemStack item) {
        if (item == null) return "";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name();
    }
}
