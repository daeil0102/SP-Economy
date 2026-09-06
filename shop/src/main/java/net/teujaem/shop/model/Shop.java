package net.teujaem.shop.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.TreeMap;

/**
 * 상점 하나(예: "무기상점") 를 나타냅니다. 슬롯 번호(1부터 시작, 원본 Skript 와 동일)를
 * 키로 하는 {@link ShopItem} 목록을 가지고 있습니다.
 */
public class Shop {

    private final String id;
    private ShopType type = ShopType.NORMAL;
    private boolean locked = false;

    private final Map<Integer, ShopItem> items = new TreeMap<>();

    // 재고 관련 자동 초기화 타이머 (분 단위)
    private int stockTimeMinutes = 0;   // 0 = 비활성화
    private int stockTimeRemaining = 0;
    private boolean stockAddOnUserSell = false;

    // 시세 변동(CHANGE 타입) 관련
    private int changeTimeMinutes = 0;  // 0 = 비활성화
    private int changeTimeRemaining = 0;
    private Integer changeRateMin;      // %
    private Integer changeRateMax;      // %

    public Shop(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public ShopType getType() {
        return type;
    }

    public void setType(ShopType type) {
        this.type = type;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Map<Integer, ShopItem> getItems() {
        return items;
    }

    public ShopItem getItem(int index) {
        return items.get(index);
    }

    public ShopItem getOrCreateItem(int index) {
        return items.computeIfAbsent(index, k -> new ShopItem());
    }

    public void setItem(int index, ShopItem item) {
        if (item == null) {
            items.remove(index);
        } else {
            items.put(index, item);
        }
    }

    public int getStockTimeMinutes() {
        return stockTimeMinutes;
    }

    public void setStockTimeMinutes(int stockTimeMinutes) {
        this.stockTimeMinutes = stockTimeMinutes;
        this.stockTimeRemaining = stockTimeMinutes;
    }

    public int getStockTimeRemaining() {
        return stockTimeRemaining;
    }

    public void setStockTimeRemaining(int stockTimeRemaining) {
        this.stockTimeRemaining = stockTimeRemaining;
    }

    public boolean isStockAddOnUserSell() {
        return stockAddOnUserSell;
    }

    public void setStockAddOnUserSell(boolean stockAddOnUserSell) {
        this.stockAddOnUserSell = stockAddOnUserSell;
    }

    public int getChangeTimeMinutes() {
        return changeTimeMinutes;
    }

    public void setChangeTimeMinutes(int changeTimeMinutes) {
        this.changeTimeMinutes = changeTimeMinutes;
        this.changeTimeRemaining = changeTimeMinutes;
    }

    public int getChangeTimeRemaining() {
        return changeTimeRemaining;
    }

    public void setChangeTimeRemaining(int changeTimeRemaining) {
        this.changeTimeRemaining = changeTimeRemaining;
    }

    public Integer getChangeRateMin() {
        return changeRateMin;
    }

    public void setChangeRateMin(Integer changeRateMin) {
        this.changeRateMin = changeRateMin;
    }

    public Integer getChangeRateMax() {
        return changeRateMax;
    }

    public void setChangeRateMax(Integer changeRateMax) {
        this.changeRateMax = changeRateMax;
    }

    // ---------- 저장/로드 ----------

    public void save(ConfigurationSection section) {
        section.set("type", type.name());
        section.set("locked", locked);
        section.set("stock-time-minutes", stockTimeMinutes);
        section.set("stock-time-remaining", stockTimeRemaining);
        section.set("stock-add-on-user-sell", stockAddOnUserSell);
        section.set("change-time-minutes", changeTimeMinutes);
        section.set("change-time-remaining", changeTimeRemaining);
        if (changeRateMin != null) section.set("change-rate-min", changeRateMin);
        if (changeRateMax != null) section.set("change-rate-max", changeRateMax);

        ConfigurationSection itemsSection = section.createSection("items");
        for (Map.Entry<Integer, ShopItem> entry : items.entrySet()) {
            ConfigurationSection itemSection = itemsSection.createSection(String.valueOf(entry.getKey()));
            entry.getValue().save(itemSection);
        }
    }

    public static Shop load(String id, ConfigurationSection section) {
        Shop shop = new Shop(id);
        try {
            shop.type = ShopType.valueOf(section.getString("type", "NORMAL"));
        } catch (IllegalArgumentException ex) {
            shop.type = ShopType.NORMAL;
        }
        shop.locked = section.getBoolean("locked", false);
        shop.stockTimeMinutes = section.getInt("stock-time-minutes", 0);
        shop.stockTimeRemaining = section.getInt("stock-time-remaining", shop.stockTimeMinutes);
        shop.stockAddOnUserSell = section.getBoolean("stock-add-on-user-sell", false);
        shop.changeTimeMinutes = section.getInt("change-time-minutes", 0);
        shop.changeTimeRemaining = section.getInt("change-time-remaining", shop.changeTimeMinutes);
        if (section.contains("change-rate-min")) shop.changeRateMin = section.getInt("change-rate-min");
        if (section.contains("change-rate-max")) shop.changeRateMax = section.getInt("change-rate-max");

        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int index = Integer.parseInt(key);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                    if (itemSection != null) {
                        shop.items.put(index, ShopItem.load(itemSection));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return shop;
    }
}
