package net.teujaem.money.manager;

import net.teujaem.money.entity.MoneyEntity;
import net.teujaem.spFramework.api.DataBase;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MoneyManager {

    private final DataBase database;
    private final Map<UUID, MoneyEntity> map = new ConcurrentHashMap<>();

    public MoneyManager() {
        this.database = new DataBase(MoneyEntity.class);
    }

    public void load(Player player) {
        UUID uuid = player.getUniqueId();

        database.find(MoneyEntity.class, uuid)
                .thenAccept(entity -> {
                    if (entity == null) {
                        entity = new MoneyEntity(uuid, 0);
                        database.save(entity);
                    }

                    map.put(uuid, entity);
                });
    }

    public int get(Player player) {
        MoneyEntity entity = map.get(player.getUniqueId());

        if (entity == null) {
            return 0;
        }

        return entity.getMoney();
    }

    public void update(Player player, int amount) {
        MoneyEntity entity = map.get(player.getUniqueId());

        if (entity == null) {
            return;
        }

        entity.setMoney(amount);
        database.save(entity);
    }

    public void add(Player player, int amount) {
        update(player, get(player) + amount);
    }

    public boolean remove(Player player, int amount) {
        int money = get(player);

        if (money < amount) {
            return false;
        }

        update(player, money - amount);
        return true;
    }

    public boolean has(Player player, int amount) {
        return get(player) >= amount;
    }

    public void unload(Player player) {
        UUID uuid = player.getUniqueId();
        MoneyEntity entity = map.get(uuid);

        if (entity == null) {
            return;
        }

        database.save(entity)
                .thenRun(() -> map.remove(uuid));
    }
}