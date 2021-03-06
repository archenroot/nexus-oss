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
package org.sonatype.nexus.repository.simple.internal;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.httpbridge.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static org.sonatype.nexus.repository.httpbridge.HttpMethods.GET;

/**
 * Simple content group handler.
 *
 * @since 3.0
 */
@Named
@Singleton
public class SimpleGroupHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(final @Nonnull Context context) throws Exception {
    String name = ContextHelper.contentName(context);
    String method = context.getRequest().getAction();
    Repository repository = context.getRepository();
    log.debug("{} repository '{}' content-name: {}", method, repository.getName(), name);

    SimpleGroupFacet group = repository.facet(SimpleGroupFacet.class);

    switch (method) {
      case GET: {
        for (Repository member : group.members()) {
          log.trace("Trying member: {}", member);
          ViewFacet view = member.facet(ViewFacet.class);
          Response response = view.dispatch(context.getRequest());
          if (response.getStatus().getCode() == 200) {
            return response;
          }
        }
        return HttpResponses.notFound(name);
      }

      default:
        return HttpResponses.methodNotAllowed(method, GET);
    }
  }
}
