package com.moneytransferservice;

import com.moneytransferservice.model.Account;
import com.moneytransferservice.model.Transfer;
import io.restassured.RestAssured;
import io.vertx.core.json.Json;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.*;
import org.zalando.jackson.datatype.money.MoneyModule;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Application RESTfull Api Integration Tests")
class ApplicationIT {

    private Account testAccount;
    private Account testAccount2;

    @BeforeAll
    static void configureRestAssured() {
        Json.mapper.registerModule(new MoneyModule());
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    @AfterAll
    static void unconfigureRestAssured() {
        RestAssured.reset();
    }

    @BeforeEach
    void setData() {
        testAccount = new Account()
                .setName("Test Account")
                .setMoney(Money.of(1000, "USD"));
        testAccount2 = new Account()
                .setName("Test Account 2")
                .setMoney(Money.of(2000, "USD"));
    }

    @Test
    @DisplayName("Test account RESTfull API")
    void testAccountRestApi() {
        final var uuidString = given()
                .body(testAccount)
                .request().post("/account/")
                .thenReturn().asString();
        testAccount.setId(UUID.fromString(uuidString));
        assertThat(RestAssured.get("/account/" + testAccount.getId()).then()
                .assertThat()
                .statusCode(200)
                .extract().body().as(Account.class)).
                isEqualTo(testAccount);
        RestAssured.delete("/account/" + testAccount.getId()).then()
                .assertThat()
                .statusCode(204);
        RestAssured.get("/account/" + testAccount.getId()).then()
                .assertThat()
                .statusCode(404);
    }

    @Test
    @DisplayName("Test money transfer commit")
    void testTransferCommit() {
        final var uuidString = given()
                .body(testAccount)
                .request().post("/account/")
                .thenReturn().asString();
        testAccount.setId(UUID.fromString(uuidString));
        final var uuidString2 = given()
                .body(testAccount2)
                .request().post("/account/")
                .thenReturn().asString();
        testAccount2.setId(UUID.fromString(uuidString2));
        final var amountMoney = Money.of(500, "USD");
        final var transfer = new Transfer()
                .setToAccount(testAccount.getId())
                .setFromAccount(testAccount2.getId())
                .setAmount(amountMoney);
        given().body(transfer).request().post("/transfer/commit").then()
                .assertThat().statusCode(200);
        assertThat(RestAssured.get("/account/" + testAccount.getId()).then()
                .assertThat()
                .statusCode(200)
                .extract().body().as(Account.class)
                .getMoney()).
                isEqualTo(testAccount.getMoney().add(amountMoney));
        assertThat(RestAssured.get("/account/" + testAccount2.getId()).then()
                .assertThat()
                .statusCode(200)
                .extract().body().as(Account.class)
                .getMoney()).
                isEqualTo(testAccount2.getMoney().subtract(amountMoney));
    }

    @Test
    @DisplayName("Test negative scenarios for transfer money")
    void testWrongTransferCommit() {
        final var uuidString = given()
                .body(testAccount)
                .request().post("/account/")
                .thenReturn().asString();
        testAccount.setId(UUID.fromString(uuidString));
        final var uuidString2 = given()
                .body(testAccount2)
                .request().post("/account/")
                .thenReturn().asString();
        testAccount2.setId(UUID.fromString(uuidString2));
        final var amountMoney = Money.of(50000, "USD");
        final var exceedingTransfer = new Transfer()
                .setToAccount(testAccount.getId())
                .setFromAccount(testAccount2.getId())
                .setAmount(amountMoney);
        given().body(exceedingTransfer).request().post("/transfer/commit").then()
                .assertThat().statusCode(400);

        final var toNotExistAccountTransfer = new Transfer()
                .setToAccount(UUID.randomUUID())
                .setFromAccount(testAccount2.getId())
                .setAmount(amountMoney);
        given().body(toNotExistAccountTransfer).request().post("/transfer/commit").then()
                .assertThat().statusCode(400);

        final var fromNotExistAccountTransfer = new Transfer()
                .setToAccount(testAccount2.getId())
                .setFromAccount(UUID.randomUUID())
                .setAmount(amountMoney);
        given().body(fromNotExistAccountTransfer).request().post("/transfer/commit").then()
                .assertThat().statusCode(400);
    }

}
