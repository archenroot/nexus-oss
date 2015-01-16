package org.sonatype.nexus.repository.raw.internal.proxy;

import java.io.IOException;
import java.net.URI;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.util.NestedAttributesMap;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.0
 */
public class ProxyFacetImpl
    extends FacetSupport
    implements ProxyFacet
{
  public static final String CONFIG_KEY = "proxy";

  private URI remoteUrl;

  private HttpClientFacet httpClient;

  private PayloadStorage localStorage;

  @Override
  protected void doInit() throws Exception {
    NestedAttributesMap attributes = getRepository().getConfiguration().attributes(CONFIG_KEY);
    String url = attributes.require("remoteUrl", String.class);
    if (!url.endsWith("/")) {
      url = url + "/";
    }
    this.remoteUrl = new URI(url);
    log.debug("Remote URL: {}", remoteUrl);
  }

  @Override
  protected void doStart() throws Exception {
    httpClient = getRepository().facet(HttpClientFacet.class);
    localStorage = getRepository().facet(PayloadStorage.class);
  }

  @Override
  protected void doStop() throws Exception {
    httpClient = null;
    localStorage = null;
  }

  @Override
  protected void doDestroy() throws Exception {
    remoteUrl = null;
  }

  @Override
  public Payload get(final Locator locator) {
    checkNotNull(locator);

    Payload content = localStorage.get(locator);

    if (content == null) {
      try {
        content = fetch(locator);
        if (content != null) {
          localStorage.put(locator, content);
        }
      }
      catch (IOException e) {
        log.warn("Failed to fetch: {}", locator, e);
      }
    }
    return content;
  }

  @Nullable
  private Payload fetch(final Locator locator) throws IOException {
    HttpClient client = httpClient.getHttpClient();

    HttpGet request = new HttpGet(remoteUrl.resolve(locator.uri()));
    log.debug("Fetching: {}", request);

    HttpResponse response = client.execute(request);
    log.debug("Response: {}", response);

    StatusLine status = response.getStatusLine();
    log.debug("Status: {}", status);

    Payload payload = null;
    if (status.getStatusCode() == HttpStatus.SC_OK) {
      HttpEntity entity = response.getEntity();
      try {
        log.debug("Entity: {}", entity);
        payload = new HttpEntityPayload(entity);
      }
      finally {
        EntityUtils.consume(entity);
      }
    }

    return payload;
  }
}