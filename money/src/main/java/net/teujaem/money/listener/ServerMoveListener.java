package net.teujaem.money.listener;

import net.teujaem.money.manager.MoneyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ServerMoveListener implements Listener {

    private final MoneyManager moneyManager;

    public ServerMoveListener(MoneyManager moneyManager) {
        this.moneyManager = moneyManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        moneyManager.load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        moneyManager.unload(event.getPlayer());
    }

}