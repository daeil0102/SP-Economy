package net.teujaem.usershop.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 판매 시점에 판매자가 이 서버에 접속해 있지 않았을 때, 정산할 금액을 쌓아두는 큐.
 * 판매자가 이 서버에 재접속하면 지급합니다. (원본 Skript의 {판매금액::*} 변수 대체)
 *
 * 구매자별로 얼마씩 벌었는지 함께 기록해, 접속 시 원본처럼
 * "누구님이 당신의 물건을 구매하여 얼마를 얻었습니다" 메시지를 보여줄 수 있습니다.
 */
public class PendingEarningsManager {

    private final JavaPlugin plugin;
    private final File file;

    // sellerUuid -> (buyerName -> 누적 금액)
    private final Map<UUID, Map<String, Integer>> pending = new ConcurrentHashMap<>();

    public PendingEarningsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pending.yml");
    }

    public void add(UUID sellerUuid, String buyerName, int amount) {
        pending.computeIfAbsent(sellerUuid, k -> new LinkedHashMap<>())
                .merge(buyerName, amount, Integer::sum);
    }

    /** 판매자의 대기 정산 내역을 꺼내면서 큐에서 제거합니다. */
    public Map<String, Integer> takeAll(UUID sellerUuid) {
        Map<String, Integer> result = pending.remove(sellerUuid);
        return result == null ? Map.of() : result;
    }

    public void load() {
        pending.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String sellerKey : yml.getKeys(false)) {
            try {
                UUID sellerUuid = UUID.fromString(sellerKey);
                Map<String, Integer> map = new LinkedHashMap<>();
                var section = yml.getConfigurationSection(sellerKey);
                if (section != null) {
                    for (String buyerName : section.getKeys(false)) {
                        map.put(buyerName, section.getInt(buyerName));
                    }
                }
                pending.put(sellerUuid, map);
            } catch (Exception e) {
                plugin.getLogger().warning("pending.yml 항목 로드 실패: " + e.getMessage());
            }
        }
    }

    public void save() {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Integer>> e : pending.entrySet()) {
            for (Map.Entry<String, Integer> inner : e.getValue().entrySet()) {
                yml.set(e.getKey() + "." + inner.getKey(), inner.getValue());
            }
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("pending.yml 저장 실패: " + ex.getMessage());
        }
    }
}
