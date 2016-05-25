package io.vertx.core.http.impl;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.VertxInternal;

public class DebugHttpServerImpl extends HttpServerImpl {
  public DebugHttpServerImpl(VertxInternal vertx, HttpServerOptions options) {
    super(vertx, options);
  }

}
