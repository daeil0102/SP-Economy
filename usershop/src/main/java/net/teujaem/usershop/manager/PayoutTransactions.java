package net.teujaem.usershop.manager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import net.teujaem.money.manager.MoneyTransactions;
import net.teujaem.usershop.entity.PendingEarningEntity;
import java.util.*;
public final class PayoutTransactions {
    private PayoutTransactions(){}
    public static Map<String,Integer> settle(EntityManager em,UUID seller){
        var balance=MoneyTransactions.locked(em,seller);
        var pending=em.createQuery("SELECT p FROM PendingEarningEntity p WHERE p.sellerUuid = :seller ORDER BY p.id",PendingEarningEntity.class)
                .setParameter("seller",seller).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        Map<String,Integer> result=new LinkedHashMap<>();int total=balance.getMoney();
        for(var row:pending){if(row.getAmount()<=0)throw new IllegalArgumentException("정산 금액 오류");total=Math.addExact(total,row.getAmount());result.merge(row.getBuyerName(),row.getAmount(),Math::addExact);}
        balance.setMoney(total);
        for(var entry:result.entrySet())em.persist(new net.teujaem.usershop.entity.SaleNoticeEntity(seller,entry.getKey(),entry.getValue()));
        for(var row:pending)em.remove(row);return result;
    }
}
