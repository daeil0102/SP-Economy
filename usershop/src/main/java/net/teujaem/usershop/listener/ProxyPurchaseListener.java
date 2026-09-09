package net.teujaem.usershop.listener;

import net.teujaem.spFramework.api.event.ProxyEvent;
import net.teujaem.usershop.UserShop;
import net.teujaem.usershop.model.PurchaseInfo;
import net.teujaem.usershop.util.MoneyFormat;
import net.teujaem.usershop.util.PendingPayout;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ProxyPurchaseListener implements Listener {
    private static final long SWEEP_PERIOD_TICKS = 200L; // 10초

    private final UserShop plugin;

    public ProxyPurchaseListener(UserShop plugin) {
        this.plugin = plugin;
        // 프록시 이벤트가 유실되거나(서버 재시작 타이밍 등) 판매자가 이벤트 수신 이후에
        // 접속한 경우를 대비해, 접속 중인 플레이어의 대기금을 주기적으로 확인해 지급한다.
        // 실제 지급 여부를 가르는 건 항상 DB(대기금 테이블)이므로 재시작에도 안전하다.
        Bukkit.getScheduler().runTaskTimer((Plugin) plugin, this::sweepOnlinePlayers, SWEEP_PERIOD_TICKS, SWEEP_PERIOD_TICKS);
    }

    private void sweepOnlinePlayers() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PendingPayout.tryPay(this.plugin, online.getUniqueId());
        }
    }

    @EventHandler
    public void onProxyEvent(ProxyEvent event) {
        if (!"UserShop".equals(event.getPluginName())) {
            return;
        }
        if (!"purchase".equals(event.getEventName())) {
            return;
        }
        Object rawValue = event.getValue();
        if (!(rawValue instanceof String)) {
            return;
        }
        String json = (String) rawValue;
        PurchaseInfo info;
        try {
            info = this.plugin.getGson().fromJson(json, PurchaseInfo.class);
        } catch (Exception ex) {
            this.plugin.getLogger().warning("구매 알림 파싱 실패: " + ex.getMessage());
            return;
        }
        if (info == null || info.buyerName == null) {
            return;
        }

        this.plugin.getLogger().info("[유저상점 구매기록] " + info.buyerName + " -> " + info.sellerName + " / "
                + info.itemDisplayName + " / " + MoneyFormat.format(info.price) + "원 (판매서버: " + info.serverName + ")");

        // 구매가 발생한 서버가 어디든, 판매자가 지금 이 서버(이벤트를 받은 서버)에
        // 접속해 있다면 즉시 지급을 시도한다. -> 판매자가 있는 서버가 바로 지급 서버가 된다.
        PendingPayout.tryPay(this.plugin, info.sellerUuid);
    }
}