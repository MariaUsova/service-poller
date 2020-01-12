package se.kry.codetest;

import java.util.Objects;

public class Service {

  private final Long id;

  private final String name;

  private final String url;

  private final String status;

  private final Long createdAt;

  public Service(final Long id, final String name, final String url, final String status,
                 final Long createdAt) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(url);
    Objects.requireNonNull(status);
    Objects.requireNonNull(createdAt);

    this.id = id;
    this.name = name;
    this.url = url;
    this.status = status;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public String getStatus() {
    return status;
  }

  public Long getCreatedAt() {
    return createdAt;
  }
}
