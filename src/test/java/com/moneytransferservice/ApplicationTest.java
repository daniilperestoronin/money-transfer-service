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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.zalando.jackson.datatype.money.MoneyModule;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Application RESTfull Api test")
@ExtendWith(VertxExtension.class)
class ApplicationTest {

    private Account testAccount;
    private Account testAccount2;
    private Account testAccount3;
    private Repository<Account> testAccountRepository;
    private Repository<Transfer> testTransferRepository;

    @BeforeAll
    static void prepare() {
        Json.mapper.registerModule(new MoneyModule());
    }

    @BeforeEach
    void testDataPreparation() {
        testAccount = new Account()
                .setName("Test Account")
                .setMoney(Money.of(1000, "USD"));
        testAccount2 = new Account()
                .setName("Test Account 2")
                .setMoney(Money.of(1500, "USD"));
        testAccount3 = new Account()
                .setName("Test Account 3")
                .setMoney(Money.of(2000, "USD"));
        testAccountRepository = new Repository<>();
        testTransferRepository = new Repository<>();
        final var uuid = testAccountRepository.create(testAccount);
        final var uuid2 = testAccountRepository.create(testAccount2);
        final var uuid3 = testAccountRepository.create(testAccount3);
        testAccount.setId(uuid);
        testAccount2.setId(uuid2);
        testAccount3.setId(uuid3);
    }

    @Test
    @DisplayName("readAll Accounts test")
    void testReadAllAccounts(Vertx vertx, VertxTestContext testContext) {
        WebClient webClient = WebClient.create(vertx);
        vertx.deployVerticle(new Application(testAccountRepository, testTransferRepository),
                testContext.succeeding(id ->
                        webClient.get(8080, "localhost", "/account/")
                                .as(BodyCodec.string())
                                .send(testContext.succeeding(resp ->
                                        testContext.verify(() -> {
                                            assertThat(resp.statusCode()).isEqualTo(200);
                                            assertThat(resp.body()).contains(
                                                    Json.encode(testAccount),
                                                    Json.encode(testAccount2),
                                                    Json.encode(testAccount3)
                                            );
                                            testContext.completeNow();
                                        })))));
    }

    @Test
    @DisplayName("read Account test")
    void testReadAccount(Vertx vertx, VertxTestContext testContext) {
        WebClient webClient = WebClient.create(vertx);
        vertx.deployVerticle(new Application(testAccountRepository, testTransferRepository),
                testContext.succeeding(id ->
                        webClient.get(8080, "localhost",
                                "/account/" + testAccount.getId().toString())
                                .as(BodyCodec.json(Account.class))
                                .send(testContext.succeeding(resp ->
                                        testContext.verify(() -> {
                                            assertThat(resp.statusCode()).isEqualTo(200);
                                            assertThat(resp.body()).isEqualTo(testAccount);
                                            testContext.completeNow();
                                        })))));
    }

    @Test
    @DisplayName("create Account test")
    void testCreateAccount(Vertx vertx, VertxTestContext testContext) {
        final var creationAccount = new Account()
                .setName("Creation account")
                .setMoney(Money.of(100, "USD"));
        WebClient webClient = WebClient.create(vertx);
        vertx.deployVerticle(new Application(testAccountRepository, testTransferRepository),
                testContext.succeeding(id ->
                        webClient.post(8080, "localhost", "/account/")
                                .as(BodyCodec.string())
                                .sendJson(creationAccount, testContext.succeeding(resp ->
                                        testContext.verify(() -> {
                                            assertThat(resp.statusCode()).isEqualTo(201);
                                            final var creationAccountUuid = UUID.fromString(resp.body());
                                            creationAccount.setId(creationAccountUuid);
                                            assertThat(testAccountRepository.read(creationAccountUuid).get())
                                                    .isEqualTo(creationAccount);
                                            testContext.completeNow();
                                        })))));
    }

    @Test
    @DisplayName("update Account test")
    void testUpdateAccount(Vertx vertx, VertxTestContext testContext) {
        final var updatingAccount = new Account()
                .setId(testAccount.getId())
                .setName("Updating account")
                .setMoney(Money.of(100, "USD"));
        WebClient webClient = WebClient.create(vertx);
        vertx.deployVerticle(new Application(testAccountRepository, testTransferRepository),
                testContext.succeeding(id ->
                        webClient.put(8080, "localhost", "/account/")
                                .as(BodyCodec.string())
                                .sendJson(updatingAccount, testContext.succeeding(resp ->
                                        testContext.verify(() -> {
                                            assertThat(resp.statusCode()).isEqualTo(200);
                                            assertThat(testAccountRepository
                                                    .read(updatingAccount.getId()).get())
                                                    .isEqualTo(updatingAccount);
                                            testContext.completeNow();
                                        })))));
    }

    @Test
    @DisplayName("delete Account test")
    void testDeleteAccount(Vertx vertx, VertxTestContext testContext) {
        WebClient webClient = WebClient.create(vertx);
        vertx.deployVerticle(new Application(testAccountRepository, testTransferRepository),
                testContext.succeeding(id ->
                        webClient.delete(8080, "localhost", "/account/" + testAccount.getId().toString())
                                .as(BodyCodec.json(Account.class))
                                .send(testContext.succeeding(resp ->
                                        testContext.verify(() -> {
                                            assertThat(resp.statusCode()).isEqualTo(204);
                                            assertThat(testAccountRepository
                                                    .read(testAccount.getId()).isPresent())
                                                    .isFalse();
                                            testContext.completeNow();
                                        })))));
    }

    @Test
    @DisplayName("commit Money transfer test")
    void testTransfer(Vertx vertx, VertxTestContext testContext) {
        final var amountMoney = Money.of(100, "USD");
        final var withdrawAccountMoney = testAccount.getMoney().subtract(amountMoney);
        final var addAccountMoney = testAccount2.getMoney().add(amountMoney);
        final var transfer = new Transfer()
                .setToAccount(testAccount2.getId())
                .setFromAccount(testAccount.getId())
                .setAmount(amountMoney);

        WebClient webClient = WebClient.create(vertx);
        vertx.deployVerticle(new Application(testAccountRepository, testTransferRepository),
                testContext.succeeding(id ->
                        webClient.post(8080, "localhost", "/transfer/commit")
                                .as(BodyCodec.string())
                                .sendJson(transfer, testContext.succeeding(trResp ->
                                        testContext.verify(() -> {
                                            assertThat(trResp.statusCode()).isEqualTo(200);
                                            assertThat(testAccount.getMoney()).isEqualTo(withdrawAccountMoney);
                                            assertThat(testAccount2.getMoney()).isEqualTo(addAccountMoney);
                                            testContext.completeNow();
                                        })))));
    }
}
