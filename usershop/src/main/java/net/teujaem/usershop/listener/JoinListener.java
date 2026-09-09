package net.teujaem.usershop.listener;

import net.teujaem.money.api.Money;
import net.teujaem.usershop.UserShop;
import net.teujaem.usershop.util.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;

/**
 * 플레이어 접속 시, 이 서버가 아니라 어느 서버에서 팔렸든 상관없이
 * DB(판매자 UUID 기준)에 쌓여있는 정산 대기금을 지급합니다.
 */
public class JoinListener implements Listener {

    private final UserShop plugin;

    public JoinListener(UserShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getShopDatabase().takePending(uuid).thenAccept(earnings -> {
            if (earnings.isEmpty()) return;
            Bukkit.getScheduler().runTask(plugin, () -> payOut(uuid, earnings));
        }).exceptionally(t -> {
            plugin.getLogger().warning("정산 대기금 조회 실패: " + t.getMessage());
            return null;
        });
    }

    private void payOut(UUID uuid, Map<String, Integer> earnings) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return; // 지급 직전에 다시 나간 경우

        String prefix = plugin.getShopConfig().prefixShop();
        for (Map.Entry<String, Integer> entry : earnings.entrySet()) {
            Money.add(player, entry.getValue());
            player.sendMessage(prefix + " §c" + entry.getKey() + "§f님이 당신의 물건을 구매하여 §e"
                    + MoneyFormat.format(entry.getValue()) + "§f원을 얻었습니다.");
        }
    }
}
