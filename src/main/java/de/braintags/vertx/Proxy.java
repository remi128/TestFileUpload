package de.braintags.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;

public class Proxy extends AbstractVerticle {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory.getLogger(Proxy.class);

  int PORT = 8082;

  @Override
  public void start() throws Exception {
    HttpClient client = vertx.createHttpClient(new HttpClientOptions());
    vertx.createHttpServer().requestHandler(req -> {
      LOGGER.info("Proxying request: " + req.uri());
      HttpClientRequest c_req = client.request(req.method(), TestFileUploadVerticle.PORT, "localhost", req.uri(),
          c_res -> {
            LOGGER.info("Proxying response: " + c_res.statusCode());
            req.response().setChunked(true);
            req.response().setStatusCode(c_res.statusCode());
            req.response().headers().setAll(c_res.headers());
            c_res.handler(data -> {
              LOGGER.info("Proxying response body: " + data.toString("ISO-8859-1"));
              req.response().write(data);
            });
            c_res.endHandler((v) -> req.response().end());
          });
      c_req.setChunked(true);
      c_req.headers().setAll(req.headers());
      req.handler(data -> {
        LOGGER.info("Proxying request body " + data.toString("ISO-8859-1"));
        c_req.write(data);
      });
      req.endHandler((v) -> c_req.end());
    }).listen(PORT);
  }
}
