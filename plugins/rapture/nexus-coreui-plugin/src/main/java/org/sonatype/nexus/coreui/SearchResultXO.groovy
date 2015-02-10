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
package org.sonatype.nexus.coreui

import groovy.transform.ToString

/**
 * Search result exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
class SearchResultXO
{
  String id
  String groupId
  String artifactId
  String format

  boolean equals(final o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    SearchResultXO that = (SearchResultXO) o

    if (artifactId != that.artifactId) return false
    if (format != that.format) return false
    if (groupId != that.groupId) return false

    return true
  }

  int hashCode() {
    int result
    result = groupId.hashCode()
    result = 31 * result + artifactId.hashCode()
    result = 31 * result + format.hashCode()
    return result
  }

}
