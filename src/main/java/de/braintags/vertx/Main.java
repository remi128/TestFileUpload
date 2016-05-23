package de.braintags.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;

/**
 * <br>
 * <br>
 * Copyright: Copyright (c) 14.12.2015 <br>
 * Company: Braintags GmbH <br>
 * 
 * @author mremme
 * 
 */

public class Main extends AbstractVerticle {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory.getLogger(Main.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    DeploymentOptions options = new DeploymentOptions();

    vertx.deployVerticle(TestFileUploadVerticle.class.getName(), options, result -> {
      if (result.failed()) {
        startFuture.fail(result.cause());
      } else {
        LOGGER.info(TestFileUploadVerticle.class.getSimpleName() + " successfully launched: " + result.result());
        startFuture.complete();
      }
    });
  }

}
