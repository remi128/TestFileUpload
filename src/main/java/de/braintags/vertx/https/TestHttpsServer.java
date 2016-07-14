package de.braintags.vertx.https;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;

/**
 * Reproducer for https://github.com/vert-x3/vertx-web/issues/385
 * 
 * @author mremme
 * 
 */

public class TestHttpsServer extends AbstractVerticle {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TestHttpsServer.class);
  private Router router;
  public static final int PORT = 8090;
  private static final String CERT_PATH = "autobob.pem";
  private static final String CERT_KEY_PATH = "autobobKey2.pem";

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.AbstractVerticle#init(io.vertx.core.Vertx, io.vertx.core.Context)
   */
  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.AbstractVerticle#start(io.vertx.core.Future)
   */
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    try {
      router = Router.router(vertx);
      initHttpsServer(router, result -> {
        if (result.failed()) {
          startFuture.fail(result.cause());
        } else {
          addRouter(router);
          startFuture.complete();
        }
      });
    } catch (Exception e) {
      startFuture.fail(e);
    }
  }

  private void initHttpsServer(Router router, Handler<AsyncResult<Void>> handler) {
    HttpServerOptions options = new HttpServerOptions().setPort(PORT);
    options.setSsl(true);
    try {
      options.setPemKeyCertOptions(new PemKeyCertOptions().setCertPath(CERT_PATH).setKeyPath(CERT_KEY_PATH));
      HttpServer server = vertx.createHttpServer(options);
      server.requestHandler(router::accept).listen(result -> {
        if (result.failed()) {
          handler.handle(Future.failedFuture(result.cause()));
        } else {
          handler.handle(Future.succeededFuture());
        }
      });
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }

  private void addRouter(Router router) {
    router.route().handler(BodyHandler.create());
    router.route().handler(CookieHandler.create());
    router.route().handler(context -> {
      context.response().end("all fine");
    });
  }

}
