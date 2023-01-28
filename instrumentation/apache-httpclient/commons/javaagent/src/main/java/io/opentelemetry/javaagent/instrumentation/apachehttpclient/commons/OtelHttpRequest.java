package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import java.net.InetSocketAddress;
import java.util.List;

public interface OtelHttpRequest {
  BytesTransferMetrics getBytesTransferMetrics();

  String getPeerName();

  Integer getPeerPort();

  InetSocketAddress getPeerSocketAddress();

  String getMethod();

  String getUrl();

  String getFlavor();

  List<String> getHeader(String name);

  String getFirstHeader(String name);

  void setHeader(String key, String value);
}
