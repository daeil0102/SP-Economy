package net.teujaem.shop.manager;

import net.teujaem.shop.Shop;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 서버에 존재하는 모든 {@link net.teujaem.shop.model.Shop} 을 관리(생성/조회/삭제/저장/로드)합니다.
 */
public class ShopManager {

    private final Shop plugin;
    private final Map<String, net.teujaem.shop.model.Shop> shops = new LinkedHashMap<>();
    private final File file;
    private boolean openEnabled = true;

    public ShopManager(Shop plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shops.yml");
    }

    public boolean exists(String id) {
        return shops.containsKey(id);
    }

    public Optional<net.teujaem.shop.model.Shop> get(String id) {
        return Optional.ofNullable(shops.get(id));
    }

    public net.teujaem.shop.model.Shop create(String id, net.teujaem.shop.model.ShopType type) {
        net.teujaem.shop.model.Shop shop = new net.teujaem.shop.model.Shop(id);
        shop.setType(type);
        shops.put(id, shop);
        return shop;
    }

    public void delete(String id) {
        shops.remove(id);
    }

    public Collection<net.teujaem.shop.model.Shop> all() {
        return shops.values();
    }

    public boolean isOpenEnabled() {
        return openEnabled;
    }

    public void setOpenEnabled(boolean openEnabled) {
        this.openEnabled = openEnabled;
    }

    // ---------- 저장 / 로드 ----------

    public void load() {
        shops.clear();
        if (!file.exists()) {
            openEnabled = true;
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        openEnabled = config.getBoolean("open-enabled", true);
        ConfigurationSection shopsSection = config.getConfigurationSection("shops");
        if (shopsSection != null) {
            for (String id : shopsSection.getKeys(false)) {
                ConfigurationSection section = shopsSection.getConfigurationSection(id);
                if (section != null) {
                    shops.put(id, net.teujaem.shop.model.Shop.load(id, section));
                }
            }
        }
        plugin.getLogger().info("상점 " + shops.size() + "개를 불러왔습니다.");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("open-enabled", openEnabled);
        ConfigurationSection shopsSection = config.createSection("shops");
        for (Map.Entry<String, net.teujaem.shop.model.Shop> entry : shops.entrySet()) {
            ConfigurationSection section = shopsSection.createSection(entry.getKey());
            entry.getValue().save(section);
        }
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("상점 데이터를 저장하지 못했습니다: " + e.getMessage());
        }
    }
}
