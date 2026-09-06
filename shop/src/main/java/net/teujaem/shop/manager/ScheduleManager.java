package net.teujaem.shop.manager;

import net.teujaem.shop.Shop;
import net.teujaem.shop.model.PriceAction;
import net.teujaem.shop.model.PriceType;
import net.teujaem.shop.model.ShopItem;
import net.teujaem.shop.model.ShopType;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * 원본 Skript 의 `every 1 minute` 트리거를 대체합니다.
 * - 재고 초기화 타이머가 다 되면 해당 상점의 재고를 기본값으로 되돌립니다.
 * - 시세(CHANGE) 타이머가 다 되면 가격을 무작위로 변동시킵니다.
 */
public class ScheduleManager {

    private final Shop plugin;
    private final Random random = new Random();
    private BukkitTask task;

    public ScheduleManager(Shop plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long period = plugin.getConfig().getLong("scheduler-period-ticks", 1200L);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        ShopManager shopManager = plugin.getShopManager();
        boolean changed = false;
        for (net.teujaem.shop.model.Shop shop : shopManager.all()) {
            if (shop.getStockTimeMinutes() > 0) {
                shop.setStockTimeRemaining(shop.getStockTimeRemaining() - 1);
                if (shop.getStockTimeRemaining() <= 0) {
                    shop.setStockTimeRemaining(shop.getStockTimeMinutes());
                    resetStock(shop);
                    changed = true;
                }
            }
            if (shop.getType() == ShopType.CHANGE && shop.getChangeTimeMinutes() > 0) {
                shop.setChangeTimeRemaining(shop.getChangeTimeRemaining() - 1);
                if (shop.getChangeTimeRemaining() <= 0) {
                    shop.setChangeTimeRemaining(shop.getChangeTimeMinutes());
                    changePrice(shop);
                    changed = true;
                }
            }
        }
        if (changed) {
            shopManager.save();
        }
    }

    public void resetStock(net.teujaem.shop.model.Shop shop) {
        for (ShopItem item : shop.getItems().values()) {
            item.resetStock();
        }
    }

    /**
     * 시세(CHANGE) 상점의 가격을 무작위로 오르내리게 합니다.
     * 변경률최소/최대(%) 범위 안에서 랜덤하게 결정하고, 슬롯별 최소/최대 가격을 벗어나지 않게 합니다.
     */
    public void changePrice(net.teujaem.shop.model.Shop shop) {
        Integer rateMin = shop.getChangeRateMin();
        Integer rateMax = shop.getChangeRateMax();
        if (rateMin == null || rateMax == null) return;

        for (ShopItem item : shop.getItems().values()) {
            boolean goingUp = random.nextBoolean();
            Map<PriceType, Integer> current = new EnumMap<>(PriceType.class);
            for (PriceType type : PriceType.values()) {
                Integer now = item.getEffectivePrice(type, true);
                if (now != null) current.put(type, now);
            }
            if (current.isEmpty()) continue;

            if (goingUp) {
                int ratePercent = rateMax <= 0 ? 0 : random.nextInt(rateMax + 1);
                for (Map.Entry<PriceType, Integer> entry : current.entrySet()) {
                    PriceType type = entry.getKey();
                    long price = entry.getValue();
                    price += Math.floorDiv(price * ratePercent, 100);
                    Integer max = item.getPrice(type, PriceAction.MAX);
                    if (max != null && price > max) price = max;
                    item.setPrice(type, PriceAction.NOW, (int) price);
                }
            } else {
                int ratePercent = rateMin <= 0 ? 0 : random.nextInt(rateMin + 1);
                for (Map.Entry<PriceType, Integer> entry : current.entrySet()) {
                    PriceType type = entry.getKey();
                    long price = entry.getValue();
                    price -= Math.floorDiv(price * ratePercent, 100);
                    Integer min = item.getPrice(type, PriceAction.MIN);
                    if (min != null && price < min) price = min;
                    item.setPrice(type, PriceAction.NOW, (int) price);
                }
            }
        }
    }
}
