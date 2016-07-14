package de.braintags.vertx.https;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestHttpClient {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TestHttpClient.class);
  public static String HOSTNAME = "localhost";

  static Vertx vertx = Vertx.vertx(new VertxOptions());

  @Test
  public void test(TestContext context) {
    Async async = context.async();
    HttpClientOptions options = new HttpClientOptions();
    options.setSsl(true);
    options.setDefaultPort(TestHttpsServer.PORT);
    options.setTrustAll(true);
    options.setVerifyHost(false);
    options.setKeepAlive(true);
    options.setTcpKeepAlive(true);

    Handler<Throwable> exceptionHandler = new Handler<Throwable>() {

      @Override
      public void handle(Throwable ex) {
        LOGGER.error("", ex);
        async.complete();
      }
    };

    HttpClient client = vertx.createHttpClient(options);

    HttpClientRequest req = client.request(HttpMethod.GET, TestHttpsServer.PORT, HOSTNAME, "/", resp -> {
      resp.exceptionHandler(exceptionHandler);

      resp.bodyHandler(buff -> {
        LOGGER.info(buff.toString());
        LOGGER.info(resp.statusCode());
        LOGGER.info(resp.statusMessage());
        LOGGER.info(resp.headers());
        LOGGER.info(resp.cookies());
        async.complete();
      });

    });
    req.exceptionHandler(exceptionHandler);
    req.end();

    async.await();
    LOGGER.info("finished");
  }

}
