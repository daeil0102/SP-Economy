package net.teujaem.usershop.util;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.teujaem.money.api.Money;
import net.teujaem.usershop.UserShop;
import org.bukkit.Bukkit;

/** Settlement is independent of the seller's server, login state and local money cache. */
public final class PendingPayout {
    private static final Set<UUID> IN_FLIGHT=ConcurrentHashMap.newKeySet();
    private PendingPayout(){}
    public static void tryPay(UserShop plugin,UUID seller){
        if(seller==null||!IN_FLIGHT.add(seller))return;
        plugin.getShopDatabase().settlePending(seller).whenComplete((earnings,error)->{
            IN_FLIGHT.remove(seller);
            if(error!=null){plugin.getLogger().warning("정산 실패; 대기금은 DB에 유지됩니다: "+error.getMessage());return;}
            // A different server may have settled first, or a join may overlap it.
            plugin.getSaleNotices().deliver(seller);
            if(earnings==null||earnings.isEmpty())return;
            Money.notifyBalanceChanged(seller);
            plugin.broadcastSaleNotice(seller);
        });
    }
}
