package net.teujaem.usershop.listener;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.teujaem.money.api.Money;
import net.teujaem.spFramework.api.ProxyData;
import net.teujaem.usershop.UserShop;
import net.teujaem.usershop.gui.GuiBuilder;
import net.teujaem.usershop.gui.ShopHolder;
import net.teujaem.usershop.gui.StorageHolder;
import net.teujaem.usershop.manager.ShopManager;
import net.teujaem.usershop.model.PurchaseInfo;
import net.teujaem.usershop.model.ShopEntry;
import net.teujaem.usershop.model.ShopUpdateInfo;
import net.teujaem.usershop.util.MoneyFormat;
import net.teujaem.usershop.util.PendingPayout;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class GuiListener implements Listener {
    private final UserShop plugin;

    public GuiListener(UserShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof ShopHolder) && !(holder instanceof StorageHolder)) {
            return;
        }
        event.setCancelled(true);
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(event.getView().getTopInventory())) {
            return;
        }
        int slot = event.getSlot();
        HumanEntity human = event.getWhoClicked();
        if (!(human instanceof Player)) {
            return;
        }
        Player player = (Player) human;
        if (slot == 48) {
            this.changePage(player, holder, -1);
            return;
        }
        if (slot == 50) {
            this.changePage(player, holder, 1);
            return;
        }
        if (slot < 0 || slot >= 45) {
            return;
        }
        if (holder instanceof ShopHolder) {
            ShopHolder shopHolder = (ShopHolder) holder;
            this.handlePurchase(player, shopHolder, slot);
        } else {
            this.handleStorageWithdraw(player, (StorageHolder) holder, slot);
        }
    }

    private void changePage(Player player, InventoryHolder holder, int delta) {
        ShopManager shopManager = this.plugin.getShopManager();
        if (holder instanceof ShopHolder) {
            ShopHolder shopHolder = (ShopHolder) holder;
            int maxPage = shopManager.getMaxPage();
            int newPage = this.clamp(shopHolder.getPage() + delta, 1, maxPage);
            player.openInventory(GuiBuilder.buildShop(player, shopManager, newPage));
        } else if (holder instanceof StorageHolder) {
            StorageHolder storageHolder = (StorageHolder) holder;
            UUID owner = storageHolder.getOwnerUuid();
            int wantedPage = storageHolder.getPage() + delta;
            shopManager.getReturnedItems(owner).thenAccept(items -> Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> {
                int maxPage = GuiBuilder.maxPageFor(items.size());
                int newPage = this.clamp(wantedPage, 1, maxPage);
                player.openInventory(GuiBuilder.buildStorage(player, owner, items, newPage));
            }));
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void handlePurchase(Player player, ShopHolder shopHolder, int slot) {
        int oneBasedIndex;
        ShopManager shopManager = this.plugin.getShopManager();
        ShopEntry entry = shopManager.getByIndex(oneBasedIndex = (shopHolder.getPage() - 1) * 45 + slot + 1);
        if (entry == null) {
            return;
        }
        String errorPrefix = this.plugin.getShopConfig().prefixError();
        if (Money.get(player) < entry.getPrice()) {
            player.sendMessage(errorPrefix + " §f돈이 부족하여 구매할 수 없습니다.");
            return;
        }
        Money.remove(player, entry.getPrice());
        this.givePlayerItem(player, entry.getItem().clone());
        shopManager.removeEntry(entry);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        UUID sellerUuid = entry.getSellerUuid();
        // 판매자가 지금 이 서버(구매가 일어난 서버)에 있는지 여부와 관계없이,
        // 판매 대금은 항상 먼저 DB(대기금)에 기록한다. 서버가 재시작되거나
        // 프록시 이벤트가 유실되더라도 돈이 사라지지 않도록 하기 위함이다.
        this.plugin.getShopDatabase().addPending(sellerUuid, player.getName(), entry.getPrice())
                .thenRun(() -> Bukkit.getScheduler().runTask((Plugin) this.plugin,
                        () -> PendingPayout.tryPay(this.plugin, sellerUuid)))
                .exceptionally(t -> {
                    this.plugin.getLogger().warning("정산 대기금 저장 실패: " + t.getMessage());
                    return null;
                });

        this.plugin.broadcastShopUpdate(ShopUpdateInfo.Type.REMOVE, entry.getId(), entry.getSellerUuid());
        this.broadcastPurchase(entry, player);
        this.plugin.refreshOpenInventories();
    }

    private void handleStorageWithdraw(Player player, StorageHolder storageHolder, int slot) {
        ShopManager shopManager = this.plugin.getShopManager();
        UUID owner = storageHolder.getOwnerUuid();
        int oneBasedIndex = (storageHolder.getPage() - 1) * 45 + slot + 1;
        ((CompletableFuture<?>) shopManager.takeReturnedItem(owner, oneBasedIndex)).thenAccept(item -> {
            if (item == null) {
                return;
            }
            Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> {
                this.givePlayerItem(player, (ItemStack) item);
                this.plugin.refreshOpenInventories();
            });
        }).exceptionally(t -> {
            this.plugin.getLogger().warning("보관함 아이템 회수 실패: " + t.getMessage());
            return null;
        });
    }

    private void givePlayerItem(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }

    private void broadcastPurchase(ShopEntry entry, Player buyer) {
        try {
            String itemName = this.displayNameOf(entry.getItem());
            PurchaseInfo info = new PurchaseInfo(entry.getId(), buyer.getUniqueId(), buyer.getName(), entry.getSellerUuid(), entry.getSellerName(), entry.getPrice(), itemName, this.plugin.getShopConfig().getServerName());
            String json = this.plugin.getGson().toJson(info);
            ProxyData.sendToServer("UserShop", "purchase", json);
        } catch (Throwable t) {
            this.plugin.getLogger().warning("ProxyEvent 브로드캐스트 실패(프록시 미설정일 수 있음): " + t.getMessage());
        }
    }

    private String displayNameOf(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name();
    }
}