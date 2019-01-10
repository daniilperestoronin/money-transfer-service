package com.moneytransferservice;

import com.moneytransferservice.model.Account;
import com.moneytransferservice.model.Transfer;
import com.moneytransferservice.repository.Repository;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.zalando.jackson.datatype.money.MoneyModule;

import java.util.Objects;
import java.util.UUID;

public class Application extends AbstractVerticle {

    private static final int DEFAULT_PORT = 8080;
    private static final String CONTENT_TYPE_HEADER = HttpHeaderNames.CONTENT_TYPE.toString();
    private static final String APPLICATION_JSON = "application/json; charset=utf-8";
    private final Repository<Account> accountRepository;
    private final Repository<Transfer> transferRepository;

    public Application() {
        this.accountRepository = new Repository<>();
        this.transferRepository = new Repository<>();
    }

    Application(Repository<Account> accountRepository,
                Repository<Transfer> transferRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    @Override
    public void start(final Future<Void> future) {
        Json.mapper.registerModule(new MoneyModule());
        final var router = getRouter();
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(
                        config().getInteger("http.port", DEFAULT_PORT),
                        result -> {
                            if (result.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(result.cause());
                            }
                        }
                );
    }

    private Router getRouter() {
        final var router = Router.router(vertx);
        router.route("/*").handler(BodyHandler.create());
        router.post("/account/").handler(this::createAccount);
        router.get("/account/").handler(this::readAllAccounts);
        router.get("/account/:uuid").handler(this::readAccount);
        router.put("/account/").handler(this::updateAccount);
        router.delete("/account/:uuid").handler(this::deleteAccount);
        router.post("/transfer/commit").handler(this::commitMoneyTransfer);
        return router;
    }

    private void createAccount(final RoutingContext context) {
        try {
            final var account = Json.decodeValue(context.getBodyAsString(), Account.class);
            UUID uuid = accountRepository.create(account);
            account.setId(uuid);
            context.response()
                    .setStatusCode(HttpResponseStatus.CREATED.code())
                    .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                    .end(uuid.toString());
        } catch (IllegalArgumentException e) {
            context.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end();
        }
    }

    private void readAllAccounts(final RoutingContext context) {
        accountRepository.readAll().ifPresent(accounts ->
                context.response()
                        .setStatusCode(HttpResponseStatus.OK.code())
                        .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .end(Json.encode(accounts)));
    }

    private void readAccount(final RoutingContext context) {
        try {
            final var uuid = UUID.fromString(context.request().getParam("uuid"));
            final var accountOptional = accountRepository.read(uuid);
            if (accountOptional.isPresent()) {
                final var account = accountOptional.get();
                context.response()
                        .setStatusCode(HttpResponseStatus.OK.code())
                        .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .end(Json.encode(account));
            } else {
                context.response()
                        .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                        .end();
            }
        } catch (IllegalArgumentException e) {
            context.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end();
        }
    }

    private void updateAccount(final RoutingContext context) {
        try {
            final var account = Json.decodeValue(context.getBodyAsString(), Account.class);
            accountRepository.update(account.getId(), account);
            context.response()
                    .setStatusCode(HttpResponseStatus.OK.code())
                    .end();
        } catch (IllegalArgumentException e) {
            context.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end();
        }
    }

    private void deleteAccount(final RoutingContext context) {
        try {
            final var uuid = UUID.fromString(Objects.requireNonNull(context.request().getParam("uuid")));
            accountRepository.delete(uuid);
            context.response()
                    .setStatusCode(HttpResponseStatus.NO_CONTENT.code())
                    .end();
        } catch (IllegalArgumentException e) {
            context.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end();
        }
    }

    private void commitMoneyTransfer(final RoutingContext context) {
        try {
            final var transfer = Json.decodeValue(context.getBodyAsString(), Transfer.class);
            final var fromAccountOptional = accountRepository.read(transfer.getFromAccount());
            final var toAccountOptional = accountRepository.read(transfer.getToAccount());
            if (!fromAccountOptional.isPresent()) {
                context.response()
                        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                        .end("The account from which the transition is made does not exist");
                return;
            }
            if (!toAccountOptional.isPresent()) {
                context.response()
                        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                        .end("The account to which the transition is made does not exist");
                return;
            }
            final var fromAccount = fromAccountOptional.get();
            final var toAccount = toAccountOptional.get();
            if (!fromAccount.getMoney().getCurrency().equals(transfer.getAmount().getCurrency())
                    || !toAccount.getMoney().getCurrency().equals(transfer.getAmount().getCurrency())) {
                context.response()
                        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                        .end("Invalid transfer сurrency specified");
                return;
            }
            if (!fromAccount.checkMoneyAvailability(transfer.getAmount())) {
                context.response()
                        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                        .end("Invalid transfer amount specified");
                return;
            }
            fromAccount.withdrawMoney(transfer.getAmount());
            toAccount.acceptMoney(transfer.getAmount());
            UUID transferUuid = transferRepository.create(transfer);
            context.response()
                    .setStatusCode(HttpResponseStatus.OK.code())
                    .end(transferUuid.toString());
        } catch (IllegalArgumentException e) {
            context.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end();
        }
    }
}