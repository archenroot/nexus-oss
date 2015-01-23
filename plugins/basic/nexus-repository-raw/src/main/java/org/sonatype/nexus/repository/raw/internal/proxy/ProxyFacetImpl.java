/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.raw.internal.proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.util.NestedAttributesMap;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import com.google.common.io.ByteStreams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;

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

  private int artifactMaxAgeMinutes;

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

    artifactMaxAgeMinutes = attributes.require("artifactMaxAge", Integer.class);
    log.debug("Artifact max age: {}", artifactMaxAgeMinutes);
  }

  // TODO: implement doUpdate()
  // TODO: Trigger other changes based on the remoteUrl changing - perhaps it calls facet(NFC.class).clear() at this point?

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
  public Payload get(final Locator locator) throws IOException {
    checkNotNull(locator);

    Payload content = localStorage.get(locator);

    if (content == null || isStale(content)) {
      try {
        final Payload remote = fetch(locator);
        if (remote != null) {

          // TODO: Introduce content validation.. perhaps content's type not matching path's implied type.

          content = localStorage.put(locator, remote);
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
        final HttpEntityPayload httpEntityPayload = new HttpEntityPayload(response, entity);

        payload = readFully(httpEntityPayload);
      }
      finally {
        EntityUtils.consume(entity);
      }
    }

    return payload;
  }

  /**
   * Read an incoming Payload into memory.
   */
  private Payload readFully(final Payload payload) throws IOException {
    try (InputStream stream = payload.openInputStream()) {
      final byte[] bytes = ByteStreams.toByteArray(stream);
      return new StreamPayload(new ByteArrayInputStream(bytes), bytes.length, payload.getContentType(),
          payload.getLastModified());
    }
  }

  private boolean isStale(Payload payload) {
    if (artifactMaxAgeMinutes < 0) {
      log.trace("Artifact max age checking disabled.");
      return false;
    }

    final DateTime lastModified = payload.getLastModified();

    if (lastModified == null) {
      log.debug("Artifact last modified date unknown.");
      return true;
    }

    final DateTime earliestFreshDate = new DateTime().minusMinutes(artifactMaxAgeMinutes);
    return lastModified.isBefore(earliestFreshDate);
  }
}
