package net.teujaem.usershop.gui;

import net.teujaem.money.api.Money;
import net.teujaem.usershop.manager.ShopManager;
import net.teujaem.usershop.model.ShopEntry;
import net.teujaem.usershop.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GuiBuilder {

    public static final String SHOP_TITLE = ChatColor.translateAlternateColorCodes('&', "&c유저상점");
    public static final String STORAGE_TITLE = ChatColor.translateAlternateColorCodes('&', "&c반환된 아이템");

    private static final ItemStack GLASS = pane(Material.GRAY_STAINED_GLASS_PANE, "&f");
    private static final ItemStack PREV_TEMPLATE = pane(Material.RED_STAINED_GLASS_PANE, null);
    private static final ItemStack PAGE_TEMPLATE = pane(Material.WHITE_STAINED_GLASS_PANE, null);
    private static final ItemStack NEXT_TEMPLATE = pane(Material.GREEN_STAINED_GLASS_PANE, null);

    private GuiBuilder() {
    }

    private static ItemStack pane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        if (name != null) {
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** 하단 컨트롤 바(45~53)를 공통으로 채웁니다. */
    private static void fillControlBar(Inventory inv, Player viewer, int page) {
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, GLASS.clone());
        }

        ItemStack prev = PREV_TEMPLATE.clone();
        ItemMeta prevMeta = prev.getItemMeta();
        prevMeta.setDisplayName(c("&c이전 페이지 (" + (page - 1) + ")"));
        prev.setItemMeta(prevMeta);
        inv.setItem(48, prev);

        ItemStack pageItem = PAGE_TEMPLATE.clone();
        ItemMeta pageMeta = pageItem.getItemMeta();
        pageMeta.setDisplayName(c("&b" + page));
        pageItem.setItemMeta(pageMeta);
        inv.setItem(49, pageItem);

        ItemStack next = NEXT_TEMPLATE.clone();
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(c("&2다음 페이지 (" + (page + 1) + ")"));
        next.setItemMeta(nextMeta);
        inv.setItem(50, next);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(viewer);
        skullMeta.setDisplayName(c("&5&l정보"));
        List<String> lore = new ArrayList<>();
        lore.add(c("&7닉네임 &f: &2" + viewer.getName()));
        lore.add(c("&a보유금액 &f: &6" + MoneyFormat.format(Money.get(viewer))));
        skullMeta.setLore(lore);
        head.setItemMeta(skullMeta);
        inv.setItem(53, head);
    }

    /** 상점(구매) GUI 인벤토리를 만듭니다. */
    public static Inventory buildShop(Player viewer, ShopManager shopManager, int page) {
        ShopHolder holder = new ShopHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54, SHOP_TITLE);
        holder.setInventory(inv);

        List<ShopEntry> entries = shopManager.getEntries();
        int start = (page - 1) * 45;
        for (int slot = 0; slot < 45; slot++) {
            int index = start + slot;
            if (index >= entries.size()) continue;
            ShopEntry entry = entries.get(index);

            ItemStack display = entry.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(c("&a&l판매아이템"));
            lore.add("");
            lore.add(c("&c&l가격 &f: &e&l" + MoneyFormat.format(entry.getPrice()) + "원"));
            lore.add(c("&c&l판매자 &f: &2&l" + entry.getSellerName()));
            lore.add(c("&c&l남은시간 &f: &b&l" + MoneyFormat.remainingTime(entry.getExpireAt() - System.currentTimeMillis())));
            meta.setLore(lore);
            display.setItemMeta(meta);

            inv.setItem(slot, display);
        }

        fillControlBar(inv, viewer, page);
        return inv;
    }

    /**
     * 반환된 아이템 보관함 GUI 인벤토리를 만듭니다.
     * 보관함은 DB에서 비동기로 가져오므로, 호출하는 쪽(명령어/리스너)에서
     * 미리 조회한 아이템 목록(items)을 메인 스레드에서 이 메서드로 넘겨줘야 합니다.
     */
    public static Inventory buildStorage(Player viewer, UUID ownerUuid, List<ItemStack> items, int page) {
        StorageHolder holder = new StorageHolder(ownerUuid, page);
        Inventory inv = Bukkit.createInventory(holder, 54, STORAGE_TITLE);
        holder.setInventory(inv);

        int start = (page - 1) * 45;
        for (int slot = 0; slot < 45; slot++) {
            int index = start + slot;
            if (index >= items.size()) continue;
            inv.setItem(slot, items.get(index).clone());
        }

        fillControlBar(inv, viewer, page);
        return inv;
    }

    public static int maxPageFor(int itemCount) {
        return Math.max(1, (int) Math.ceil(itemCount / 45.0));
    }
}
