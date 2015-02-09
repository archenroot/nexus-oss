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
package org.sonatype.nexus.testsuite.npm.smoke;

import java.io.File;

import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.testsuite.npm.NpmMockRegistryITSupport;

import com.bolyuba.nexus.plugin.npm.client.NpmProxyRepository;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * NPM IT for NPM plugin testing proxy cache deletion.
 */
public class NpmCacheDeleteIT
    extends NpmMockRegistryITSupport
{
  public NpmCacheDeleteIT(String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void cacheDelete() throws Exception {
    // create a NPM Proxy repository that proxies mock NPM registry
    NpmProxyRepository proxy = createNpmProxyRepository(testMethodName());

    final File irrelevant = util.createTempFile();
    final Location commonjs = Location.repositoryLocation(proxy.id(), "/commonjs");

    // fetch to populate the cache
    content().download(commonjs, irrelevant);

    // block proxy
    proxy.block().save();

    // delete it from cache
    content().delete(commonjs);

    try {
      content().download(commonjs, irrelevant);
      fail("Should be 404");
    }
    catch (NexusClientNotFoundException e) {
      // good
    }
  }
}