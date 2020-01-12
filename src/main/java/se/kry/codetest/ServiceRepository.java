package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.UpdateResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServiceRepository {

  private final DBConnector connector;

  public ServiceRepository(final DBConnector connector) {
    this.connector = connector;
  }

  private static Service recordToService(JsonObject record) {
    return new Service(
        record.getLong("id"),
        record.getString("name"),
        record.getString("url"),
        record.getString("status"),
        record.getLong("created_at"));
  }

  public final Future<List<Service>> getAll() {
    final Future<List<Service>> future = Future.future();

    connector.query("SELECT id, url, name, created_at, status FROM service")
        .setHandler(asyncResult -> {
          if (asyncResult.failed()) {
            future.fail(asyncResult.cause());
          } else {
            future.complete(
                asyncResult
                    .result()
                    .getRows()
                    .stream()
                    .map(ServiceRepository::recordToService)
                    .collect(Collectors.toList()));
          }
        });

    return future;
  }


  public final Future<Optional<Service>> get(final Long id) {
    Objects.requireNonNull(id);

    final Future<Optional<Service>> future = Future.future();

    final JsonArray params = new JsonArray().add(id);
    connector.query("SELECT id, url, name, created_at, status FROM service WHERE id = ?", params)
        .setHandler(asyncResult -> {
          if (asyncResult.failed()) {
            future.fail(asyncResult.cause());
          } else {
            final Optional<Service> service;
            final List<JsonObject> rows = asyncResult.result().getRows();
            if (rows.size() != 1) {
              service = Optional.empty();
            } else {
              service = Optional.of(ServiceRepository.recordToService(rows.get(0)));
            }
            future.complete(service);
          }
        });

    return future;
  }

  public Future<Optional<Long>> save(final Service service) {
    Objects.requireNonNull(service);

    final Future<Optional<Long>> future = Future.future();
    final JsonArray params = new JsonArray()
        .add(service.getUrl())
        .add(service.getName())
        .add(service.getStatus());

    connector
        .update("INSERT INTO service (url, name, status) VALUES (?, ?, ?)", params)
        .setHandler(asyncResult -> {
          if (asyncResult.failed()) {
            future.fail(asyncResult.cause());
          } else {
            final UpdateResult result = asyncResult.result();
            final Optional<Long> id;
            if (result.getUpdated() == 1) {
              id = Optional.of(Long.parseLong(result.getKeys().getList().get(0).toString()));
            } else {
              id = Optional.empty();
            }
            future.complete(id);
          }
        });

    return future;
  }

  public Future<Boolean> update(final Long id, final String url, final String name) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(url);
    Objects.requireNonNull(name);

    final Future<Boolean> future = Future.future();
    final JsonArray params = new JsonArray()
        .add(url)
        .add(name)
        .add(id);

    connector
        .update("UPDATE service SET url = ?, name = ? WHERE id = ?", params)
        .setHandler(asyncResult -> {
          if (asyncResult.failed()) {
            future.fail(asyncResult.cause());
          } else {
            final Boolean isUpdated = asyncResult.result().getUpdated() == 1;
            future.complete(isUpdated);
          }
        });

    return future;
  }

  public Future<Boolean> updateStatus(final Long id, final String status) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(status);

    final Future<Boolean> future = Future.future();
    final JsonArray params = new JsonArray()
        .add(status)
        .add(id);

    connector
        .update("UPDATE service SET status = ? WHERE id = ?", params)
        .setHandler(asyncResult -> {
          if (asyncResult.failed()) {
            future.fail(asyncResult.cause());
          } else {
            final Boolean isUpdated = asyncResult.result().getUpdated() == 1;
            future.complete(isUpdated);
          }
        });

    return future;
  }

  public final Future<Boolean> delete(final Long id) {
    Objects.requireNonNull(id);

    final Future<Boolean> future = Future.future();
    final JsonArray params = new JsonArray().add(id);
    connector.update("DELETE FROM service WHERE id = ?", params)
        .setHandler(asyncResult -> {
          if (asyncResult.failed()) {
            future.fail(asyncResult.cause());
          } else {
            final Boolean isDeleted = asyncResult.result().getUpdated() == 1;
            future.complete(isDeleted);
          }
        });

    return future;
  }
}
