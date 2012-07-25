/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.bundle.launcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.sonatype.sisu.goodies.marshal.internal.jackson.JacksonMarshaller;
import org.sonatype.sisu.litmus.testsupport.TestUtil;
import com.google.common.base.Throwables;

/**
 * Utility class for loading Nexus integration tests parameters.
 *
 * @since 2.2
 */
public abstract class ParametersLoader
{

    /**
     * Load test parameters form specified file. The file should contain a serialized Object[][] in json format.
     *
     * @param parametersFile file containing parameters
     * @return test parameters
     */
    public static Collection<Object[]> loadTestParameters( final File parametersFile )
    {
        try
        {
            final Object[][] parametersSets = new JacksonMarshaller().unmarshal(
                readFileToString( checkNotNull( parametersFile ) ), Object[][].class
            );
            if ( parametersSets == null )
            {
                return null;
            }
            return Arrays.asList( parametersSets );
        }
        catch ( final Exception e )
        {
            throw Throwables.propagate( e );
        }
    }

    /**
     * Load test parameters from "target/test-classes/parameters.json".
     *
     * @return test parameters
     * @see {@link #loadTestParameters(java.io.File)}
     */
    public static Collection<Object[]> loadDefaultTestParameters()
    {
        return loadTestParameters(
            new TestUtil( ParametrizedNexusRunningITSupport.class ).resolveFile(
                TestUtil.TARGET + "/test-classes/parameters.json"
            )
        );
    }

}
