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
package org.sonatype.security.rest.users;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.rest.model.PlexusUserResource;
import org.sonatype.security.rest.model.PlexusUserResourceResponse;
import org.sonatype.security.usermanagement.NoSuchUserManagerException;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * REST resource for retrieving a users by source. The sources is typically the security realm the user belongs too.
 *
 * @author bdemers
 */
@Singleton
@Typed(PlexusResource.class)
@Named("UserBySourcePlexusResource")
@Produces({"application/xml", "application/json"})
@Consumes({"application/xml", "application/json"})
@Path(UserBySourcePlexusResource.RESOURCE_URI)
public class UserBySourcePlexusResource
    extends AbstractSecurityPlexusResource
{
  public static final String USER_ID_KEY = "userId";

  public static final String USER_SOURCE_KEY = "userSource";

  public static final String RESOURCE_URI = "/plexus_user/{" + USER_SOURCE_KEY + "}/{" + USER_ID_KEY + "}";

  public UserBySourcePlexusResource() {
    setModifiable(false);
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/plexus_user/*/*", "authcBasic,perms[security:users]");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  /**
   * Retrieves user information.
   *
   * @param sourceId The Id of the source. A source specifies where the users/roles came from, for example the source
   *                 Id of 'LDAP' identifies the users/roles as coming from an LDAP source.
   * @param userId   The Id of the user.
   */
  @Override
  @GET
  public PlexusUserResourceResponse get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    PlexusUserResourceResponse result = new PlexusUserResourceResponse();

    User user;
    try {
      // TODO: remove the "all" we either need to move it down into the SecuritySystem, or remove it, i vote
      // remove it.
      String source = getUserSource(request);

      if ("all".equalsIgnoreCase(source)) {
        user = this.getSecuritySystem().getUser(getUserId(request));
      }
      else {
        user = this.getSecuritySystem().getUser(getUserId(request), source);
      }

    }
    catch (UserNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }
    catch (NoSuchUserManagerException e) {
      this.getLogger().warn(e.getMessage(), e);
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }

    if (user == null) {

    }

    PlexusUserResource resource = securityToRestModel(user);

    result.setData(resource);

    return result;
  }

  protected String getUserId(Request request) {
    return getRequestAttribute(request, USER_ID_KEY);
  }

  protected String getUserSource(Request request) {
    return getRequestAttribute(request, USER_SOURCE_KEY);
  }
}
