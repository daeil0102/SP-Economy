package net.teujaem.usershop.model;

import java.util.UUID;

/**
 * 상점 데이터(판매글)에 변경이 생겼을 때 다른 서버에 브로드캐스트하는 정보.
 * 받는 쪽은 DB를 통째로 다시 읽지 않고, 이 정보를 참고해 필요한 만큼만
 * (바뀐 항목 하나, 혹은 전체 초기화) 다시 불러옵니다.
 */
public class ShopUpdateInfo {

    public enum Type {
        /** 새 판매글이 등록됨 */
        ADD,
        /** 판매글이 구매/취소 등으로 제거됨 */
        REMOVE,
        /** 판매글이 만료되어 판매자 보관함으로 이동함 */
        EXPIRE,
        /** 관리자가 전체 초기화함 (모든 서버가 캐시를 비워야 함) */
        RESET
    }

    public Type type;
    public UUID listingId;
    public UUID sellerUuid;
    public String originServer;

    public ShopUpdateInfo() {
    }

    public ShopUpdateInfo(Type type, UUID listingId, UUID sellerUuid, String originServer) {
        this.type = type;
        this.listingId = listingId;
        this.sellerUuid = sellerUuid;
        this.originServer = originServer;
    }
}