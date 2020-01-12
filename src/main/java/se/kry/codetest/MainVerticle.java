package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  private static final String POLLER_UPDATES_TOPIC = "poller.updates";

  private ServiceRepository serviceRepository;
  private DBConnector connector;
  private BackgroundPoller poller;

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    serviceRepository = new ServiceRepository(connector);
    poller = new BackgroundPoller(WebClient.create(vertx));

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    setRoutes(router);

    vertx.setPeriodic(1000 * 10, timerId -> {
      serviceRepository.getAll().setHandler(asyncResult -> {
        if (asyncResult.succeeded()) {
          final List<Service> services = asyncResult.result();
          poller.pollServices(services).setHandler(asyncPoolResult -> {
            if (asyncPoolResult.succeeded()) {

              final Map<Long, String> result = asyncPoolResult.result();
              for (final Map.Entry<Long, String> entry : result.entrySet()) {
                serviceRepository.updateStatus(entry.getKey(), entry.getValue());
              }

              vertx.eventBus().publish("poller.updates", JsonObject.mapFrom(result).encode());
            }
          });
        }
      });
    });

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            LOGGER.info("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });
  }

  private void setRoutes(Router router) {
    router.route("/*").handler(StaticHandler.create());

    router.route("/eventbus/*").handler(eventBusHandler());

    router.get("/service").handler(req -> {
      serviceRepository.getAll().setHandler(asyncResult -> {
        if (asyncResult.failed()) {
          handleError(req, asyncResult.cause());
        } else {
          req.response()
              .putHeader("content-type", "application/json")
              .end(new JsonArray(asyncResult.result()).encode());
        }
      });
    });

    router.post("/service").handler(req -> {
      final JsonObject jsonBody = req.getBodyAsJson();

      final Service service =
          new Service(0L, jsonBody.getString("name"), jsonBody.getString("url"),
              "UNKNOWN", 0L);

      serviceRepository.save(service).setHandler(asyncResult -> {
        if (asyncResult.failed()) {
          handleError(req, asyncResult.cause());
        } else {
          final Optional<Long> id = asyncResult.result();
          if (id.isPresent()) {
            final Service createdService =
                new Service(id.get(), service.getName(), service.getUrl(), service.getStatus(),
                    service.getCreatedAt());
            req.response()
                .putHeader("content-type", "application/json")
                .putHeader("location", "/service/" + id.get())
                .end(JsonObject.mapFrom(createdService).encode());
          } else {
            handleError(req, new RuntimeException("ID for the created service not found"));
          }
        }
      });
    });

    router.get("/service/:id").handler(req -> {
      final Long id = Long.parseLong(req.pathParam("id"));

      serviceRepository.get(id).setHandler(asyncResult -> {
        if (asyncResult.failed()) {
          handleError(req, asyncResult.cause());
        } else {
          final Optional<Service> service = asyncResult.result();
          if (service.isPresent()) {
            req.response()
                .putHeader("content-type", "application/json")
                .putHeader("location", "/service/" + id)
                .end(JsonObject.mapFrom(service.get()).encode());
          } else {
            req.response()
                .putHeader("content-type", "application/json")
                .setStatusCode(404)
                .end(new JsonObject()
                    .put("message", "Service not found")
                    .encode()
                );
          }
        }
      });
    });

    router.put("/service/:id").handler(req -> {
      final Long id = Long.parseLong(req.pathParam("id"));
      final JsonObject data = req.getBodyAsJson();

      serviceRepository.update(id, data.getString("url"), data.getString("name"))
          .setHandler(asyncUpdateResult -> {
            if (asyncUpdateResult.failed()) {
              handleError(req, asyncUpdateResult.cause());
            } else {
              if (asyncUpdateResult.result()) {
                req.response()
                    .putHeader("content-type", "application/json")
                    .putHeader("location", "/service/" + id)
                    .end(new JsonObject()
                        .put("success", true)
                        .encode());
              } else {
                req.response()
                    .putHeader("content-type", "application/json")
                    .setStatusCode(404)
                    .end(new JsonObject()
                        .put("message", "Service not found")
                        .encode()
                    );
              }
            }
          });
    });

    router.delete("/service/:id").handler(req -> {
      final Long id = Long.parseLong(req.pathParam("id"));
      serviceRepository.delete(id).setHandler(asyncResult -> {
        if (asyncResult.failed()) {
          handleError(req, asyncResult.cause());
        } else {
          req.response()
              .putHeader("content-type", "application/json")
              .putHeader("location", "/service")
              .end();
        }
      });
    });
  }

  private SockJSHandler eventBusHandler() {
    final BridgeOptions options = new BridgeOptions()
        .addOutboundPermitted(new PermittedOptions().setAddress(POLLER_UPDATES_TOPIC));

    return SockJSHandler.create(vertx).bridge(options, event -> {
      if (event.type() == BridgeEventType.SOCKET_CREATED) {
        LOGGER.info("A socket was created");
      }
      event.complete(true);
    });
  }

  private void handleError(final RoutingContext req, final Throwable cause) {
    req.response()
        .putHeader("content-type", "application/json")
        .setStatusCode(500)
        .end(new JsonObject()
            .put("message", cause.getMessage())
            .encode()
        );
  }
}



