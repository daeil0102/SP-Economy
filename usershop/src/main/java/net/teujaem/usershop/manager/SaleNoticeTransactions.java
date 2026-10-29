package net.teujaem.usershop.manager;
import jakarta.persistence.*;
import net.teujaem.usershop.entity.SaleNoticeEntity;
import java.util.*;
public final class SaleNoticeTransactions {
    private SaleNoticeTransactions(){}
    public static List<SaleNoticeEntity> claim(EntityManager em,UUID seller,String token,long now){
        var rows=em.createQuery("SELECT n FROM SaleNoticeEntity n WHERE n.sellerUuid=:seller ORDER BY n.createdAt,n.id",SaleNoticeEntity.class)
                .setParameter("seller",seller).setMaxResults(20).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        List<SaleNoticeEntity> claimed=new ArrayList<>();
        for(var row:rows)if(row.getLeaseUntil()<=now){row.claim(token,now+30000);claimed.add(row);}return claimed;
    }
    public static void finish(EntityManager em,UUID id,String token,boolean delivered){
        var row=em.find(SaleNoticeEntity.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(row==null||!Objects.equals(token,row.getLeaseToken()))return;
        if(delivered)em.remove(row);else row.release();
    }
}
