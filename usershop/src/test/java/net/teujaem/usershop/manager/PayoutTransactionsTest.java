package net.teujaem.usershop.manager;
import net.teujaem.money.entity.MoneyEntity;
import net.teujaem.money.manager.MoneyTransactions;
import net.teujaem.usershop.entity.SaleNoticeEntity;
import net.teujaem.usershop.manager.SaleNoticeTransactions;
import net.teujaem.usershop.manager.SaleNoticeDelivery;
import net.teujaem.usershop.entity.PendingEarningEntity;
import org.hibernate.cfg.Configuration;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class PayoutTransactionsTest {
    private SessionFactory factory;
    @BeforeEach void setup(){factory=new Configuration().addAnnotatedClass(MoneyEntity.class).addAnnotatedClass(PendingEarningEntity.class).addAnnotatedClass(SaleNoticeEntity.class)
            .setProperty("hibernate.connection.driver_class","org.h2.Driver")
            .setProperty("hibernate.connection.url","jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000")
            .setProperty("hibernate.hbm2ddl.auto","create-drop").setProperty("hibernate.show_sql","false")
            .setProperty("hibernate.connection.pool_size","20").buildSessionFactory();}
    @AfterEach void close(){factory.close();}
    private <T>T tx(Function<EntityManager,T> action){try(var em=factory.createEntityManager()){var t=em.getTransaction();t.begin();try{T value=action.apply(em);t.commit();return value;}catch(RuntimeException e){t.rollback();throw e;}}}
    private void pending(UUID id,String buyer,int amount){tx(em->{em.persist(new PendingEarningEntity(id,buyer,amount));return null;});}
    private int balance(UUID id){return tx(em->em.find(MoneyEntity.class,id).getMoney());}
    private long rows(UUID id){return tx(em->em.createQuery("SELECT COUNT(p) FROM PendingEarningEntity p WHERE p.sellerUuid=:id",Long.class).setParameter("id",id).getSingleResult());}
    @Test void paysOfflineSellerWithoutAnyBukkitPlayer(){UUID seller=UUID.randomUUID();pending(seller,"buyer",300);assertEquals(Map.of("buyer",300),tx(em->PayoutTransactions.settle(em,seller)));assertEquals(300,balance(seller));assertEquals(0,rows(seller));}
    @Test void replayDoesNotPayTwice(){UUID seller=UUID.randomUUID();pending(seller,"buyer",75);tx(em->PayoutTransactions.settle(em,seller));assertTrue(tx(em->PayoutTransactions.settle(em,seller)).isEmpty());assertEquals(75,balance(seller));}
    @Test void overflowRollsBackCreditAndPendingDeletion(){UUID seller=UUID.randomUUID();tx(em->{em.persist(new MoneyEntity(seller,Integer.MAX_VALUE-5));return null;});pending(seller,"buyer",10);assertThrows(ArithmeticException.class,()->tx(em->PayoutTransactions.settle(em,seller)));assertEquals(Integer.MAX_VALUE-5,balance(seller));assertEquals(1,rows(seller));}
    @Test void concurrentServersConsumeOnce() throws Exception {
        UUID seller=UUID.randomUUID();tx(em->{em.persist(new MoneyEntity(seller,50));return null;});for(int i=0;i<50;i++)pending(seller,"buyer",10);
        var pool=Executors.newFixedThreadPool(8);try{List<Future<Integer>> futures=new ArrayList<>();for(int i=0;i<8;i++)futures.add(pool.submit(()->tx(em->PayoutTransactions.settle(em,seller)).values().stream().mapToInt(Integer::intValue).sum()));int total=0;for(var f:futures)total+=f.get(30,TimeUnit.SECONDS);assertEquals(500,total);assertEquals(550,balance(seller));assertEquals(0,rows(seller));}finally{pool.shutdownNow();}
    }
    @Test void concurrentBalanceChangesDoNotOverwriteSellerCredit() throws Exception {
        UUID seller=UUID.randomUUID();tx(em->{em.persist(new MoneyEntity(seller,1000));return null;});pending(seller,"buyer",500);
        var pool=Executors.newFixedThreadPool(8);try{List<Future<?>> futures=new ArrayList<>();futures.add(pool.submit(()->tx(em->PayoutTransactions.settle(em,seller))));for(int i=0;i<40;i++)futures.add(pool.submit(()->tx(em->MoneyTransactions.add(em,seller,10))));for(var f:futures)f.get(30,TimeUnit.SECONDS);assertEquals(1900,balance(seller));}finally{pool.shutdownNow();}
    }
    @Test void offlineSaleNoticeIsDurableAndDeliveredOnlyOnce(){
        UUID seller=UUID.randomUUID();pending(seller,"buyer",250);tx(em->PayoutTransactions.settle(em,seller));
        var notices=tx(em->SaleNoticeTransactions.claim(em,seller,"server-b",1000));assertEquals(1,notices.size());assertEquals(250,notices.get(0).getAmount());
        assertTrue(tx(em->SaleNoticeTransactions.claim(em,seller,"server-a",1001)).isEmpty());
        tx(em->{SaleNoticeTransactions.finish(em,notices.get(0).getId(),"wrong-server",true);return null;});
        tx(em->{SaleNoticeTransactions.finish(em,notices.get(0).getId(),"server-b",true);return null;});
        assertTrue(tx(em->SaleNoticeTransactions.claim(em,seller,"server-c",32000)).isEmpty());assertEquals(250,balance(seller));
    }
    @Test void disconnectReleasesNoticeAndServerCrashLeaseExpires(){
        UUID seller=UUID.randomUUID();pending(seller,"buyer",30);tx(em->PayoutTransactions.settle(em,seller));
        var first=tx(em->SaleNoticeTransactions.claim(em,seller,"old",1000));
        tx(em->{SaleNoticeTransactions.finish(em,first.get(0).getId(),"old",false);return null;});
        assertEquals(1,tx(em->SaleNoticeTransactions.claim(em,seller,"new",1100)).size());
        assertTrue(tx(em->SaleNoticeTransactions.claim(em,seller,"other",1200)).isEmpty());
        assertEquals(1,tx(em->SaleNoticeTransactions.claim(em,seller,"restart",32000)).size());assertEquals(30,balance(seller));
    }
    @Test void concurrentServersCannotClaimSameChatNotice()throws Exception{
        UUID seller=UUID.randomUUID();pending(seller,"buyer",15);tx(em->PayoutTransactions.settle(em,seller));
        var pool=Executors.newFixedThreadPool(4);try{List<Future<Integer>> futures=new ArrayList<>();for(int i=0;i<4;i++){String token="node-"+i;futures.add(pool.submit(()->tx(em->SaleNoticeTransactions.claim(em,seller,token,1000)).size()));}int total=0;for(var f:futures)total+=f.get(20,TimeUnit.SECONDS);assertEquals(1,total);}finally{pool.shutdownNow();}
    }

    private final class DeliveryServer {
        long now=1000; boolean online=true, holdClaim, failAck;
        final Queue<Runnable> main=new ArrayDeque<>();
        final List<UUID> chats=new ArrayList<>();
        CompletableFuture<List<SaleNoticeEntity>> held;
        List<SaleNoticeEntity> heldRows;
        final SaleNoticeDelivery delivery=new SaleNoticeDelivery(new SaleNoticeDelivery.Store(){
            public CompletableFuture<List<SaleNoticeEntity>> claim(UUID seller,String token,long time){
                var rows=tx(em->SaleNoticeTransactions.claim(em,seller,token,time));
                if(holdClaim){holdClaim=false;heldRows=rows;held=new CompletableFuture<>();return held;}
                return CompletableFuture.completedFuture(rows);
            }
            public CompletableFuture<Void> finish(UUID id,String token,boolean delivered){
                if(failAck){failAck=false;return CompletableFuture.failedFuture(new IllegalStateException("DB unavailable"));}
                tx(em->{SaleNoticeTransactions.finish(em,id,token,delivered);return null;});return CompletableFuture.completedFuture(null);
            }
        },new SaleNoticeDelivery.Recipient(){
            public boolean online(UUID seller){return online;}
            public void send(UUID seller,SaleNoticeEntity notice){assertTrue(online);chats.add(notice.getId());}
        },main::add,()->now,error->{});
        void pump(){int count=0;while(!main.isEmpty()){assertTrue(count++<200,"unbounded delivery loop");main.remove().run();}}
        void poll(UUID seller){delivery.request(seller);pump();}
        void release(){held.complete(heldRows);pump();}
    }
    @Test void sameServerSaleSendsOneChatDespiteRepeatedEvents(){
        UUID seller=UUID.randomUUID();pending(seller,"buyer",200);tx(em->PayoutTransactions.settle(em,seller));
        var server=new DeliveryServer();for(int i=0;i<10;i++)server.delivery.request(seller);server.pump();
        server.poll(seller);assertEquals(1,server.chats.size());assertEquals(200,balance(seller));
    }
    @Test void otherServerAndOfflineSaleIsDeliveredOnceOnJoin(){
        UUID seller=UUID.randomUUID();pending(seller,"buyer",300);tx(em->PayoutTransactions.settle(em,seller));
        var buyerServer=new DeliveryServer();buyerServer.online=false;buyerServer.poll(seller);assertTrue(buyerServer.chats.isEmpty());
        var sellerServer=new DeliveryServer();sellerServer.online=false;sellerServer.poll(seller);
        sellerServer.online=true;sellerServer.poll(seller);sellerServer.poll(seller);
        assertEquals(1,sellerServer.chats.size());assertTrue(buyerServer.chats.isEmpty());
    }
    @Test void joinQueryOverlappingSettlementRechecksInsteadOfDroppingRequest(){
        UUID seller=UUID.randomUUID();var server=new DeliveryServer();server.holdClaim=true;server.poll(seller);
        pending(seller,"buyer",70);tx(em->PayoutTransactions.settle(em,seller));server.poll(seller);
        server.release();assertEquals(1,server.chats.size());
    }
    @Test void disconnectBeforeSendKeepsNoticeForNextLogin(){
        UUID seller=UUID.randomUUID();pending(seller,"buyer",90);tx(em->PayoutTransactions.settle(em,seller));
        var server=new DeliveryServer();server.holdClaim=true;server.poll(seller);server.online=false;server.release();
        assertTrue(server.chats.isEmpty());server.online=true;server.poll(seller);server.poll(seller);assertEquals(1,server.chats.size());
    }
    @Test void failedAcknowledgementRetriesWithoutRepeatingChat(){
        UUID seller=UUID.randomUUID();pending(seller,"buyer",40);tx(em->PayoutTransactions.settle(em,seller));
        var server=new DeliveryServer();server.failAck=true;server.poll(seller);assertEquals(1,server.chats.size());
        server.now=32000;server.poll(seller);server.poll(seller);assertEquals(1,server.chats.size());
        assertTrue(tx(em->SaleNoticeTransactions.claim(em,seller,"verify",64000)).isEmpty());
    }
    @Test void expiredOldServerCallbackCannotSendDuplicateAfterMove(){
        UUID seller=UUID.randomUUID();pending(seller,"buyer",50);tx(em->PayoutTransactions.settle(em,seller));
        var oldServer=new DeliveryServer();oldServer.holdClaim=true;oldServer.poll(seller);
        var newServer=new DeliveryServer();newServer.now=40000;newServer.poll(seller);
        oldServer.now=40000;oldServer.release();assertEquals(1,newServer.chats.size());assertTrue(oldServer.chats.isEmpty());
    }
    @Test void loginDrainsMoreThanOneBatchAndNextLoginDoesNotRepeat(){
        UUID seller=UUID.randomUUID();for(int i=0;i<25;i++)pending(seller,"buyer-"+i,1);tx(em->PayoutTransactions.settle(em,seller));
        var server=new DeliveryServer();server.poll(seller);assertEquals(25,server.chats.size());assertEquals(25,new HashSet<>(server.chats).size());
        server.online=false;server.poll(seller);server.online=true;server.poll(seller);assertEquals(25,server.chats.size());
    }
}
