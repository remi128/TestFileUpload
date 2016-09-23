package de.braintags.vertx.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TestAsync {

  @Test
  public void testAsync(TestContext context) {
    Async async = context.async();
    handle(context, res -> {
      async.complete();
    });
    async.await();
    System.out.println("ready");
  }

  public void handle(TestContext context, Handler<AsyncResult<Void>> handler) {
    Async async = context.async();
    doLoop(500, res -> {
      async.complete();
    });
    async.await();
  }

  public void doLoop(int loop, Handler<AsyncResult<Void>> handler) {
    for (int i = 0; i < loop; i++) {
      System.out.println(i);
    }
    handler.handle(Future.succeededFuture());
  }
}
