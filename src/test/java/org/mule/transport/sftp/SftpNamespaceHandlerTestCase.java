/*
 * $Id: NamespaceHandlerTestCase.vm 10787 2008-02-12 18:51:50Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.tck.FunctionalTestCase;

/**
 * TODO
 */
public class SftpNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "sftp-namespace-config.xml";
    }

    public void testSftpConfig() throws Exception
    {
        SftpConnector c = (SftpConnector) muleContext.getRegistry().lookupConnector("sftpConnector");
        assertNotNull(c);
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
        assertTrue(c.isAutoDelete());
        assertEquals(c.getPollingFrequency(),15000);
        //TODO Assert specific properties are configured correctly


    }
}
