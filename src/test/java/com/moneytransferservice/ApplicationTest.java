package com.moneytransferservice;

import com.moneytransferservice.model.Account;
import com.moneytransferservice.model.Transfer;
import com.moneytransferservice.repository.Repository;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.zalando.jackson.datatype.money.MoneyModule;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Application Restfull Api test")
@ExtendWith(VertxExtension.class)
class ApplicationTest {

    private final Account testAccount = new Account()
            .setName("Test Account")
            .setMoney(Money.of(1000, "USD"));
    private final Account testAccount2 = new Account()
            .setName("Test Account 2")
            .setMoney(Money.of(1500, "USD"));
    private final List accounts = List.of(testAccount, testAccount2);

    @BeforeAll
    static void prepare() {
        Json.mapper.registerModule(new MoneyModule());
    }

    @Test
    @DisplayName("Account creation and reading test")
    void testAccountApi(Vertx vertx, VertxTestContext testContext) {
        WebClient webClient = WebClient.create(vertx);
        vertx.deployVerticle(new Application(), testContext.succeeding(id -> {
            webClient.get(8080, "localhost", "/account/")
                    .as(BodyCodec.json(List.class))
                    .send(testContext.succeeding(resp -> {
                        testContext.verify(() -> {
                            assertThat(resp.statusCode()).isEqualTo(200);
                            assertThat(resp.body()).isEqualTo(Collections.emptyList());
                            testContext.completeNow();
                        });
                    }));
            webClient.post(8080, "localhost", "/account/")
                    .as(BodyCodec.string())
                    .sendJson(testAccount, testContext.succeeding(resp -> {
                        testContext.verify(() -> {
                            assertThat(resp.statusCode()).isEqualTo(201);
                            testAccount.setId(UUID.fromString(resp.body()));
                            testContext.completeNow();
                        });
                    }));
            webClient.get(8080, "localhost", "/account/" + testAccount.getId())
                    .as(BodyCodec.json(Account.class))
                    .send(testContext.succeeding(resp -> {
                        testContext.verify(() -> {
                            assertThat(resp.statusCode()).isEqualTo(200);
                            assertThat(resp.body()).isEqualTo(testAccount);
                            testContext.completeNow();
                        });
                    }));
            webClient.post(8080, "localhost", "/account/")
                    .as(BodyCodec.string())
                    .sendJson(testAccount2, testContext.succeeding(resp -> {
                        testContext.verify(() -> {
                            assertThat(resp.statusCode()).isEqualTo(201);
                            testAccount2.setId(UUID.fromString(resp.body()));
                            testContext.completeNow();
                        });
                    }));
            webClient.get(8080, "localhost", "/account/" + testAccount2.getId())
                    .as(BodyCodec.json(Account.class))
                    .send(testContext.succeeding(resp -> {
                        testContext.verify(() -> {
                            assertThat(resp.statusCode()).isEqualTo(200);
                            assertThat(resp.body()).isEqualTo(testAccount2);
                            testContext.completeNow();
                        });
                    }));
            webClient.get(8080, "localhost", "/account/")
                    .as(BodyCodec.json(List.class))
                    .send(testContext.succeeding(resp -> {
                        testContext.verify(() -> {
                            assertThat(resp.statusCode()).isEqualTo(200);
                            assertThat(resp.body()).isEqualTo(accounts);
                            testContext.completeNow();
                        });
                    }));
        }));
    }

    @Test
    @DisplayName("Money transfer test")
    void testTransfer(Vertx vertx, VertxTestContext testContext) {

        final Repository<Account> accountRepository = new Repository<>();
        final Repository<Transfer> transferRepository = new Repository<>();
        final var uuid = accountRepository.create(testAccount);
        final var uuid2 = accountRepository.create(testAccount2);
        testAccount.setId(uuid);
        testAccount2.setId(uuid2);
        final var amountMoney = Money.of(100, "USD");
        final var withdrawAccountMoney = testAccount.getMoney().subtract(amountMoney);
        final var addAccountMoney = testAccount2.getMoney().add(amountMoney);
        final var transfer = new Transfer()
                .setToAccount(testAccount2.getId())
                .setFromAccount(testAccount.getId())
                .setAmount(amountMoney);

        WebClient webClient = WebClient.create(vertx);
        vertx.deployVerticle(new Application(accountRepository, transferRepository)
                , testContext.succeeding(id -> {
                    webClient.post(8080, "localhost", "/transfer/commit")
                            .as(BodyCodec.string())
                            .sendJson(transfer, testContext.succeeding(trResp -> {
                                testContext.verify(() -> {
                                    assertThat(trResp.statusCode()).isEqualTo(200);
                                    assertThat(testAccount.getMoney()).isEqualTo(withdrawAccountMoney);
                                    assertThat(testAccount2.getMoney()).isEqualTo(addAccountMoney);
                                    testContext.completeNow();
                                });
                            }));
                }));
    }
}
