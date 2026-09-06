package net.teujaem.shop.gui;

import net.teujaem.shop.Shop;
import net.teujaem.shop.model.PriceAction;
import net.teujaem.shop.model.PriceType;
import net.teujaem.shop.model.ShopItem;
import net.teujaem.shop.model.ShopType;
import net.teujaem.shop.util.ItemUtil;
import net.teujaem.shop.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GuiFactory {

    public static final int ITEMS_PER_PAGE = 45;
    public static final int SLOT_PREV_PAGE = 48;
    public static final int SLOT_PAGE_INDICATOR = 49;
    public static final int SLOT_NEXT_PAGE = 50;
    public static final int SLOT_INFO_SKULL = 53;

    private final Shop plugin;

    public GuiFactory(Shop plugin) {
        this.plugin = plugin;
    }

    private Inventory create(ShopInventoryHolder holder, int size, String title) {
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);
        return inventory;
    }

    private void fillNavRow(Inventory inv, int page, boolean includeInfoSkull, Player viewer) {
        ItemStack fill = ItemUtil.named(Material.WHITE_STAINED_GLASS_PANE, "&f");
        for (int slot = 45; slot <= 53; slot++) {
            inv.setItem(slot, fill);
        }
        inv.setItem(SLOT_PREV_PAGE, ItemUtil.named(Material.RED_STAINED_GLASS_PANE,
                "&c전 페이지 (" + (page - 1) + ")"));
        inv.setItem(SLOT_PAGE_INDICATOR, ItemUtil.named(Material.WHITE_STAINED_GLASS_PANE, "&b" + page));
        inv.setItem(SLOT_NEXT_PAGE, ItemUtil.named(Material.GREEN_STAINED_GLASS_PANE,
                "&2다음 페이지 (" + (page + 1) + ")"));
        if (includeInfoSkull && viewer != null) {
            List<String> lore = new ArrayList<>();
            lore.add("&7닉네임 &f: &2" + viewer.getName());
            lore.add("&a보유금액 &f: &6" + plugin.getEconomyService().getBalance(viewer));
            inv.setItem(SLOT_INFO_SKULL, ItemUtil.playerHead(viewer, "&5&l정보", lore));
        }
    }

    private static int slotForIndex(int indexInPage) {
        return indexInPage; // 0..44
    }

    // ---------------- 메인 상점 (유저용) ----------------

    public Inventory buildMain(net.teujaem.shop.model.Shop shop, int page, Player viewer) {
        ShopInventoryHolder holder = new ShopInventoryHolder(GuiType.MAIN, shop.getId(), page);
        String title = MessageUtil.guiTitle("main", shop.getId());
        Inventory inv = create(holder, 54, title);
        fillNavRow(inv, page, true, viewer);

        int start = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int index = start + i + 1;
            ShopItem shopItem = shop.getItem(index);
            if (shopItem == null || shopItem.getDisplayItem() == null) continue;
            ItemStack display = shopItem.getDisplayItem().clone();
            List<String> lore = ItemUtil.getLore(shopItem.getDisplayItem());
            lore = new ArrayList<>(lore);
            lore.add("");
            if (shop.getType() == ShopType.NORMAL || shop.getType() == ShopType.CHANGE) {
                boolean useNow = shop.getType() == ShopType.CHANGE;
                lore.add(priceLine(shopItem, PriceType.SELL, useNow));
                lore.add(priceLine(shopItem, PriceType.BUY, useNow));
                if (shop.getType() == ShopType.CHANGE) {
                    lore.add("");
                    Integer sellMin = shopItem.getPrice(PriceType.SELL, PriceAction.MIN);
                    Integer sellMax = shopItem.getPrice(PriceType.SELL, PriceAction.MAX);
                    Integer buyMin = shopItem.getPrice(PriceType.BUY, PriceAction.MIN);
                    Integer buyMax = shopItem.getPrice(PriceType.BUY, PriceAction.MAX);
                    if (sellMin != null && sellMin.equals(sellMax)) {
                        lore.add("&b판매 가격고정");
                    } else {
                        if (buyMin != null) lore.add("&b구매 최소가 : " + buyMin);
                        if (buyMax != null) lore.add("&b구매 최대가 : " + buyMax);
                        if (sellMin != null) lore.add("&b판매 최소가 : " + sellMin);
                        if (sellMax != null) lore.add("&b판매 최대가 : " + sellMax);
                    }
                }
                lore.add("");
                lore.add("&f좌클릭 : 구매");
                lore.add("&f우클릭 : 판매");
                lore.add("&f쉬프트 + 좌클릭 : 64개 구매");
                lore.add("&f쉬프트 + 우클릭 : 전부 판매");
            } else { // TRADE
                if (shopItem.hasTradeRequirements()) {
                    for (ItemStack req : shopItem.getTradeRequirements()) {
                        lore.add("&f" + ItemUtil.displayName(req) + " " + req.getAmount() + "개 필요");
                    }
                } else {
                    lore.add("&c거래 불가");
                }
            }
            if (shopItem.hasLimitedStock()) {
                Integer now = shopItem.getStockNow();
                lore.add("&7재고 : " + (now == null ? 0 : now));
            }
            ItemUtil.setLore(display, lore);
            inv.setItem(slotForIndex(i), display);
        }
        return inv;
    }

    private String priceLine(ShopItem item, PriceType type, boolean useNow) {
        Integer value = useNow ? item.getEffectivePrice(type, true) : item.getPrice(type, PriceAction.DEFAULT);
        String typeKr = type.displayName();
        if (value == null) {
            return "&c" + typeKr + " 불가";
        }
        return "&f" + typeKr + "가 : " + value;
    }

    // ---------------- 관리자: 아이템 편집 ----------------

    public Inventory buildItemEdit(net.teujaem.shop.model.Shop shop, int page) {
        ShopInventoryHolder holder = new ShopInventoryHolder(GuiType.ITEM_EDIT, shop.getId(), page);
        Inventory inv = create(holder, 54, MessageUtil.guiTitle("item-edit", shop.getId()));
        fillNavRow(inv, page, false, null);
        int start = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int index = start + i + 1;
            ShopItem shopItem = shop.getItem(index);
            if (shopItem != null && shopItem.getDisplayItem() != null) {
                inv.setItem(slotForIndex(i), shopItem.getDisplayItem().clone());
            }
        }
        return inv;
    }

    // ---------------- 관리자: 가격 편집 목록 ----------------

    public Inventory buildPriceEdit(net.teujaem.shop.model.Shop shop, int page, PriceType priceType, Player viewer) {
        ShopInventoryHolder holder = new ShopInventoryHolder(GuiType.PRICE_EDIT, shop.getId(), page)
                .priceType(priceType);
        String key = priceType == PriceType.BUY ? "price-edit-buy" : "price-edit-sell";
        Inventory inv = create(holder, 54, MessageUtil.guiTitle(key, shop.getId()));
        fillNavRow(inv, page, false, null);

        int start = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int index = start + i + 1;
            ShopItem shopItem = shop.getItem(index);
            if (shopItem == null || shopItem.getDisplayItem() == null) continue;
            ItemStack display = shopItem.getDisplayItem().clone();
            List<String> lore = new ArrayList<>(ItemUtil.getLore(shopItem.getDisplayItem()));
            lore.add("");
            lore.add("&a좌클릭 : 기본값 설정 (" + describe(shopItem.getPrice(priceType, PriceAction.DEFAULT)) + ")");
            lore.add("&a우클릭 : 현재값 설정 (" + describe(shopItem.getPrice(priceType, PriceAction.NOW)) + ")");
            lore.add("&a쉬프트 + 좌클릭 : 최소값 설정 (" + describe(shopItem.getPrice(priceType, PriceAction.MIN)) + ")");
            lore.add("&a쉬프트 + 우클릭 : 최대값 설정 (" + describe(shopItem.getPrice(priceType, PriceAction.MAX)) + ")");
            ItemUtil.setLore(display, lore);
            inv.setItem(slotForIndex(i), display);
        }
        return inv;
    }

    // ---------------- 관리자: 거래 편집 목록 ----------------

    public Inventory buildTradeEdit(net.teujaem.shop.model.Shop shop, int page) {
        ShopInventoryHolder holder = new ShopInventoryHolder(GuiType.TRADE_EDIT, shop.getId(), page);
        Inventory inv = create(holder, 54, MessageUtil.guiTitle("trade-edit", shop.getId()));
        fillNavRow(inv, page, false, null);

        int start = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int index = start + i + 1;
            ShopItem shopItem = shop.getItem(index);
            if (shopItem == null || shopItem.getDisplayItem() == null) continue;
            ItemStack display = shopItem.getDisplayItem().clone();
            List<String> lore = new ArrayList<>(ItemUtil.getLore(shopItem.getDisplayItem()));
            lore.add("");
            if (shopItem.hasTradeRequirements()) {
                for (ItemStack req : shopItem.getTradeRequirements()) {
                    lore.add("&f" + ItemUtil.displayName(req) + " " + req.getAmount() + "개 필요");
                }
            } else {
                lore.add("&c거래 불가");
            }
            ItemUtil.setLore(display, lore);
            inv.setItem(slotForIndex(i), display);
        }
        return inv;
    }

    // ---------------- 관리자: 재고 편집 목록 ----------------

    public Inventory buildStockEdit(net.teujaem.shop.model.Shop shop, int page) {
        ShopInventoryHolder holder = new ShopInventoryHolder(GuiType.STOCK_EDIT, shop.getId(), page);
        Inventory inv = create(holder, 54, MessageUtil.guiTitle("stock-edit", shop.getId()));
        fillNavRow(inv, page, false, null);

        int start = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int index = start + i + 1;
            ShopItem shopItem = shop.getItem(index);
            if (shopItem == null || shopItem.getDisplayItem() == null) continue;
            ItemStack display = shopItem.getDisplayItem().clone();
            List<String> lore = new ArrayList<>(ItemUtil.getLore(shopItem.getDisplayItem()));
            lore.add("");
            lore.add("&a좌클릭 : 기본 재고 설정 (" + describe(shopItem.getStockDefault()) + ")");
            lore.add("&a우클릭 : 현재 재고 설정 (" + describe(shopItem.getStockNow()) + ")");
            ItemUtil.setLore(display, lore);
            inv.setItem(slotForIndex(i), display);
        }
        return inv;
    }

    private String describe(Integer value) {
        return value == null ? "없음" : String.valueOf(value);
    }

    // ---------------- 숫자 키패드 (가격/재고 입력 공용) ----------------

    private Inventory buildKeypad(ShopInventoryHolder holder, String title, PlayerSession session) {
        Inventory inv = create(holder, 54, title);
        String[][] rows = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"}
        };
        int[] rowStarts = {21, 30, 39};
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                inv.setItem(rowStarts[r] + c, ItemUtil.named(Material.BLACK_STAINED_GLASS_PANE, "&a" + rows[r][c]));
            }
        }
        inv.setItem(49, ItemUtil.named(Material.BLACK_STAINED_GLASS_PANE, "&a0"));
        inv.setItem(48, ItemUtil.named(Material.RED_STAINED_GLASS_PANE, "&c지우기"));
        inv.setItem(50, ItemUtil.named(Material.GREEN_STAINED_GLASS_PANE, "&2확인"));
        inv.setItem(34, ItemUtil.named(Material.ORANGE_STAINED_GLASS_PANE,
                "&b현재 숫자 : " + session.getKeypadDisplay()));
        return inv;
    }

    public Inventory buildPriceSettingKeypad(String shopId, PriceType priceType, PlayerSession session) {
        session.resetKeypad();
        ShopInventoryHolder holder = new ShopInventoryHolder(GuiType.PRICE_SETTING, shopId, 1).priceType(priceType);
        return buildKeypad(holder, MessageUtil.guiTitle("price-setting", shopId), session);
    }

    public Inventory buildStockSettingKeypad(String shopId, PlayerSession session) {
        session.resetKeypad();
        ShopInventoryHolder holder = new ShopInventoryHolder(GuiType.STOCK_SETTING, shopId, 1);
        return buildKeypad(holder, MessageUtil.guiTitle("stock-setting", shopId), session);
    }

    /** 키패드 인벤토리의 34번 슬롯 표시를 현재 세션 값으로 갱신합니다. */
    public void refreshKeypadDisplay(Inventory inv, PlayerSession session) {
        inv.setItem(34, ItemUtil.named(Material.ORANGE_STAINED_GLASS_PANE,
                "&b현재 숫자 : " + session.getKeypadDisplay()));
    }

    // ---------------- 관리자: 거래(TRADE) 요구 아이템 설정 ----------------

    public Inventory buildTradeSetting(String shopId) {
        ShopInventoryHolder holder = new ShopInventoryHolder(GuiType.TRADE_SETTING, shopId, 1);
        Inventory inv = create(holder, 18, MessageUtil.guiTitle("trade-setting", shopId));
        for (int i = 9; i < 18; i++) {
            if (i == 13) {
                inv.setItem(13, ItemUtil.named(Material.GREEN_STAINED_GLASS_PANE, "&a확인"));
            } else {
                inv.setItem(i, ItemUtil.named(Material.WHITE_STAINED_GLASS_PANE, "&f"));
            }
        }
        return inv;
    }

    public static ShopInventoryHolder holderOf(Inventory inventory) {
        if (inventory != null && inventory.getHolder() instanceof ShopInventoryHolder) {
            return (ShopInventoryHolder) inventory.getHolder();
        }
        return null;
    }
}
