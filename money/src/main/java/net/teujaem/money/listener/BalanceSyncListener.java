package net.teujaem.money.listener;
import net.teujaem.money.manager.MoneyManager;
import net.teujaem.spFramework.api.event.ProxyEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.UUID;
public final class BalanceSyncListener implements Listener {
    private final MoneyManager manager;
    public BalanceSyncListener(MoneyManager manager){this.manager=manager;}
    @EventHandler public void changed(ProxyEvent event){
        if(!"SP-Money".equals(event.getPluginName())||!"balance_changed".equals(event.getEventName())||!(event.getValue() instanceof String value)||value.length()!=36)return;
        try{UUID id=UUID.fromString(value);manager.refresh(id);}catch(IllegalArgumentException ignored){}
    }
}
