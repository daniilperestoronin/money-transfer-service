package com.moneytransferservice.model;

import org.javamoney.moneta.Money;

import java.util.Objects;
import java.util.UUID;

public class Transfer {

    private UUID uuid;
    private UUID fromAccount;
    private UUID toAccount;
    private Money amount;

    public UUID getId() {
        return uuid;
    }

    public Transfer setId(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    public UUID getFromAccount() {
        return fromAccount;
    }

    public Transfer setFromAccount(UUID fromAccount) {
        this.fromAccount = fromAccount;
        return this;
    }

    public UUID getToAccount() {
        return toAccount;
    }

    public Transfer setToAccount(UUID toAccount) {
        this.toAccount = toAccount;
        return this;
    }

    public Money getAmount() {
        return amount;
    }

    public Transfer setAmount(Money amount) {
        this.amount = amount;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transfer transfer = (Transfer) o;
        return Objects.equals(uuid, transfer.uuid) &&
                Objects.equals(fromAccount, transfer.fromAccount) &&
                Objects.equals(toAccount, transfer.toAccount) &&
                Objects.equals(amount, transfer.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, fromAccount, toAccount, amount);
    }
}