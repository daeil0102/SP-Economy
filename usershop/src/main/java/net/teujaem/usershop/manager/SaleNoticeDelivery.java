package net.teujaem.usershop.manager;

import net.teujaem.usershop.entity.SaleNoticeEntity;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** All mutable state and chat operations belong to the server thread. */
public final class SaleNoticeDelivery {
    public interface Store {
        CompletableFuture<List<SaleNoticeEntity>> claim(UUID seller, String token, long now);
        CompletableFuture<Void> finish(UUID id, String token, boolean delivered);
    }
    public interface Recipient {
        boolean online(UUID seller);
        void send(UUID seller, SaleNoticeEntity notice);
    }
    private static final class Request { boolean again; }
    private final Store store;
    private final Recipient recipient;
    private final Consumer<Runnable> main;
    private final Consumer<Throwable> errors;
    private final LongSupplier clock;
    private final Map<UUID, Request> active = new HashMap<>();
    // Failed DB acknowledgements must not repeat an already sent chat on retry.
    private final Set<UUID> sent = new HashSet<>();

    public SaleNoticeDelivery(Store store, Recipient recipient, Consumer<Runnable> main,
                              LongSupplier clock, Consumer<Throwable> errors) {
        this.store=store; this.recipient=recipient; this.main=main; this.clock=clock; this.errors=errors;
    }
    public void request(UUID seller) { main.accept(() -> begin(seller)); }
    private void begin(UUID seller) {
        if (!recipient.online(seller)) return;
        Request existing=active.get(seller);
        if (existing!=null) { existing.again=true; return; }
        Request request=new Request(); active.put(seller,request);
        String token=UUID.randomUUID().toString();
        CompletableFuture<List<SaleNoticeEntity>> claim;
        try { claim=store.claim(seller,token,clock.getAsLong()); }
        catch (Throwable error) { complete(seller,request,false,error); return; }
        claim.whenComplete((rows,error) -> main.accept(() -> {
            if (error!=null) { complete(seller,request,false,error); return; }
            List<CompletableFuture<?>> acknowledgements=new ArrayList<>();
            for (SaleNoticeEntity notice:rows) {
                boolean delivered=sent.contains(notice.getId());
                try {
                    if (!delivered && recipient.online(seller) && clock.getAsLong()<notice.getLeaseUntil()-1000) {
                        recipient.send(seller,notice); sent.add(notice.getId()); delivered=true;
                    }
                    boolean wasDelivered=delivered;
                    acknowledgements.add(store.finish(notice.getId(),token,delivered)
                            .thenRun(() -> main.accept(() -> { if(wasDelivered) sent.remove(notice.getId()); })));
                } catch (Throwable failure) {
                    acknowledgements.add(CompletableFuture.failedFuture(failure));
                }
            }
            CompletableFuture.allOf(acknowledgements.toArray(CompletableFuture[]::new))
                    .whenComplete((ignored,failure) -> main.accept(() -> complete(seller,request,rows.size()==20,failure)));
        }));
    }
    private void complete(UUID seller,Request request,boolean more,Throwable error) {
        active.remove(seller,request);
        if (error!=null) { errors.accept(error); return; }
        if ((request.again||more)&&recipient.online(seller)) begin(seller);
    }
}
