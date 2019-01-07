package com.moneytransferservice.model;

import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account testAccount = new Account()
            .setId(UUID.randomUUID())
            .setName("Test")
            .setMoney(Money.of(100, "USD"));

    @Test
    void testCheckMoneyAvailability() {
        assertAll(
                () -> assertTrue(testAccount.checkMoneyAvailability(Money.of(20, "USD"))),
                () -> assertFalse(testAccount.checkMoneyAvailability(Money.of(200, "USD")))
        );
    }

    @Test
    void testWithdrawMoney() {
        assertAll(
                () -> assertEquals(
                        Money.of(90, "USD"),
                        testAccount.withdrawMoney(Money.of(10, "USD")).getMoney()
                ),
                () -> assertEquals(
                        Money.of(80, "USD"),
                        testAccount.withdrawMoney(Money.of(10, "USD")).getMoney()
                )
        );
    }

    @Test
    void testAcceptMoney() {
        assertAll(
                () -> assertEquals(
                        Money.of(110, "USD"),
                        testAccount.acceptMoney(Money.of(10, "USD")).getMoney()
                ),
                () -> assertEquals(
                        Money.of(120, "USD"),
                        testAccount.acceptMoney(Money.of(10, "USD")).getMoney()
                )
        );
    }
}
