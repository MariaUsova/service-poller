package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BackgroundPoller {

  private final WebClient webClient;

  public BackgroundPoller(final WebClient webClient) {
    this.webClient = webClient;
  }

  public Future<Map<Long, String>> pollServices(final List<Service> services) {
    final Future<Map<Long, String>> future = Future.future();

    final Map<Long, String> results = new HashMap<>();
    final int totalRequests = services.size();
    final AtomicInteger completedRequests = new AtomicInteger(0);

    for (final Service service : services) {
      final URI uri = URI.create(service.getUrl());
      final int port = uri.getPort() > 0 ? uri.getPort() : 80;
      webClient.get(port, uri.getHost(), uri.getPath())
          .timeout(5000)
          .expect(ResponsePredicate.status(200, 202))
          .send(result -> {
            final String status = result.succeeded() ? "OK" : "FAIL";
            results.put(service.getId(), status);

            final int requests = completedRequests.incrementAndGet();
            if (requests == totalRequests) {
              future.complete(results);
            }
          });
    }

    return future;
  }
}
