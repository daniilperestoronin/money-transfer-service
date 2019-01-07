package com.moneytransferservice.model;

import org.javamoney.moneta.Money;

import java.util.Objects;
import java.util.UUID;

public class Account {

    private UUID uuid;
    private String name;
    private Money money;

    public UUID getId() {
        return uuid;
    }

    public Account setId(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getName() {
        return name;
    }

    public Account setName(String name) {
        this.name = name;
        return this;
    }

    public Money getMoney() {
        return money;
    }

    public Account setMoney(Money money) {
        this.money = money;
        return this;
    }

    public boolean checkMoneyAvailability(Money money) {
        return this.money.isGreaterThan(money);
    }

    public Account withdrawMoney(Money money) {
        setMoney(this.money.subtract(money));
        return this;
    }

    public Account acceptMoney(Money money) {
        setMoney(this.money.add(money));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(uuid, account.uuid) &&
                Objects.equals(name, account.name) &&
                Objects.equals(money, account.money);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, money);
    }
}