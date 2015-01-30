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
package org.sonatype.nexus.testsuite.security.nexus233;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.security.targets.TargetPrivilegeDescriptor;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeDescriptor;
import org.sonatype.security.rest.model.PrivilegeProperty;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * CRUD tests for XML request/response.
 */
public class Nexus233PrivilegesCrudXMLIT
    extends AbstractNexusIntegrationTest
{

  protected PrivilegesMessageUtil messageUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    this.messageUtil = new PrivilegesMessageUtil(this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createReadMethodTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("read");
    resource.setMethod(methods);
    resource.setName("createReadMethodTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    // get the Resource object
    List<PrivilegeStatusResource> statusResources = this.messageUtil.createPrivileges(resource);

    Assert.assertTrue(statusResources.size() == 1);

    // make sure the id != null
    Assert.assertNotNull(statusResources.get(0).getId());

    Assert.assertEquals("read", getPrivilegeProperty(statusResources.get(0),
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("createReadMethodTest - (read)", statusResources.get(0).getName()); // ' - (read)' is
    // automatically added
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, statusResources.get(0).getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(statusResources.get(0),
        TargetPrivilegeDescriptor.P_TARGET_ID));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createCreateMethodTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("create");
    resource.setMethod(methods);
    resource.setName("createCreateMethodTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    // get the Resource object
    List<PrivilegeStatusResource> statusResources = this.messageUtil.createPrivileges(resource);

    Assert.assertTrue(statusResources.size() == 1);

    // make sure the id != null
    Assert.assertNotNull(statusResources.get(0).getId());

    Assert.assertEquals("create,read", getPrivilegeProperty(statusResources.get(0),
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("createCreateMethodTest - (create)", statusResources.get(0).getName()); // ' - (read)'
    // is
    // automatically added
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, statusResources.get(0).getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(statusResources.get(0),
        TargetPrivilegeDescriptor.P_TARGET_ID));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createUpdateMethodTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("update");
    resource.setMethod(methods);
    resource.setName("createUpdateMethodTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    // get the Resource object
    List<PrivilegeStatusResource> statusResources = this.messageUtil.createPrivileges(resource);

    Assert.assertTrue(statusResources.size() == 1);

    // make sure the id != null
    Assert.assertNotNull(statusResources.get(0).getId());

    Assert.assertEquals("update,read", getPrivilegeProperty(statusResources.get(0),
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("createUpdateMethodTest - (update)", statusResources.get(0).getName()); // ' - (read)'
    // is
    // automatically added
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, statusResources.get(0).getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(statusResources.get(0),
        TargetPrivilegeDescriptor.P_TARGET_ID));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createDeleteMethodTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("delete");
    resource.setMethod(methods);
    resource.setName("createDeleteMethodTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    // get the Resource object
    List<PrivilegeStatusResource> statusResources = this.messageUtil.createPrivileges(resource);

    Assert.assertTrue(statusResources.size() == 1);

    // make sure the id != null
    Assert.assertNotNull(statusResources.get(0).getId());

    Assert.assertEquals("delete,read", getPrivilegeProperty(statusResources.get(0),
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("createDeleteMethodTest - (delete)", statusResources.get(0).getName()); // ' - (read)'
    // is
    // automatically added
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, statusResources.get(0).getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(statusResources.get(0),
        TargetPrivilegeDescriptor.P_TARGET_ID));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createAllMethodTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("create");
    methods.add("read");
    methods.add("update");
    methods.add("delete");
    resource.setMethod(methods);
    resource.setName("createAllMethodTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    // get the Resource object
    List<PrivilegeStatusResource> statusResources = this.messageUtil.createPrivileges(resource);

    Assert.assertTrue(statusResources.size() == 4);

    PrivilegeStatusResource createPriv = this.getPrivilegeByMethod("create,read", statusResources);
    Assert.assertNotNull(createPriv.getId());
    Assert.assertEquals("create,read", getPrivilegeProperty(createPriv,
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("createAllMethodTest - (create)", createPriv.getName());
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, createPriv.getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(createPriv,
        TargetPrivilegeDescriptor.P_TARGET_ID));

    PrivilegeStatusResource readPriv = this.getPrivilegeByMethod("read", statusResources);
    Assert.assertNotNull(readPriv.getId());
    Assert.assertEquals("read", getPrivilegeProperty(readPriv,
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("createAllMethodTest - (read)", readPriv.getName());
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, readPriv.getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(readPriv,
        TargetPrivilegeDescriptor.P_TARGET_ID));

    PrivilegeStatusResource updatePriv = this.getPrivilegeByMethod("update,read", statusResources);
    Assert.assertNotNull(updatePriv.getId());
    Assert.assertEquals("update,read", getPrivilegeProperty(updatePriv,
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("createAllMethodTest - (update)", updatePriv.getName());
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, updatePriv.getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(updatePriv,
        TargetPrivilegeDescriptor.P_TARGET_ID));

    PrivilegeStatusResource deletePriv = this.getPrivilegeByMethod("delete,read", statusResources);
    Assert.assertNotNull(deletePriv.getId());
    Assert.assertEquals("delete,read", getPrivilegeProperty(deletePriv,
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("createAllMethodTest - (delete)", deletePriv.getName());
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, deletePriv.getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(deletePriv,
        TargetPrivilegeDescriptor.P_TARGET_ID));
  }

  private PrivilegeStatusResource getPrivilegeByMethod(String method, List<PrivilegeStatusResource> statusResources) {
    for (Iterator<PrivilegeStatusResource> iter = statusResources.iterator(); iter.hasNext(); ) {
      PrivilegeStatusResource privilegeBaseStatusResource = iter.next();

      if (getPrivilegeProperty(privilegeBaseStatusResource,
          ApplicationPrivilegeDescriptor.P_METHOD).equals(method)) {
        return privilegeBaseStatusResource;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void readTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("read");
    resource.setMethod(methods);
    resource.setName("readTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    // get the Resource object
    List<PrivilegeStatusResource> statusResources = this.messageUtil.createPrivileges(resource);

    Assert.assertTrue(statusResources.size() == 1);

    // make sure the id != null
    Assert.assertNotNull(statusResources.get(0).getId());

    String readPrivId = statusResources.get(0).getId();

    Assert.assertEquals("read", getPrivilegeProperty(statusResources.get(0),
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("readTest - (read)", statusResources.get(0).getName()); // ' - (read)' is automatically
    // added
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, statusResources.get(0).getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(statusResources.get(0),
        TargetPrivilegeDescriptor.P_TARGET_ID));

    Response response = this.messageUtil.sendMessage(Method.POST, resource, readPrivId);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create privilege: " + response.getStatus());
    }

    statusResources = this.messageUtil.getResourceListFromResponse(response);

    Assert.assertTrue(statusResources.size() == 1);

    // make sure the id != null
    Assert.assertNotNull(statusResources.get(0).getId());

    Assert.assertEquals("read", getPrivilegeProperty(statusResources.get(0),
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("readTest - (read)", statusResources.get(0).getName()); // ' - (read)' is automatically
    // added
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, statusResources.get(0).getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(statusResources.get(0),
        TargetPrivilegeDescriptor.P_TARGET_ID));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void updateTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("read");
    resource.setMethod(methods);
    resource.setName("updateTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    // get the Resource object
    List<PrivilegeStatusResource> statusResources = this.messageUtil.createPrivileges(resource);

    Assert.assertTrue(statusResources.size() == 1);

    // make sure the id != null
    Assert.assertNotNull(statusResources.get(0).getId());

    String readPrivId = statusResources.get(0).getId();

    Assert.assertEquals("read", getPrivilegeProperty(statusResources.get(0),
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("updateTest - (read)", statusResources.get(0).getName()); // ' - (read)' is
    // automatically
    // added
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, statusResources.get(0).getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(statusResources.get(0),
        TargetPrivilegeDescriptor.P_TARGET_ID));

    Response response = this.messageUtil.sendMessage(Method.PUT, resource, readPrivId);

    if (response.getStatus().getCode() != 405) // Method Not Allowed
    {
      Assert.fail("Update should have returned a 405: " + response.getStatus());
    }

  }

  @SuppressWarnings("unchecked")
  @Test
  public void deleteTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("read");
    resource.setMethod(methods);
    resource.setName("deleteTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    // get the Resource object
    List<PrivilegeStatusResource> statusResources = this.messageUtil.createPrivileges(resource);

    Assert.assertTrue(statusResources.size() == 1);

    // make sure the id != null
    Assert.assertNotNull(statusResources.get(0).getId());

    String readPrivId = statusResources.get(0).getId();

    Assert.assertEquals("read", getPrivilegeProperty(statusResources.get(0),
        ApplicationPrivilegeDescriptor.P_METHOD));
    Assert.assertEquals("deleteTest - (read)", statusResources.get(0).getName()); // ' - (read)' is
    // automatically
    // added
    Assert.assertEquals(TargetPrivilegeDescriptor.TYPE, statusResources.get(0).getType());
    Assert.assertEquals("testTarget", getPrivilegeProperty(statusResources.get(0),
        TargetPrivilegeDescriptor.P_TARGET_ID));

    Response response = this.messageUtil.sendMessage(Method.DELETE, resource, readPrivId);

    if (!response.getStatus().isSuccess()) // Method Not Allowed
    {
      Assert.fail("Delete failed: " + response.getStatus());
    }

    assertThat(messageUtil.findPrivilege(readPrivId), is(nullValue()));
  }

  public String getPrivilegeProperty(PrivilegeStatusResource priv, String key) {
    for (PrivilegeProperty prop : priv.getProperties()) {
      if (prop.getKey().equals(key)) {
        return prop.getValue();
      }
    }
    return null;
  }

}
