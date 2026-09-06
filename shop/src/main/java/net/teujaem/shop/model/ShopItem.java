package net.teujaem.shop.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 상점 한 슬롯에 대한 데이터(전시 아이템 + 가격 + 재고 + 교환 요구 아이템).
 * 원본 Skript 의 {shop.item::*}, {shop.price.*::*}, {shop.stock.*::*}, {shop.trade::*} 변수를
 * 하나의 객체로 묶은 것입니다.
 */
public class ShopItem {

    private ItemStack displayItem;

    // 가격: [BUY|SELL] -> [DEFAULT|NOW|MIN|MAX] -> 값(없으면 해당 동작 불가)
    private final Map<PriceType, Map<PriceAction, Integer>> prices = new EnumMap<>(PriceType.class);

    // 재고
    private Integer stockDefault;   // null 이면 무제한
    private Integer stockNow;       // null 이면 재고 없음(구매 불가) - stockDefault 가 set 되어 있을 때만 의미가 있음

    // 거래(TRADE) 타입 상점에서 필요한 아이템 9칸
    private final List<ItemStack> tradeRequirements = new ArrayList<>();

    public ShopItem() {
        prices.put(PriceType.BUY, new EnumMap<>(PriceAction.class));
        prices.put(PriceType.SELL, new EnumMap<>(PriceAction.class));
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public void setDisplayItem(ItemStack displayItem) {
        this.displayItem = displayItem;
    }

    public Integer getPrice(PriceType type, PriceAction action) {
        return prices.get(type).get(action);
    }

    public void setPrice(PriceType type, PriceAction action, Integer value) {
        if (value == null) {
            prices.get(type).remove(action);
        } else {
            prices.get(type).put(action, value);
        }
    }

    /** now -> default 순으로 유효 가격을 찾습니다 (구매/판매 가능 여부 판단용). */
    public Integer getEffectivePrice(PriceType type, boolean useNow) {
        Integer now = prices.get(type).get(PriceAction.NOW);
        Integer def = prices.get(type).get(PriceAction.DEFAULT);
        if (useNow) {
            return now != null ? now : def;
        }
        return def;
    }

    public Integer getStockDefault() {
        return stockDefault;
    }

    public void setStockDefault(Integer stockDefault) {
        this.stockDefault = stockDefault;
    }

    public Integer getStockNow() {
        return stockNow;
    }

    public void setStockNow(Integer stockNow) {
        this.stockNow = stockNow;
    }

    public boolean hasLimitedStock() {
        return stockDefault != null;
    }

    public void resetStock() {
        if (stockDefault != null) {
            stockNow = stockDefault;
        }
    }

    public List<ItemStack> getTradeRequirements() {
        return tradeRequirements;
    }

    public void setTradeRequirements(List<ItemStack> items) {
        tradeRequirements.clear();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    tradeRequirements.add(item);
                }
            }
        }
    }

    public boolean hasTradeRequirements() {
        return !tradeRequirements.isEmpty();
    }

    // ---------- 저장/로드 ----------

    public void save(ConfigurationSection section) {
        section.set("item", displayItem);
        for (PriceType type : PriceType.values()) {
            for (Map.Entry<PriceAction, Integer> entry : prices.get(type).entrySet()) {
                section.set("price." + type.name() + "." + entry.getKey().name(), entry.getValue());
            }
        }
        if (stockDefault != null) section.set("stock.default", stockDefault);
        if (stockNow != null) section.set("stock.now", stockNow);
        if (!tradeRequirements.isEmpty()) {
            section.set("trade", tradeRequirements);
        }
    }

    public static ShopItem load(ConfigurationSection section) {
        ShopItem shopItem = new ShopItem();
        shopItem.displayItem = section.getItemStack("item");
        for (PriceType type : PriceType.values()) {
            ConfigurationSection priceSection = section.getConfigurationSection("price." + type.name());
            if (priceSection != null) {
                for (PriceAction action : PriceAction.values()) {
                    if (priceSection.contains(action.name())) {
                        shopItem.setPrice(type, action, priceSection.getInt(action.name()));
                    }
                }
            }
        }
        if (section.contains("stock.default")) {
            shopItem.stockDefault = section.getInt("stock.default");
        }
        if (section.contains("stock.now")) {
            shopItem.stockNow = section.getInt("stock.now");
        }
        List<?> list = section.getList("trade");
        if (list != null) {
            List<ItemStack> items = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof ItemStack) items.add((ItemStack) o);
            }
            shopItem.tradeRequirements.addAll(items);
        }
        return shopItem;
    }
}
