package de.braintags.vertx.test;

import java.util.List;
import java.util.function.Consumer;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.braintags.io.vertx.util.ResultObject;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ExecuteRequests {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(ExecuteRequests.class);
  // private String HOSTNAME = "localhost";
  // private String HOSTNAME = "192.168.41.69";
  private String HOSTNAME = "78.138.125.69";
  // private String HOSTNAME = "serverscope.de";
  private int PORT = 8081;

  protected static Vertx vertx;
  protected static HttpClient client;
  private static final int LOOP = 1;

  @Test
  public void testComplexFormDefect(TestContext context) throws Exception {
    String fileName = "src/main/resources/requests/formRequestComplexDefect.txt";
    String boundaryId = "104460758110407996421465278279";
    for (int i = 0; i < LOOP; i++) {
      LOGGER.info("run " + i);
      execute(context, fileName, boundaryId);
    }
  }

  @BeforeClass
  public static void startup(TestContext context) throws Exception {
    LOGGER.debug("starting class");
    vertx = Vertx.vertx(new VertxOptions());
    client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
  }

  public void execute(TestContext context, String fileName, String boundaryId) throws Exception {
    try {
      Buffer buffer = loadFile(fileName);
      String url = "/testtemplate/testUploadInsertResult.html";
      testRequest(context, HttpMethod.POST, url, httpConn -> {
        LOGGER.info("content-length is: " + buffer.length());
        httpConn.headers().set("content-length", String.valueOf(buffer.length()));
        httpConn.headers().set("Content-Type",
            "multipart/form-data; boundary=---------------------------" + boundaryId);
        httpConn.headers().set("User-Agent", "CodeJava Agent");

        httpConn.write(buffer);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        LOGGER.info("HEADERS: " + resp.headers);
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  private Buffer loadFile(String path) {
    LOGGER.info(vertx.fileSystem().fsPropsBlocking(".").toString());
    return vertx.fileSystem().readFileBlocking(path);
  }

  protected final void testRequest(TestContext context, HttpMethod method, String path,
      Consumer<HttpClientRequest> requestAction, Consumer<ResponseCopy> responseAction, int statusCode,
      String statusMessage, String responseBody) throws Exception {
    testRequestBuffer(context, method, path, requestAction, responseAction, statusCode, statusMessage,
        responseBody != null ? Buffer.buffer(responseBody) : null);
  }

  protected final void testRequestBuffer(TestContext context, HttpMethod method, String path,
      Consumer<HttpClientRequest> requestAction, Consumer<ResponseCopy> responseAction, int statusCode,
      String statusMessage, Buffer responseBodyBuffer) throws Exception {
    testRequestBuffer(context, client, method, PORT, path, requestAction, responseAction, statusCode, statusMessage,
        responseBodyBuffer);
  }

  protected final void testRequestBuffer(TestContext context, HttpClient client, HttpMethod method, int port,
      String path, Consumer<HttpClientRequest> requestAction, Consumer<ResponseCopy> responseAction, int statusCode,
      String statusMessage, Buffer responseBodyBuffer) throws Exception {
    Async async = context.async();
    ResultObject<ResponseCopy> resultObject = new ResultObject<>(null);

    Handler<Throwable> exceptionHandler = new Handler<Throwable>() {

      @Override
      public void handle(Throwable ex) {
        LOGGER.error("", ex);
        async.complete();
      }
    };

    HttpClientRequest req = client.request(method, port, HOSTNAME, path, resp -> {
      resp.exceptionHandler(exceptionHandler);

      ResponseCopy rc = new ResponseCopy();
      resp.bodyHandler(buff -> {
        rc.content = buff.toString();
        rc.code = resp.statusCode();
        rc.statusMessage = resp.statusMessage();
        rc.headers = MultiMap.caseInsensitiveMultiMap();
        rc.headers.addAll(resp.headers());
        rc.cookies = resp.cookies();
        resultObject.setResult(rc);
        async.complete();
      });
    });
    req.exceptionHandler(exceptionHandler);
    if (requestAction != null) {
      requestAction.accept(req);
    }
    req.end();
    async.await();

    ResponseCopy rc = resultObject.getResult();
    if (responseAction != null) {
      responseAction.accept(rc);
    }
    context.assertEquals(statusCode, rc.code);
    context.assertEquals(statusMessage, rc.statusMessage);
    if (responseBodyBuffer == null) {
      // async.complete();
    } else {
      context.assertEquals(responseBodyBuffer.toString(), rc.content);
    }
  }

  public static void main(String[] args) {

  }

  public class ResponseCopy {
    public String content;
    public int code;
    public String statusMessage;
    public MultiMap headers;
    public List<String> cookies;
  }

}
