package net.teujaem.money.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "speconomy_money")
public class MoneyEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID uuid;

    @Column(nullable = false)
    private int money;

    protected MoneyEntity() {
    }

    public MoneyEntity(UUID uuid, int money) {
        this.uuid = uuid;
        this.money = money;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }
}