package de.braintags.vertx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
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
 * Autobob<br>
 * <br>
 * Copyright: Copyright (c) 26.11.2015 <br>
 * Company: Braintags GmbH <br>
 * 
 * @author mremme
 * 
 */

public class TestFileUploadVerticle extends AbstractVerticle {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TestFileUploadVerticle.class);
  public static final String MOVE_MESSAGE = "moved uploaded file from %s to %s";
  private Router router;
  private String uploadDirectory = "webroot/upload/";
  private String uploadRelativePath = "upload/";
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
          Map<String, String> params = extractProperties(entity, context);
          handleFileUploads(entity, context, params);
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

  private void handleFileUploads(String entityName, RoutingContext context, Map<String, String> params) {
    String startKey = entityName.toLowerCase() + ".";
    Set<FileUpload> fileUploads = context.fileUploads();
    if (fileUploads.size() == 0) {
      throw new IllegalArgumentException("No uploads found");
    }
    FileSystem fs = getVertx().fileSystem();
    LOGGER.info("Number of fileuploads: " + fileUploads.size());

    for (FileUpload upload : fileUploads) {
      if (isHandleUpload(upload, startKey)) {
        String fieldName = upload.name().toLowerCase();
        LOGGER.info("uploaded file detected for field name " + fieldName + ", fileName: " + upload.fileName());
        String relativePath = handleOneFile(fs, upload);
        String pureKey = fieldName.substring(startKey.length());
        params.put(pureKey, relativePath);
      }
    }
  }

  private String handleOneFile(FileSystem fs, FileUpload upload) {
    String uploadedFile = upload.uploadedFileName();
    String[] newDestination = examineNewDestination(fs, upload);
    fs.moveBlocking(uploadedFile, newDestination[0]);

    LOGGER.info(String.format(MOVE_MESSAGE, uploadedFile, newDestination[0]));
    return newDestination[1];
  }

  private boolean isHandleUpload(FileUpload upload, String startKey) {
    LOGGER.info(
        "CHECKING: " + upload.uploadedFileName() + " | fileName: " + upload.fileName() + " | name: " + upload.name());
    String fieldName = upload.name().toLowerCase();
    if (upload.size() <= 0) {
      throw new IllegalArgumentException("In this scenario upload size must be > 0 )");
    }
    if (!fieldName.startsWith(startKey)) {
      LOGGER.info("NOT HANDLED: fieldname does not start with:" + startKey + " | " + fieldName);
      return false;
    }
    return true;
  }

  private String[] examineNewDestination(FileSystem fs, FileUpload upload) {
    if (upload.fileName() == null || upload.fileName().hashCode() == 0) {
      throw new IllegalArgumentException("The upload contains no filename");
    }
    String[] destinations = new String[2];
    String upDir = uploadDirectory;
    if (!fs.existsBlocking(upDir)) {
      fs.mkdirsBlocking(upDir);
    }
    String relDir = uploadRelativePath;
    String fileName = createUniqueName(fs, upDir, upload.fileName());
    destinations[0] = upDir + (upDir.endsWith("/") ? "" : "/") + fileName;
    destinations[1] = relDir + (relDir.endsWith("/") ? "" : "/") + fileName;
    return destinations;
  }

  private String createUniqueName(FileSystem fs, String upDir, String fileName) {
    fileName = fileName.replaceAll(" ", "_");
    String newFileName = fileName;
    int counter = 0;
    String path = upDir + (upDir.endsWith("/") ? "" : "/") + newFileName;
    while (fs.existsBlocking(path)) {
      LOGGER.info("file exists already: " + path);
      if (fileName.indexOf('.') > 0) {
        newFileName = fileName.replaceFirst("\\.", counter++ + ".");
      } else {
        newFileName = fileName + counter++;
      }
      path = upDir + (upDir.endsWith("/") ? "" : "/") + newFileName;
    }
    return newFileName;
  }

  /**
   * Extract the properties from the request, where the name starts with the entity name, which shall be handled by the
   * current request
   * 
   * @param entityName
   *          the name, like it was specified by the parameter {@link PersistenceController#MAPPER_CAPTURE_KEY}
   * @param captureMap
   *          the resolved capture parameters for the current request
   * @param context
   *          the {@link RoutingContext} of the request
   * @param mapper
   *          the IMapper for the current request
   * @return the key / values of the request, where the key starts with "entityName.". The key is reduced to the pure
   *         name
   */
  protected Map<String, String> extractProperties(String entityName, RoutingContext context) {
    String startKey = entityName.toLowerCase() + ".";
    Map<String, String> map = new HashMap<>();
    extractPropertiesFromMap(startKey, map, context.request().formAttributes());
    extractPropertiesFromMap(startKey, map, context.request().params());
    return map;
  }

  /**
   * @param startKey
   * @param map
   * @param attrs
   */
  private void extractPropertiesFromMap(String startKey, Map<String, String> map, MultiMap attrs) {
    Iterator<Entry<String, String>> it = attrs.iterator();
    while (it.hasNext()) {
      Entry<String, String> entry = it.next();
      String key = entry.getKey().toLowerCase();
      if (key.startsWith(startKey)) {
        String pureKey = key.substring(startKey.length());
        String value = entry.getValue();
        map.put(pureKey, value);
      }
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
