package net.teujaem.shop.listener;

import net.teujaem.shop.Shop;
import net.teujaem.shop.economy.EconomyService;
import net.teujaem.shop.gui.GuiFactory;
import net.teujaem.shop.gui.GuiType;
import net.teujaem.shop.gui.PlayerSession;
import net.teujaem.shop.gui.ShopInventoryHolder;
import net.teujaem.shop.manager.ShopManager;
import net.teujaem.shop.model.PriceAction;
import net.teujaem.shop.model.PriceType;
import net.teujaem.shop.model.ShopItem;
import net.teujaem.shop.model.ShopType;
import net.teujaem.shop.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShopGuiListener implements Listener {

    private final Shop plugin;
    private final ShopManager shopManager;
    private final GuiFactory guiFactory;
    private final EconomyService economyService;

    public ShopGuiListener(Shop plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getShopManager();
        this.guiFactory = plugin.getGuiFactory();
        this.economyService = plugin.getEconomyService();
    }

    // =========================================================
    //  클릭 이벤트
    // =========================================================

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        ShopInventoryHolder holder = GuiFactory.holderOf(event.getView().getTopInventory());
        if (holder == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        boolean topClick = event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getView().getTopInventory());
        int slot = event.getSlot();

        switch (holder.getType()) {
            case MAIN:
                event.setCancelled(true);
                if (topClick) handleMainClick(player, holder, slot, event.getClick());
                break;
            case ITEM_EDIT:
                if (!topClick) return; // 플레이어 인벤토리는 자유롭게 조작 가능
                if (slot < GuiFactory.ITEMS_PER_PAGE) return; // 편집 구역은 자유롭게 아이템 배치 가능
                event.setCancelled(true);
                handleEditNav(player, holder, slot, GuiType.ITEM_EDIT, null);
                break;
            case PRICE_EDIT:
                event.setCancelled(true);
                if (!topClick) return;
                if (slot < GuiFactory.ITEMS_PER_PAGE) {
                    handlePriceEditClick(player, holder, slot, event.getClick());
                } else {
                    handleEditNav(player, holder, slot, GuiType.PRICE_EDIT, holder.getPriceType());
                }
                break;
            case STOCK_EDIT:
                event.setCancelled(true);
                if (!topClick) return;
                if (slot < GuiFactory.ITEMS_PER_PAGE) {
                    handleStockEditClick(player, holder, slot, event.getClick());
                } else {
                    handleEditNav(player, holder, slot, GuiType.STOCK_EDIT, null);
                }
                break;
            case TRADE_EDIT:
                event.setCancelled(true);
                if (!topClick) return;
                if (slot < GuiFactory.ITEMS_PER_PAGE) {
                    handleTradeEditClick(player, holder, slot);
                } else {
                    handleEditNav(player, holder, slot, GuiType.TRADE_EDIT, null);
                }
                break;
            case PRICE_SETTING:
            case STOCK_SETTING:
                event.setCancelled(true);
                if (topClick) handleKeypadClick(player, holder, slot);
                break;
            case TRADE_SETTING:
                if (topClick && slot >= 9) {
                    event.setCancelled(true);
                    if (slot == 13) handleTradeSettingConfirm(player, holder);
                }
                break;
        }
    }

    // ---------------- 메인 상점 (구매/판매/교환) ----------------

    private void handleMainClick(Player player, ShopInventoryHolder holder, int slot, ClickType click) {
        net.teujaem.shop.model.Shop shop = shopManager.get(holder.getShopId()).orElse(null);
        if (shop == null) return;

        if (slot == GuiFactory.SLOT_PREV_PAGE) {
            openMain(player, shop, holder.getPage() - 1);
            return;
        }
        if (slot == GuiFactory.SLOT_NEXT_PAGE) {
            openMain(player, shop, holder.getPage() + 1);
            return;
        }
        if (slot >= GuiFactory.ITEMS_PER_PAGE) return;

        int index = (holder.getPage() - 1) * GuiFactory.ITEMS_PER_PAGE + slot + 1;
        ShopItem shopItem = shop.getItem(index);
        if (shopItem == null || shopItem.getDisplayItem() == null) return;
        ItemStack sample = shopItem.getDisplayItem();

        if (shopItem.hasLimitedStock() && shopItem.getStockNow() == null) {
            error(player, "해당 아이템의 재고가 없습니다.");
            return;
        }

        if (shop.getType() == ShopType.TRADE) {
            handleTradeBuy(player, shop, holder, shopItem, sample, click);
        } else {
            handleNormalTrade(player, shop, holder, shopItem, sample, click);
        }
    }

    private void handleNormalTrade(Player player, net.teujaem.shop.model.Shop shop, ShopInventoryHolder holder,
                                   ShopItem shopItem, ItemStack sample, ClickType click) {
        boolean sell;
        int amount;
        if (click == ClickType.LEFT) {
            sell = false;
            amount = 1;
        } else if (click == ClickType.RIGHT) {
            sell = true;
            amount = 1;
            if (countMatching(player.getInventory(), sample) < 1) {
                error(player, "당신은 해당 아이템을 가지고 있지 않습니다.");
                return;
            }
        } else if (click == ClickType.SHIFT_LEFT) {
            sell = false;
            amount = 64;
        } else if (click == ClickType.SHIFT_RIGHT) {
            sell = true;
            amount = countMatching(player.getInventory(), sample);
            if (amount <= 0) {
                error(player, "당신은 해당 아이템을 가지고 있지 않습니다.");
                return;
            }
        } else {
            return;
        }

        PriceType priceType = sell ? PriceType.SELL : PriceType.BUY;
        boolean useNow = shop.getType() == ShopType.CHANGE;
        Integer unitPrice = shopItem.getPrice(priceType, useNow ? PriceAction.NOW : PriceAction.DEFAULT);
        if (unitPrice == null || unitPrice <= 0) {
            error(player, "해당 아이템은 " + priceType.displayName() + "하실 수 없습니다.");
            return;
        }
        int totalPrice = unitPrice * amount;

        if (!sell) {
            if (totalPrice > economyService.getBalance(player)) {
                error(player, "당신은 돈이 없으므로 해당 아이템을 구매하실 수 없습니다.");
                return;
            }
            int giveAmount = amount * sample.getAmount();
            if (freeCapacityFor(player.getInventory(), sample) < giveAmount) {
                error(player, "당신은 인벤토리에 공간이 없으므로 해당 아이템을 구매하실 수 없습니다.");
                return;
            }
            economyService.withdraw(player, totalPrice);
            giveItems(player, sample, giveAmount);
            shop(player, "아이템을 구매하셨습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            if (shopItem.hasLimitedStock()) {
                int now = shopItem.getStockNow() == null ? 0 : shopItem.getStockNow();
                now -= giveAmount;
                shopItem.setStockNow(now <= 0 ? null : now);
            }
        } else {
            removeMatching(player.getInventory(), sample, amount);
            economyService.deposit(player, totalPrice);
            shop(player, "아이템을 판매하셨습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            if (shop.isStockAddOnUserSell() && shopItem.hasLimitedStock()) {
                int now = shopItem.getStockNow() == null ? 0 : shopItem.getStockNow();
                shopItem.setStockNow(now + amount);
            }
        }
        shopManager.save();
        openMain(player, shop, holder.getPage());
    }

    private void handleTradeBuy(Player player, net.teujaem.shop.model.Shop shop, ShopInventoryHolder holder,
                                ShopItem shopItem, ItemStack sample, ClickType click) {
        int amount;
        if (click == ClickType.LEFT) {
            amount = 1;
        } else if (click == ClickType.SHIFT_LEFT) {
            amount = 64;
        } else if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            error(player, "거래 상점에서는 판매할 수 없습니다.");
            return;
        } else {
            return;
        }

        if (!shopItem.hasTradeRequirements()) {
            error(player, "해당 아이템은 구매하실 수 없습니다.");
            return;
        }

        for (ItemStack requirement : shopItem.getTradeRequirements()) {
            int needed = requirement.getAmount() * amount;
            if (countMatching(player.getInventory(), requirement) < needed) {
                error(player, "당신은 아이템이 없으므로 거래하실 수 없습니다.");
                return;
            }
        }
        for (ItemStack requirement : shopItem.getTradeRequirements()) {
            removeMatching(player.getInventory(), requirement, requirement.getAmount() * amount);
        }
        int giveAmount = amount * sample.getAmount();
        giveItems(player, sample, giveAmount);
        shop(player, "아이템을 거래하셨습니다.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        openMain(player, shop, holder.getPage());
    }

    // ---------------- 가격 편집 ----------------

    private void handlePriceEditClick(Player player, ShopInventoryHolder holder, int slot, ClickType click) {
        net.teujaem.shop.model.Shop shop = shopManager.get(holder.getShopId()).orElse(null);
        if (shop == null) return;
        int index = (holder.getPage() - 1) * GuiFactory.ITEMS_PER_PAGE + slot + 1;
        ShopItem shopItem = shop.getItem(index);
        if (shopItem == null || shopItem.getDisplayItem() == null) return;

        PlayerSession session = plugin.getSessionManager().get(player.getUniqueId());
        Long pending = session.getPendingNumber();
        if (pending == null) return; // 값을 먼저 입력해야 함
        int amount = (int) (long) pending;

        PriceAction action;
        switch (click) {
            case LEFT: action = PriceAction.DEFAULT; break;
            case RIGHT: action = PriceAction.NOW; break;
            case SHIFT_LEFT: action = PriceAction.MIN; break;
            case SHIFT_RIGHT: action = PriceAction.MAX; break;
            default: return;
        }
        applyPrice(player, shop, shopItem, index, holder.getPriceType(), action, amount);
        shopManager.save();
        holder.setChangingPage(true);
        openPriceEdit(player, shop, holder.getPage(), holder.getPriceType());
    }

    private void applyPrice(Player player, net.teujaem.shop.model.Shop shop, ShopItem shopItem, int index,
                            PriceType type, PriceAction action, int amount) {
        String typeKr = type.displayName();
        if (amount != 0) {
            if (action == PriceAction.MIN) {
                Integer min = shopItem.getPrice(type, PriceAction.MIN);
                if (min != null && min > amount) {
                    error(player, "최대 가격이 최소 가격보다 낮을 수 없습니다.");
                    return;
                }
            }
            if (action == PriceAction.MAX) {
                Integer min = shopItem.getPrice(type, PriceAction.MIN);
                if (min != null && min > amount) {
                    error(player, "최대 가격이 최소 가격보다 낮을 수 없습니다.");
                    return;
                }
            }
            shopItem.setPrice(type, action, amount);
            shop(player, "해당 슬롯의 " + typeKr + " " + priceActionLabel(action) + "이(가) " + amount + "원으로 설정되었습니다.");
        } else {
            shopItem.setPrice(type, action, null);
            if (action == PriceAction.DEFAULT) {
                shop(player, "해당 슬롯을 " + typeKr + " 불가로 설정했습니다.");
            } else {
                shop(player, "해당 슬롯의 " + typeKr + " " + priceActionLabel(action) + "이(가) 초기화 되었습니다.");
            }
        }
    }

    private String priceActionLabel(PriceAction action) {
        switch (action) {
            case DEFAULT: return "기본 가격";
            case NOW: return "현재 가격";
            case MIN: return "최소 가격";
            case MAX: return "최대 가격";
            default: return "";
        }
    }

    // ---------------- 재고 편집 ----------------

    private void handleStockEditClick(Player player, ShopInventoryHolder holder, int slot, ClickType click) {
        net.teujaem.shop.model.Shop shop = shopManager.get(holder.getShopId()).orElse(null);
        if (shop == null) return;
        int index = (holder.getPage() - 1) * GuiFactory.ITEMS_PER_PAGE + slot + 1;
        ShopItem shopItem = shop.getItem(index);
        if (shopItem == null || shopItem.getDisplayItem() == null) return;

        PlayerSession session = plugin.getSessionManager().get(player.getUniqueId());
        Long pending = session.getPendingNumber();
        if (pending == null) return;
        int amount = (int) (long) pending;

        if (click == ClickType.LEFT) {
            shopItem.setStockDefault(amount == 0 ? null : amount);
            shop(player, "해당 슬롯의 기본 재고가 " + amount + "으로 설정되었습니다.");
        } else if (click == ClickType.RIGHT) {
            shopItem.setStockNow(amount == 0 ? null : amount);
            shop(player, "해당 슬롯의 현재 재고가 " + amount + "으로 설정되었습니다.");
        } else {
            return;
        }
        shopManager.save();
        holder.setChangingPage(true);
        openStockEdit(player, shop, holder.getPage());
    }

    // ---------------- 거래 편집 ----------------

    private void handleTradeEditClick(Player player, ShopInventoryHolder holder, int slot) {
        net.teujaem.shop.model.Shop shop = shopManager.get(holder.getShopId()).orElse(null);
        if (shop == null) return;
        int index = (holder.getPage() - 1) * GuiFactory.ITEMS_PER_PAGE + slot + 1;
        ShopItem shopItem = shop.getItem(index);
        if (shopItem == null || shopItem.getDisplayItem() == null) return;

        PlayerSession session = plugin.getSessionManager().get(player.getUniqueId());
        shopItem.setTradeRequirements(new ArrayList<>(session.getPendingTradeItems()));
        shopManager.save();
        shop(player, "해당 슬롯의 교환아이템을 설정했습니다.");
    }

    // ---------------- 페이지 이동(관리자 GUI 공용) ----------------

    private void handleEditNav(Player player, ShopInventoryHolder holder, int slot, GuiType type, PriceType priceType) {
        net.teujaem.shop.model.Shop shop = shopManager.get(holder.getShopId()).orElse(null);
        if (shop == null) return;
        int newPage = holder.getPage();
        if (slot == GuiFactory.SLOT_PREV_PAGE) newPage--;
        else if (slot == GuiFactory.SLOT_NEXT_PAGE) newPage++;
        else return;

        holder.setChangingPage(true);
        switch (type) {
            case ITEM_EDIT: openItemEdit(player, shop, newPage); break;
            case PRICE_EDIT: openPriceEdit(player, shop, newPage, priceType); break;
            case STOCK_EDIT: openStockEdit(player, shop, newPage); break;
            case TRADE_EDIT: openTradeEdit(player, shop, newPage); break;
            default: break;
        }
    }

    // ---------------- 숫자 키패드 ----------------

    private void handleKeypadClick(Player player, ShopInventoryHolder holder, int slot) {
        PlayerSession session = plugin.getSessionManager().get(player.getUniqueId());
        int[] digitSlots = {21, 22, 23, 30, 31, 32, 39, 40, 41};
        String[] digits = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
        for (int i = 0; i < digitSlots.length; i++) {
            if (slot == digitSlots[i]) {
                session.appendDigit(digits[i].charAt(0));
                refreshKeypad(player, session);
                return;
            }
        }
        if (slot == 49) {
            session.appendDigit('0');
            refreshKeypad(player, session);
            return;
        }
        if (slot == 48) {
            session.backspace();
            refreshKeypad(player, session);
            return;
        }
        if (slot == 50) {
            long value = session.confirmKeypad();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            net.teujaem.shop.model.Shop shop = shopManager.get(holder.getShopId()).orElse(null);
            if (shop == null) return;
            holder.setChangingPage(true);
            if (holder.getType() == GuiType.PRICE_SETTING) {
                openPriceEdit(player, shop, 1, holder.getPriceType());
            } else {
                openStockEdit(player, shop, 1);
            }
        }
    }

    private void refreshKeypad(Player player, PlayerSession session) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        guiFactory.refreshKeypadDisplay(player.getOpenInventory().getTopInventory(), session);
    }

    // ---------------- 거래 요구 아이템 설정(확인 버튼) ----------------

    private void handleTradeSettingConfirm(Player player, ShopInventoryHolder holder) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getOpenInventory().getTopInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        PlayerSession session = plugin.getSessionManager().get(player.getUniqueId());
        session.setPendingTradeItems(items);

        net.teujaem.shop.model.Shop shop = shopManager.get(holder.getShopId()).orElse(null);
        if (shop == null) return;
        openTradeEdit(player, shop, 1);
    }

    // =========================================================
    //  닫기 이벤트 (편집 내용 저장 / 세션 정리)
    // =========================================================

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        ShopInventoryHolder holder = GuiFactory.holderOf(event.getInventory());
        if (holder == null) return;
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        PlayerSession session = plugin.getSessionManager().get(player.getUniqueId());

        switch (holder.getType()) {
            case ITEM_EDIT:
                saveItemEditPage(holder, event.getInventory());
                shopManager.save();
                break;
            case PRICE_EDIT:
                if (!holder.isChangingPage()) {
                    session.clearPendingNumber();
                }
                break;
            case STOCK_EDIT:
                if (!holder.isChangingPage()) {
                    session.clearPendingNumber();
                }
                break;
            case TRADE_EDIT:
                if (!holder.isChangingPage()) {
                    session.clearPendingTradeItems();
                }
                break;
            default:
                break;
        }
        holder.setChangingPage(false);
    }

    private void saveItemEditPage(ShopInventoryHolder holder, org.bukkit.inventory.Inventory inventory) {
        net.teujaem.shop.model.Shop shop = shopManager.get(holder.getShopId()).orElse(null);
        if (shop == null) return;
        int start = (holder.getPage() - 1) * GuiFactory.ITEMS_PER_PAGE;
        for (int i = 0; i < GuiFactory.ITEMS_PER_PAGE; i++) {
            int index = start + i + 1;
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                shop.setItem(index, null);
            } else {
                ShopItem shopItem = shop.getOrCreateItem(index);
                shopItem.setDisplayItem(item.clone());
            }
        }
    }

    // =========================================================
    //  GUI 열기 헬퍼 (커맨드에서도 재사용)
    // =========================================================

    public void openMain(Player player, net.teujaem.shop.model.Shop shop, int page) {
        player.openInventory(guiFactory.buildMain(shop, Math.max(1, page), player));
    }

    public void openItemEdit(Player player, net.teujaem.shop.model.Shop shop, int page) {
        player.openInventory(guiFactory.buildItemEdit(shop, Math.max(1, page)));
    }

    public void openPriceEdit(Player player, net.teujaem.shop.model.Shop shop, int page, PriceType type) {
        player.openInventory(guiFactory.buildPriceEdit(shop, Math.max(1, page), type, player));
    }

    public void openStockEdit(Player player, net.teujaem.shop.model.Shop shop, int page) {
        player.openInventory(guiFactory.buildStockEdit(shop, Math.max(1, page)));
    }

    public void openTradeEdit(Player player, net.teujaem.shop.model.Shop shop, int page) {
        player.openInventory(guiFactory.buildTradeEdit(shop, Math.max(1, page)));
    }

    public void openPriceSetting(Player player, net.teujaem.shop.model.Shop shop, PriceType type) {
        PlayerSession session = plugin.getSessionManager().get(player.getUniqueId());
        player.openInventory(guiFactory.buildPriceSettingKeypad(shop.getId(), type, session));
    }

    public void openStockSetting(Player player, net.teujaem.shop.model.Shop shop) {
        PlayerSession session = plugin.getSessionManager().get(player.getUniqueId());
        player.openInventory(guiFactory.buildStockSettingKeypad(shop.getId(), session));
    }

    public void openTradeSetting(Player player, net.teujaem.shop.model.Shop shop) {
        player.openInventory(guiFactory.buildTradeSetting(shop.getId()));
    }

    // =========================================================
    //  아이템 스택 유틸 (매장 구매/판매/거래용)
    // =========================================================

    private int countMatching(PlayerInventory inv, ItemStack sample) {
        int count = 0;
        for (ItemStack slot : inv.getStorageContents()) {
            if (slot != null && slot.isSimilar(sample)) count += slot.getAmount();
        }
        return count;
    }

    private void removeMatching(PlayerInventory inv, ItemStack sample, int amount) {
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack slotItem = contents[i];
            if (slotItem != null && slotItem.isSimilar(sample)) {
                int take = Math.min(slotItem.getAmount(), amount);
                slotItem.setAmount(slotItem.getAmount() - take);
                amount -= take;
                if (slotItem.getAmount() <= 0) contents[i] = null;
            }
        }
        inv.setStorageContents(contents);
    }

    private int freeCapacityFor(PlayerInventory inv, ItemStack sample) {
        int maxStack = sample.getMaxStackSize();
        int free = 0;
        for (ItemStack slotItem : inv.getStorageContents()) {
            if (slotItem == null || slotItem.getType() == Material.AIR) {
                free += maxStack;
            } else if (slotItem.isSimilar(sample)) {
                free += Math.max(0, maxStack - slotItem.getAmount());
            }
        }
        return free;
    }

    private void giveItems(Player player, ItemStack sample, int totalAmount) {
        int maxStack = sample.getMaxStackSize();
        while (totalAmount > 0) {
            int stackAmount = Math.min(maxStack, totalAmount);
            ItemStack toGive = sample.clone();
            toGive.setAmount(stackAmount);
            Optional.ofNullable(player.getInventory().addItem(toGive))
                    .ifPresent(leftover -> leftover.values().forEach(left ->
                            player.getWorld().dropItem(player.getLocation(), left)));
            totalAmount -= stackAmount;
        }
    }

    private void shop(Player player, String message) {
        MessageUtil.shop(player, message);
    }

    private void error(Player player, String message) {
        MessageUtil.error(player, message);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }
}
