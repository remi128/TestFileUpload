package de.braintags.vertx;

import java.util.Set;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.impl.DebugHttpServerImpl;
import io.vertx.core.impl.VertxInternal;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;

/**
 * Reproducer for https://github.com/vert-x3/vertx-web/issues/385
 * 
 * @author mremme
 * 
 */

public class TestFileUploadVerticle extends AbstractVerticle {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TestFileUploadVerticle.class);
  private Router router;
  public static final int PORT = 8081;

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
      initHttpServer(router, result -> {
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

  private void initHttpServer(Router router, Handler<AsyncResult<Void>> handler) {
    HttpServerOptions options = new HttpServerOptions().setPort(PORT);
    HttpServer server = new DebugHttpServerImpl((VertxInternal) vertx, options);

    server.requestHandler(router::accept).listen(result -> {
      if (result.failed()) {
        handler.handle(Future.failedFuture(result.cause()));
      } else {
        handler.handle(Future.succeededFuture());
      }
    });
  }

  private void addRouter(Router router) {
    router.route().handler(BodyHandler.create());
    router.route().handler(CookieHandler.create());
    router.route().handler(context -> {
      try {
        logRequest(context);
        String entity = context.request().getFormAttribute("entity");
        if (entity == null || entity.hashCode() == 0) {
          context.response().setStatusCode(400);
          context.response().end(getReply(context, "error"));
        } else {
          context.response().end(getReply(context, "OK"));
        }
      } catch (Exception e) {
        LOGGER.error("", e);
        context.fail(e);
      }
    });
  }

  private Buffer getReply(RoutingContext context, String title) {
    Buffer buffer = Buffer.buffer();
    buffer.appendString("<h3>").appendString(title).appendString("</h3>\n\n");
    MultiMap headers = context.request().headers();
    buffer.appendString("HEADERS: " + headers.size()).appendString("\n");
    headers.entries()
        .forEach(entry -> buffer.appendString("   " + entry.getKey() + ": " + entry.getValue()).appendString("\n"));

    MultiMap params = context.request().params();
    buffer.appendString("PARAMETER: " + params.size()).appendString("\n");
    params.entries()
        .forEach(entry -> buffer.appendString("   " + entry.getKey() + ": " + entry.getValue()).appendString("\n"));

    MultiMap formAttributes = context.request().formAttributes();
    buffer.appendString("FORM_ATTRIBUTES: " + formAttributes.size()).appendString("\n");
    formAttributes.entries()
        .forEach(entry -> buffer.appendString("   " + entry.getKey() + ": " + entry.getValue()).appendString("\n"));

    Set<FileUpload> fileUploads = context.fileUploads();
    buffer.appendString("FILE UPLOADS: " + fileUploads.size()).appendString("\n");
    fileUploads.forEach(fu -> buffer.appendString("   NAME: " + fu.name() + " | FILENAME: " + fu.fileName()
        + " | UPLOADED: " + fu.uploadedFileName() + " | SIZE: " + fu.size()).appendString("\n"));

    return buffer.appendString("USER: " + context.user());
  }

  private void logRequest(RoutingContext context) {

    LOGGER.info("LOGGING REQUEST FOR " + context.request().path());

    MultiMap headers = context.request().headers();
    LOGGER.info("HEADERS: " + headers.size());
    headers.entries().forEach(entry -> LOGGER.info("   " + entry.getKey() + ": " + entry.getValue()));

    MultiMap params = context.request().params();
    LOGGER.info("PARAMETER: " + params.size());
    params.entries().forEach(entry -> LOGGER.info("   " + entry.getKey() + ": " + entry.getValue()));

    MultiMap formAttributes = context.request().formAttributes();
    LOGGER.info("FORM_ATTRIBUTES: " + formAttributes.size());
    formAttributes.entries().forEach(entry -> LOGGER.info("   " + entry.getKey() + ": " + entry.getValue()));

    Set<FileUpload> fileUploads = context.fileUploads();
    LOGGER.info("FILE UPLOADS: " + fileUploads.size());
    fileUploads.forEach(fu -> LOGGER.info("   NAME: " + fu.name() + " | FILENAME: " + fu.fileName() + " | UPLOADED: "
        + fu.uploadedFileName() + " | SIZE: " + fu.size()));

    LOGGER.info("USER: " + context.user());
  }

}
