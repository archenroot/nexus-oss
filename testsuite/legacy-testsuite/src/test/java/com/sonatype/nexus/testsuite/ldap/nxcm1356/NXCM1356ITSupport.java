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
package com.sonatype.nexus.testsuite.ldap.nxcm1356;

import com.sonatype.nexus.testsuite.ldap.AbstractLdapIT;

import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.test.utils.SettingsMessageUtil;

import org.junit.Before;

import static java.util.Arrays.asList;

public class NXCM1356ITSupport
    extends AbstractLdapIT
{

  @Before
  public void activateRealms() throws Exception {
    GlobalConfigurationResource settings = SettingsMessageUtil.getCurrentSettings();
    settings.setSecurityRealms(asList("LdapRealm", "NexusAuthenticatingRealm", "NexusAuthorizingRealm"));
    SettingsMessageUtil.save(settings);
  }

}
