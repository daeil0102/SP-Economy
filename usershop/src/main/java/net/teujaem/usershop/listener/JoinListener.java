package net.teujaem.usershop.listener;
import net.teujaem.usershop.UserShop;
import net.teujaem.usershop.util.PendingPayout;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
public final class JoinListener implements Listener {
    private final UserShop plugin;
    public JoinListener(UserShop plugin){this.plugin=plugin;}
    @EventHandler public void join(PlayerJoinEvent event){
        plugin.getSaleNotices().deliver(event.getPlayer().getUniqueId());
        PendingPayout.tryPay(plugin,event.getPlayer().getUniqueId());
    }
}
