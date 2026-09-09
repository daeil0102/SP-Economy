package net.teujaem.usershop.util;

import java.util.Map;
import java.util.UUID;
import net.teujaem.money.api.Money;
import net.teujaem.usershop.UserShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 판매 대금을 DB(대기금 테이블)에서 안전하게 꺼내 온라인 상태인 판매자에게 지급하는 공용 로직.
 *
 * 어디서 호출되든(구매 직후, 프록시 이벤트 수신, 접속 시, 주기적 스윕) 동일하게 동작하며,
 * ShopDatabase.takePending()이 DB에서 원자적으로 꺼내면서 삭제하므로 중복 지급되지 않는다.
 * 즉, 판매자가 "구매가 발생한 서버"가 아니라 "현재 실제로 접속해 있는 서버" 어디에서든
 * 이 로직이 실행되면 정산이 이루어진다.
 */
public final class PendingPayout {

    private PendingPayout() {
    }

    public static void tryPay(UserShop plugin, UUID sellerUuid) {
        if (sellerUuid == null) {
            return;
        }
        Player player = Bukkit.getPlayer(sellerUuid);
        if (player == null || !player.isOnline()) {
            // 판매자가 이 서버에는 없음 -> 이 서버는 아무 것도 하지 않는다.
            // (DB에 남아있는 대기금은 다른 서버의 이벤트/스윕, 혹은 다음 접속 시 처리된다)
            return;
        }
        plugin.getShopDatabase().takePending(sellerUuid).thenAccept(earnings -> {
            if (earnings == null || earnings.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask((Plugin) plugin, () -> pay(plugin, sellerUuid, earnings));
        }).exceptionally(t -> {
            plugin.getLogger().warning("정산 대기금 조회 실패: " + t.getMessage());
            return null;
        });
    }

    private static void pay(UserShop plugin, UUID sellerUuid, Map<String, Integer> earnings) {
        Player player = Bukkit.getPlayer(sellerUuid);
        if (player == null || !player.isOnline()) {
            // 조회 응답이 오는 사이 접속을 종료한 경우: 그냥 지급을 포기하면 돈이 증발하므로
            // 다시 대기금으로 되돌려 다음 기회(다른 서버 접속 등)에 지급되게 한다.
            for (Map.Entry<String, Integer> entry : earnings.entrySet()) {
                plugin.getShopDatabase().addPending(sellerUuid, entry.getKey(), entry.getValue())
                        .exceptionally(t -> {
                            plugin.getLogger().warning("정산 대기금 복구 실패: " + t.getMessage());
                            return null;
                        });
            }
            return;
        }
        String prefix = plugin.getShopConfig().prefixShop();
        for (Map.Entry<String, Integer> entry : earnings.entrySet()) {
            Money.add(player, entry.getValue());
            player.sendMessage(prefix + " §c" + entry.getKey() + "§f님이 당신의 물건을 구매하여 §e"
                    + MoneyFormat.format(entry.getValue()) + "§f원을 얻었습니다.");
        }
    }
}