package net.teujaem.money.api;

import net.teujaem.money.manager.MoneyManager;
import org.bukkit.entity.Player;

public class Money {

    private static final net.teujaem.money.Money plugin = net.teujaem.money.Money.getInstance();

    public static void set(Player player, int amount) {
        plugin.getMoneyManager().update(player, amount);
    }

    public static void add(Player player, int amount) {
        MoneyManager moneyManager = plugin.getMoneyManager();
        moneyManager.update(player, moneyManager.get(player) + amount);
    }

    public static void remove(Player player, int amount) {
        MoneyManager moneyManager = plugin.getMoneyManager();
        moneyManager.update(player, moneyManager.get(player) - amount);
    }

    public static int get(Player player) {
        return plugin.getMoneyManager().get(player);
    }
}
