package net.teujaem.usershop.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity
@Table(name="speconomy_sale_notices",indexes=@Index(name="speconomy_notice_seller",columnList="sellerUuid"))
public class SaleNoticeEntity {
    @Id private UUID id;
    @Column(nullable=false) private UUID sellerUuid;
    @Column(nullable=false,length=64) private String buyerName;
    @Column(nullable=false) private int amount;
    @Column(nullable=false) private long createdAt;
    private String leaseToken;
    @Column(nullable=false) private long leaseUntil;
    protected SaleNoticeEntity(){}
    public SaleNoticeEntity(UUID seller,String buyer,int amount){id=UUID.randomUUID();sellerUuid=seller;buyerName=buyer;this.amount=amount;createdAt=System.currentTimeMillis();}
    public UUID getId(){return id;}
    public String getBuyerName(){return buyerName;}
    public int getAmount(){return amount;}
    public String getLeaseToken(){return leaseToken;}
    public long getLeaseUntil(){return leaseUntil;}
    public void claim(String token,long until){leaseToken=token;leaseUntil=until;}
    public void release(){leaseToken=null;leaseUntil=0;}
}
