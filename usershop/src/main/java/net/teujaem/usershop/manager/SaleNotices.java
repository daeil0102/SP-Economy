package net.teujaem.usershop.manager;
import net.teujaem.usershop.UserShop;
import net.teujaem.usershop.entity.SaleNoticeEntity;
import org.bukkit.Bukkit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class SaleNotices {
    private final SaleNoticeDelivery delivery;
    public SaleNotices(UserShop plugin,ShopDatabase database) {
        delivery=new SaleNoticeDelivery(new SaleNoticeDelivery.Store() {
            public CompletableFuture<List<SaleNoticeEntity>> claim(UUID seller,String token,long now) {
                return database.claimNotices(seller,token,now);
            }
            public CompletableFuture<Void> finish(UUID id,String token,boolean delivered) {
                return database.finishNotice(id,token,delivered);
            }
        },new SaleNoticeDelivery.Recipient() {
            public boolean online(UUID seller) {
                var player=Bukkit.getPlayer(seller);return player!=null&&player.isOnline();
            }
            public void send(UUID seller,SaleNoticeEntity notice) {
                var player=Bukkit.getPlayer(seller);
                if(player==null||!player.isOnline())throw new IllegalStateException("판매자가 접속을 종료했습니다.");
                player.sendMessage(plugin.getShopConfig().prefixShop()+" §c"+notice.getBuyerName()+"§f님이 당신의 물건을 구매하여 §e"+String.format("%,d",notice.getAmount())+"§f원을 얻었습니다.");
            }
        },task->{
            if(plugin.isEnabled())Bukkit.getScheduler().runTask(plugin,task);
        },System::currentTimeMillis,error->plugin.getLogger().warning("판매 알림 처리 실패; DB에 남겨 재시도합니다: "+error.getMessage()));
    }
    public void deliver(UUID seller) { delivery.request(seller); }
}
