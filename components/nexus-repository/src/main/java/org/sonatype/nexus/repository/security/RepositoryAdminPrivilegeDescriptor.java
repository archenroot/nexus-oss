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

package org.sonatype.nexus.repository.security;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CPrivilegeBuilder;
import org.sonatype.security.realms.privileges.PrivilegeDescriptor;
import org.sonatype.security.realms.privileges.PrivilegeDescriptorSupport;

import org.apache.shiro.authz.Permission;

/**
 * Repository admin {@link PrivilegeDescriptor}.
 *
 * @see RepositoryAdminPermission
 * @since 3.0
 */
@Named(RepositoryAdminPrivilegeDescriptor.TYPE)
@Singleton
public class RepositoryAdminPrivilegeDescriptor
    extends PrivilegeDescriptorSupport
{
  public static final String TYPE = RepositoryAdminPermission.DOMAIN;

  public static final String P_FORMAT = "format";

  public static final String P_REPOSITORY = "repository";

  public static final String P_ACTIONS = "actions";

  public RepositoryAdminPrivilegeDescriptor() {
    super(TYPE);
  }

  @Override
  public Permission createPermission(final CPrivilege privilege) {
    assert privilege != null;
    String format = readProperty(privilege, P_FORMAT, ALL);
    String name = readProperty(privilege, P_REPOSITORY, ALL);
    List<String> actions = readListProperty(privilege, P_ACTIONS, ALL);
    return new RepositoryAdminPermission(format, name, actions);
  }

  //
  // Helpers
  //

  // FIXME: Update actions to collection

  public static String id(final String format, final String name, final String actions) {
    return String.format("%s-%s-%s-%s", TYPE, format, name, actions);
  }

  public static CPrivilege privilege(final String format, final String name, final String actions) {
    return new CPrivilegeBuilder()
        .type(TYPE)
        .id(id(format, name, actions))
        .property(P_FORMAT, format)
        .property(P_REPOSITORY, name)
        .property(P_ACTIONS, actions)
        .create();
  }
}