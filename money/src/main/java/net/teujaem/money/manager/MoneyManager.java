package net.teujaem.money.manager;
import net.teujaem.jpalib.jpa.JpaManager;
import net.teujaem.money.entity.MoneyEntity;
import net.teujaem.spFramework.SPFramework;
import net.teujaem.spFramework.api.ProxyData;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import jakarta.persistence.EntityManager;

public class MoneyManager {
    private static final class Session {int balance; boolean ready; long revision;}
    private final JpaManager database;
    private final Map<UUID,Session> sessions=new ConcurrentHashMap<>();
    private final Map<UUID,CompletableFuture<Integer>> tails=new HashMap<>();
    public MoneyManager(){database=SPFramework.getInstance().getDatabaseController().createJpaManager(MoneyEntity.class);}
    public void load(Player player){sessions.put(player.getUniqueId(),new Session());refresh(player.getUniqueId());}
    public boolean isLoaded(Player player){Session s=sessions.get(player.getUniqueId());if(s==null)return false;synchronized(s){return s.ready;}}
    public int get(Player player){Session s=sessions.get(player.getUniqueId());if(s==null)return 0;synchronized(s){return s.ready?s.balance:0;}}
    public void update(Player player,int amount){UUID id=player.getUniqueId();mutate(id,em->MoneyTransactions.set(em,id,amount),amount,true);}
    public void add(Player player,int amount){
        if(amount<0)throw new IllegalArgumentException("추가 금액은 음수일 수 없습니다.");
        UUID id=player.getUniqueId();mutate(id,em->MoneyTransactions.add(em,id,amount),amount,false);
    }
    public boolean remove(Player player,int amount){
        if(amount<0)throw new IllegalArgumentException("차감 금액은 음수일 수 없습니다.");
        if(!isLoaded(player)||get(player)<amount)return false;
        UUID id=player.getUniqueId();mutate(id,em->MoneyTransactions.add(em,id,-amount),-amount,false);return true;
    }
    public boolean has(Player player,int amount){return isLoaded(player)&&amount>=0&&get(player)>=amount;}
    private CompletableFuture<Integer> serial(UUID uuid,Function<EntityManager,Integer> action){
        CompletableFuture<Integer> next;
        synchronized(tails){CompletableFuture<Integer> previous=tails.get(uuid);
            next=(previous==null?CompletableFuture.completedFuture(0):previous.handle((v,e)->0))
                    .thenCompose(ignored->attempt(action,0));tails.put(uuid,next);
        }
        CompletableFuture<Integer> result=next;
        next.whenComplete((v,e)->{synchronized(tails){tails.remove(uuid,result);}});return next;
    }
    private CompletableFuture<Integer> attempt(Function<EntityManager,Integer> action,int retries){
        return database.executeAsync(action).handle((v,error)->{
            if(error==null)return CompletableFuture.completedFuture(v);
            if(retries<2&&retryable(error))return attempt(action,retries+1);
            return CompletableFuture.<Integer>failedFuture(error);
        }).thenCompose(Function.identity());
    }
    private static boolean retryable(Throwable e){
        for(Throwable t=e;t!=null;t=t.getCause())if(t instanceof java.sql.SQLException sql){
            String state=sql.getSQLState();
            if("23505".equals(state)||"23000".equals(state)||"40001".equals(state)||sql.getErrorCode()==1213)return true;
        }
        return false;
    }
    private void mutate(UUID uuid,Function<EntityManager,Integer> action,int value,boolean absolute){
        Session session=sessions.get(uuid);long version=0;
        if(session!=null)synchronized(session){
            int balance=session.ready?(absolute?value:Math.addExact(session.balance,value)):session.balance;
            version=++session.revision;if(session.ready)session.balance=balance;
        }
        final long expected=version;
        serial(uuid,action).whenComplete((balance,error)->{
            if(error==null){
                if(session!=null&&sessions.get(uuid)==session)synchronized(session){if(session.revision==expected){session.balance=balance;session.ready=true;}}
                notifyChanged(uuid);
            }else{
                net.teujaem.money.Money.getInstance().getLogger().warning("잔액 저장 실패; 캐시를 재조회합니다: "+error.getMessage());refresh(uuid);
            }
        });
    }
    public CompletableFuture<Integer> refresh(UUID uuid){
        Session session=sessions.get(uuid);if(session==null)return CompletableFuture.completedFuture(0);
        long expected;synchronized(session){expected=session.revision;}
        return serial(uuid,em->{MoneyEntity row=em.find(MoneyEntity.class,uuid);return row==null?0:row.getMoney();}).thenApply(balance->{
            if(sessions.get(uuid)==session)synchronized(session){if(session.revision==expected){session.balance=balance;session.ready=true;}}
            return balance;
        }).exceptionally(error->{net.teujaem.money.Money.getInstance().getLogger().warning("잔액 조회 실패: "+error.getMessage());return 0;});
    }
    public void notifyChanged(UUID uuid){
        refresh(uuid);
        try{ProxyData.sendToServer("SP-Money","balance_changed",uuid.toString());}
        catch(RuntimeException e){net.teujaem.money.Money.getInstance().getLogger().fine("잔액 동기화 알림 실패; 주기 조회로 복구: "+e.getMessage());}
    }
    public void unload(Player player){sessions.remove(player.getUniqueId());/* Never merge stale cached balances on quit. */}
    public void shutdown(){
        CompletableFuture<?>[] pending;synchronized(tails){pending=tails.values().toArray(CompletableFuture[]::new);}
        try{CompletableFuture.allOf(pending).get(5,TimeUnit.SECONDS);}catch(Exception e){net.teujaem.money.Money.getInstance().getLogger().warning("종료 시 잔액 작업 대기: "+e.getMessage());}
        sessions.clear();
    }
}
