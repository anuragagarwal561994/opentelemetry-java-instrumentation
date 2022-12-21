/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import java.util.List;
import javax.annotation.Nullable;

final class ApacheHttpClientHttpAttributesGetter
    implements HttpClientAttributesGetter<ApacheHttpClientRequest, ApacheHttpClientResponse> {

  @Override
  public String getMethod(ApacheHttpClientRequest request) {
    return request.getMethod();
  }

  @Override
  public String getUrl(ApacheHttpClientRequest request) {
    return request.getUrl();
  }

  @Override
  public List<String> getRequestHeader(ApacheHttpClientRequest request, String name) {
    return request.getHeader(name);
  }

  @Override
  public Integer getStatusCode(ApacheHttpClientRequest request, ApacheHttpClientResponse response, @Nullable Throwable error) {
    return response.getStatusCode();
  }

  @Override
  @Nullable
  public String getFlavor(ApacheHttpClientRequest request, @Nullable ApacheHttpClientResponse response) {
    String flavor = request.getFlavor();
    if (flavor == null && response != null) {
      flavor = response.getFlavor();
    }
    return flavor;
  }

  @Override
  public List<String> getResponseHeader(ApacheHttpClientRequest request, ApacheHttpClientResponse response, String name) {
    return response.getHeader(name);
  }
}
