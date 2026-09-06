package net.teujaem.shop.economy;

import net.teujaem.money.api.Money;
import org.bukkit.entity.Player;

/**
 * 이 플러그인에서 돈과 관련된 모든 처리는 반드시 이 클래스를 거칩니다.
 * 다른 곳(GUI, 커맨드, 리스너 등)에서는 {@link Money} API 를 직접 호출하지 않고
 * 이 서비스만 사용하도록 구조를 분리했습니다. 나중에 이코노미 플러그인이 바뀌더라도
 * 이 클래스만 수정하면 됩니다.
 */
public class EconomyService {

    /** 플레이어의 현재 잔액을 조회합니다. */
    public int getBalance(Player player) {
        return Money.get(player);
    }

    /** 잔액을 특정 금액으로 지정합니다. */
    public void setBalance(Player player, int amount) {
        Money.set(player, amount);
    }

    /** 잔액에 금액을 더합니다. */
    public void deposit(Player player, int amount) {
        if (amount <= 0) return;
        Money.add(player, amount);
    }

    /** 잔액에서 금액을 뺍니다. */
    public void withdraw(Player player, int amount) {
        if (amount <= 0) return;
        Money.remove(player, amount);
    }

    /** 플레이어가 해당 금액을 지불할 수 있는지 확인합니다. */
    public boolean hasBalance(Player player, int amount) {
        return getBalance(player) >= amount;
    }
}
