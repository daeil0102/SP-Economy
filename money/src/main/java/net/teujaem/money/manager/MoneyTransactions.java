package net.teujaem.money.manager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import net.teujaem.money.entity.MoneyEntity;
import java.util.UUID;
public final class MoneyTransactions {
    private MoneyTransactions() {}
    public static MoneyEntity locked(EntityManager em,UUID uuid){
        MoneyEntity row=em.find(MoneyEntity.class,uuid,LockModeType.PESSIMISTIC_WRITE);
        if(row==null){row=new MoneyEntity(uuid,0);em.persist(row);em.flush();}return row;
    }
    public static int set(EntityManager em,UUID uuid,int amount){
        if(amount<0)throw new IllegalArgumentException("잔액은 음수일 수 없습니다.");
        MoneyEntity row=locked(em,uuid);row.setMoney(amount);return amount;
    }
    public static int add(EntityManager em,UUID uuid,int amount){
        MoneyEntity row=locked(em,uuid);int total=Math.addExact(row.getMoney(),amount);
        if(total<0)throw new IllegalArgumentException("잔액이 부족합니다.");row.setMoney(total);return total;
    }
}
