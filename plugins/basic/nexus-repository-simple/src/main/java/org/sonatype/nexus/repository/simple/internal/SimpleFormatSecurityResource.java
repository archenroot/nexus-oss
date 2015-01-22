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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.security.CRoleBuilder;
import org.sonatype.nexus.repository.security.MutableDynamicSecurityResource;
import org.sonatype.nexus.repository.security.RepositoryFormatPrivilegeDescriptor;
import org.sonatype.security.model.SecurityModelConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.security.BreadActions.ADD;
import static org.sonatype.nexus.repository.security.BreadActions.BROWSE;
import static org.sonatype.nexus.repository.security.BreadActions.DELETE;
import static org.sonatype.nexus.repository.security.BreadActions.EDIT;
import static org.sonatype.nexus.repository.security.BreadActions.READ;
import static org.sonatype.nexus.repository.security.RepositoryFormatPrivilegeDescriptor.id;
import static org.sonatype.nexus.repository.security.RepositoryFormatPrivilegeDescriptor.privilege;

/**
 * Simple format security resource.
 *
 * @since 3.0
 */
@Named
@Singleton
public class SimpleFormatSecurityResource
    extends MutableDynamicSecurityResource
{
  private final Format format;

  @Inject
  public SimpleFormatSecurityResource(final @Named(SimpleFormat.NAME) Format format) {
    this.format = checkNotNull(format);

    // apply initial configuration
    apply(new Mutator()
    {
      @Override
      public void apply(final SecurityModelConfiguration model) {
        initial(model);
      }
    });
  }

  private void initial(final SecurityModelConfiguration model) {
    String formatName = format.getValue();

    // add repository-format privileges
    model.addPrivilege(privilege(formatName, BROWSE));
    model.addPrivilege(privilege(formatName, READ));
    model.addPrivilege(privilege(formatName, EDIT));
    model.addPrivilege(privilege(formatName, ADD));
    model.addPrivilege(privilege(formatName, DELETE));

    // add repository-format 'admin' role
    model.addRole(new CRoleBuilder()
        .id(String.format("%s-%s-admin", RepositoryFormatPrivilegeDescriptor.TYPE, formatName))
        .privilege(id(formatName, BROWSE))
        .privilege(id(formatName, READ))
        .privilege(id(formatName, EDIT))
        .privilege(id(formatName, ADD))
        .privilege(id(formatName, DELETE))
        .create());

    // add repository-format 'readonly' role
    model.addRole(new CRoleBuilder()
        .id(String.format("%s-%s-readonly", RepositoryFormatPrivilegeDescriptor.TYPE, formatName))
        .privilege(id(formatName, BROWSE))
        .privilege(id(formatName, READ))
        .create());

    // add repository-format 'deployer' role
    model.addRole(new CRoleBuilder()
        .id(String.format("%s-%s-deployer", RepositoryFormatPrivilegeDescriptor.TYPE, formatName))
        .privilege(id(formatName, BROWSE))
        .privilege(id(formatName, READ))
        .privilege(id(formatName, EDIT))
        .privilege(id(formatName, ADD))
        .create());
  }
}
