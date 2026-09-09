package net.teujaem.usershop;

import com.google.gson.Gson;
import net.teujaem.spFramework.api.ProxyData;
import net.teujaem.usershop.command.UserShopCommand;
import net.teujaem.usershop.config.ShopConfig;
import net.teujaem.usershop.gui.GuiBuilder;
import net.teujaem.usershop.gui.ShopHolder;
import net.teujaem.usershop.gui.StorageHolder;
import net.teujaem.usershop.listener.GuiListener;
import net.teujaem.usershop.listener.JoinListener;
import net.teujaem.usershop.listener.ProxyPurchaseListener;
import net.teujaem.usershop.listener.ShopSyncListener;
import net.teujaem.usershop.manager.ShopDatabase;
import net.teujaem.usershop.manager.ShopManager;
import net.teujaem.usershop.model.ShopEntry;
import net.teujaem.usershop.model.ShopUpdateInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public class UserShop extends JavaPlugin {

    /** ProxyEvent / ProxyData 에 사용할 이 플러그인의 식별 이름 */
    public static final String PROXY_PLUGIN_NAME = "UserShop";
    /** 누가 구매했는지 알리는 이벤트 이름 */
    public static final String PROXY_EVENT_PURCHASE = "purchase";
    /** 판매글 캐시 동기화(등록/제거/만료/초기화) 이벤트 이름 */
    public static final String PROXY_EVENT_SHOP_UPDATE = "shop_update";

    private ShopConfig shopConfig;
    private ShopDatabase shopDatabase;
    private ShopManager shopManager;
    private final Gson gson = new Gson();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.shopConfig = new ShopConfig(this);
        this.shopDatabase = new ShopDatabase();
        this.shopManager = new ShopManager(this, shopDatabase);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ProxyPurchaseListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopSyncListener(this), this);

        UserShopCommand command = new UserShopCommand(this);
        getCommand("유저상점").setExecutor(command);
        getCommand("유저상점").setTabCompleter(command);

        // 공유 DB에서 판매글 목록을 비동기로 불러옵니다. (완료 전에는 ShopManager.isReady()가 false)
        shopManager.bootstrap();

        // 1분(1200틱)마다 만료된 판매글을 회수하고, 열려있는 GUI를 갱신합니다.
        getServer().getScheduler().runTaskTimer(this, this::tickExpiration, 1200L, 1200L);

        getLogger().info("UserShop 플러그인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        // 판매글/보관함/정산대기금은 매 변경마다 즉시 DB에 저장되므로 여기서 따로 할 일이 없습니다.
        getLogger().info("UserShop 플러그인이 비활성화되었습니다.");
    }

    private void tickExpiration() {
        if (!shopManager.isReady()) return;
        List<ShopEntry> expired = shopManager.processExpired();
        if (!expired.isEmpty()) {
            for (ShopEntry entry : expired) {
                broadcastShopUpdate(ShopUpdateInfo.Type.EXPIRE, entry.getId(), entry.getSellerUuid());
            }
            refreshOpenInventories();
        }
    }

    /** 상점 변경 사항을 ProxyEvent로 다른 서버(및 이 서버)에 브로드캐스트합니다. */
    public void broadcastShopUpdate(ShopUpdateInfo.Type type, UUID listingId, UUID sellerUuid) {
        try {
            ShopUpdateInfo info = new ShopUpdateInfo(type, listingId, sellerUuid, shopConfig.getServerName());
            String json = gson.toJson(info);
            ProxyData.sendToServer(PROXY_PLUGIN_NAME, PROXY_EVENT_SHOP_UPDATE, json);
        } catch (Throwable t) {
            getLogger().warning("상점 동기화 브로드캐스트 실패(프록시 미설정일 수 있음): " + t.getMessage());
        }
    }

    /** 현재 상점/보관함 GUI를 보고 있는 모든 플레이어의 화면을 다시 그립니다. */
    public void refreshOpenInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            InventoryHolder holder = top.getHolder();
            if (holder instanceof ShopHolder shopHolder) {
                int page = Math.min(shopHolder.getPage(), shopManager.getMaxPage());
                player.openInventory(GuiBuilder.buildShop(player, shopManager, page));
            } else if (holder instanceof StorageHolder storageHolder) {
                // 보관함은 DB에서 다시 읽어와야 하므로 비동기로 갱신합니다.
                UUID owner = storageHolder.getOwnerUuid();
                int page = storageHolder.getPage();
                shopManager.getReturnedItems(owner).thenAccept(items ->
                        Bukkit.getScheduler().runTask(this, () -> {
                            int maxPage = GuiBuilder.maxPageFor(items.size());
                            int clampedPage = Math.min(page, maxPage);
                            player.openInventory(GuiBuilder.buildStorage(player, owner, items, clampedPage));
                        })
                );
            }
        }
    }

    public ShopConfig getShopConfig() {
        return shopConfig;
    }

    public ShopDatabase getShopDatabase() {
        return shopDatabase;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public Gson getGson() {
        return gson;
    }
}