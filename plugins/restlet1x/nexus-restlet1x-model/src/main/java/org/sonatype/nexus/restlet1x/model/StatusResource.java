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
// =================== DO NOT EDIT THIS FILE ====================
// Generated by Modello 1.7,
// any modifications will be overwritten.
// ==============================================================

package org.sonatype.nexus.restlet1x.model;

/**
 * Nexus status details.
 *
 * @version $Revision$ $Date$
 */
@SuppressWarnings("all")
@javax.xml.bind.annotation.XmlType(name = "status-resource")
@javax.xml.bind.annotation.XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.FIELD)
public class StatusResource
    implements java.io.Serializable
{
  private String appName;

  private String version;

  private String editionShort;

  public String getAppName() {
    return appName;
  }

  public void setAppName(final String appName) {
    this.appName = appName;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getEditionShort() {
    return editionShort;
  }

  public void setEditionShort(final String editionShort) {
    this.editionShort = editionShort;
  }
}
