package net.teujaem.money;

import net.teujaem.money.listener.ServerMoveListener;
import net.teujaem.money.manager.MoneyManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Money extends JavaPlugin {

    private static Money instance;

    private static MoneyManager moneyManager;

    @Override
    public void onEnable() {
        instance = this;
        reload();
    }

    @Override
    public void onDisable() {
        if (moneyManager != null) moneyManager.shutdown();
    }

    private void reload() {
        moneyManager = new MoneyManager();
        getServer().getPluginManager().registerEvents(new ServerMoveListener(moneyManager), this);
        getServer().getPluginManager().registerEvents(new net.teujaem.money.listener.BalanceSyncListener(moneyManager), this);
        getServer().getOnlinePlayers().forEach(moneyManager::load);
        getServer().getScheduler().runTaskTimer(this, () -> getServer().getOnlinePlayers()
                .forEach(player -> moneyManager.refresh(player.getUniqueId())), 100L, 100L);
    }

    public static Money getInstance() {
        return instance;
    }

    public MoneyManager getMoneyManager() {
        return moneyManager;
    }

}
