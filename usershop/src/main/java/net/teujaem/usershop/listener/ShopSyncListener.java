package net.teujaem.usershop.listener;

import net.teujaem.spFramework.api.event.ProxyEvent;
import net.teujaem.usershop.UserShop;
import net.teujaem.usershop.model.ShopUpdateInfo;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 다른 서버(혹은 이 서버 자신)에서 상점 데이터에 변경이 생겼을 때 오는 ProxyEvent를 받아
 * DB를 통째로 다시 읽지 않고, 바뀐 부분만 다시 불러와 로컬 캐시(ShopManager)를 갱신합니다.
 */
public class ShopSyncListener implements Listener {

    private final UserShop plugin;

    public ShopSyncListener(UserShop plugin) {
        this.plugin = plugin;
    }

    // 참고: SP-Framework는 ProxyEvent를 항상 메인 스레드에 스케줄링해서 발생시키므로
    // (WebSocketClient가 Bukkit.getScheduler().runTask로 콜백), 여기서부터는 메인 스레드입니다.
    @EventHandler
    public void onProxyEvent(ProxyEvent event) {
        if (!UserShop.PROXY_PLUGIN_NAME.equals(event.getPluginName())) return;
        if (!UserShop.PROXY_EVENT_SHOP_UPDATE.equals(event.getEventName())) return;

        Object rawValue = event.getValue();
        if (!(rawValue instanceof String json)) return;

        ShopUpdateInfo info;
        try {
            info = plugin.getGson().fromJson(json, ShopUpdateInfo.class);
        } catch (Exception ex) {
            plugin.getLogger().warning("상점 동기화 이벤트 파싱 실패: " + ex.getMessage());
            return;
        }
        if (info == null || info.type == null) return;

        switch (info.type) {
            case ADD -> {
                if (info.listingId == null) return;
                plugin.getShopManager().applyRemoteAdd(info.listingId)
                        .thenRun(() -> Bukkit.getScheduler().runTask(plugin, plugin::refreshOpenInventories));
            }
            case REMOVE, EXPIRE -> {
                if (info.listingId == null) return;
                plugin.getShopManager().applyRemoteRemove(info.listingId);
                plugin.refreshOpenInventories();
            }
            case RESET -> {
                plugin.getShopManager().clearCache();
                plugin.refreshOpenInventories();
            }
        }
    }
}
