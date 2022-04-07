/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */


import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.instrumentation.test.AgentTestTrait
import io.opentelemetry.instrumentation.test.base.HttpClientTest
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.util.Timeout
import spock.lang.AutoCleanup
import spock.lang.Shared

import java.util.concurrent.CancellationException

abstract class ApacheHttpAsyncClientTest extends HttpClientTest<SimpleHttpRequest> implements AgentTestTrait {

  @Shared
  RequestConfig requestConfig = RequestConfig.custom()
    .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
    .build()

  @Shared
  RequestConfig requestWithReadTimeoutConfig = RequestConfig.copy(requestConfig)
    .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT_MS))
    .build()

  @AutoCleanup
  @Shared
  def client = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build()

  @AutoCleanup
  @Shared
  def clientWithReadTimeout = HttpAsyncClients.custom().setDefaultRequestConfig(requestWithReadTimeoutConfig).build()

  def setupSpec() {
    client.start()
    clientWithReadTimeout.start()
  }

  CloseableHttpAsyncClient getClient(URI uri) {
    if (uri.toString().contains("/read-timeout")) {
      return clientWithReadTimeout
    }
    return client
  }

  @Override
  String userAgent() {
    return "httpasyncclient"
  }

  @Override
  Integer responseCodeOnRedirectError() {
    return 302
  }

  @Override
  boolean testReadTimeout() {
    true
  }

  @Override
  SimpleHttpRequest buildRequest(String method, URI uri, Map<String, String> headers) {
    def request = createRequest(method, uri)
    request.addHeader("user-agent", userAgent())
    headers.entrySet().each {
      request.setHeader(new BasicHeader(it.key, it.value))
    }
    return request
  }

  @Override
  Set<AttributeKey<?>> httpAttributes(URI uri) {
    Set<AttributeKey<?>> extra = [
      SemanticAttributes.HTTP_SCHEME,
      SemanticAttributes.HTTP_TARGET
    ]
    super.httpAttributes(uri) + extra
  }

  // compilation fails with @Override annotation on this method (groovy quirk?)
  int sendRequest(SimpleHttpRequest request, String method, URI uri, Map<String, String> headers) {
    def response = executeRequest(request, uri)
    response.entity?.content?.close() // Make sure the connection is closed.
    return response.statusLine.statusCode
  }

  // compilation fails with @Override annotation on this method (groovy quirk?)
  void sendRequestWithCallback(SimpleHttpRequest request, String method, URI uri, Map<String, String> headers, AbstractHttpClientTest.RequestResult requestResult) {
    try {
      executeRequestWithCallback(request, uri, new FutureCallback<SimpleHttpResponse>() {
        @Override
        void completed(SimpleHttpResponse httpResponse) {
          httpResponse.entity?.content?.close() // Make sure the connection is closed.
          requestResult.complete(httpResponse.statusLine.statusCode)
        }

        @Override
        void failed(Exception e) {
          requestResult.complete(e)
        }

        @Override
        void cancelled() {
          requestResult.complete(new CancellationException())
        }
      })
    } catch (Throwable throwable) {
      requestResult.complete(throwable)
    }
  }

  abstract SimpleHttpRequest createRequest(String method, URI uri)

  abstract SimpleHttpResponse executeRequest(SimpleHttpRequest request, URI uri)

  abstract void executeRequestWithCallback(SimpleHttpRequest request, URI uri, FutureCallback<SimpleHttpResponse> callback)

  static String fullPathFromURI(URI uri) {
    StringBuilder builder = new StringBuilder()
    if (uri.getPath() != null) {
      builder.append(uri.getPath())
    }

    if (uri.getQuery() != null) {
      builder.append('?')
      builder.append(uri.getQuery())
    }

    if (uri.getFragment() != null) {
      builder.append('#')
      builder.append(uri.getFragment())
    }
    return builder.toString()
  }
}

class ApacheClientUriRequest extends ApacheHttpAsyncClientTest {
  @Override
  SimpleHttpRequest createRequest(String method, URI uri) {
    return new SimpleHttpRequest(method, uri)
  }

  @Override
  SimpleHttpResponse executeRequest(SimpleHttpRequest request, URI uri) {
    return getClient(uri).execute(request, null).get()
  }

  @Override
  void executeRequestWithCallback(SimpleHttpRequest request, URI uri, FutureCallback<SimpleHttpResponse> callback) {
    getClient(uri).execute(request, callback)
  }
}

class ApacheClientHostRequest extends ApacheHttpAsyncClientTest {
  @Override
  SimpleHttpRequest createRequest(String method, URI uri) {
    // also testing with absolute path below
    return new SimpleHttpRequest(method, new URI(fullPathFromURI(uri)))
  }

  @Override
  SimpleHttpResponse executeRequest(SimpleHttpRequest request, URI uri) {
    return getClient(uri).execute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), request, null).get()
  }

  @Override
  void executeRequestWithCallback(SimpleHttpRequest request, URI uri, FutureCallback<SimpleHttpResponse> callback) {
    getClient(uri).execute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), request, callback)
  }
}

class ApacheClientHostAbsoluteUriRequest extends ApacheHttpAsyncClientTest {

  @Override
  SimpleHttpRequest createRequest(String method, URI uri) {
    return new SimpleHttpRequest(method, new URI(uri.toString()))
  }

  @Override
  SimpleHttpResponse executeRequest(SimpleHttpRequest request, URI uri) {
    return getClient(uri).execute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), request, null).get()
  }

  @Override
  void executeRequestWithCallback(SimpleHttpRequest request, URI uri, FutureCallback<SimpleHttpResponse> callback) {
    getClient(uri).execute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), request, callback)
  }
}
