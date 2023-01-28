package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpInternalEntityStorage;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;

public final class ApacheHttpClientInernalEntityStorage extends OtelHttpInternalEntityStorage<HttpRequest, HttpResponse> {
  private static final ApacheHttpClientInernalEntityStorage INSTANCE;

  static {
    INSTANCE = new ApacheHttpClientInernalEntityStorage();
  }

  private ApacheHttpClientInernalEntityStorage() {
    super(
        VirtualField.find(Context.class, HttpRequest.class),
        VirtualField.find(Context.class, HttpResponse.class)
    );
  }

  public static OtelHttpInternalEntityStorage<HttpRequest, HttpResponse> storage() {
    return INSTANCE;
  }
}
