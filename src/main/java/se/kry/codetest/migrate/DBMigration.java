package se.kry.codetest.migrate;

import io.vertx.core.Vertx;
import se.kry.codetest.DBConnector;

public class DBMigration {

  public static void main(String[] args) {
    final Vertx vertx = Vertx.vertx();
    final DBConnector connector = new DBConnector(vertx);

    final String createTableQuery =
        "CREATE TABLE IF NOT EXISTS service (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "url VARCHAR(128) NOT NULL, " +
        "name VARCHAR(128) NOT NULL, " +
        "status VARCHAR(128) NOT NULL, " +
        "created_at TIMESTAMP DEFAULT (strftime('%s', 'now')) NOT NULL" +
        ")";

    connector.query(
        createTableQuery).setHandler(done -> {
      if (done.succeeded()) {
        System.out.println("completed db migrations");
      } else {
        done.cause().printStackTrace();
      }
      vertx.close(shutdown -> {
        System.exit(0);
      });
    });
  }
}
