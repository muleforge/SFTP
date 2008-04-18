/*
 * $Id: NamespaceHandlerTestCase.vm 10787 2008-02-12 18:51:50Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.$transportid;

import org.mule.tck.FunctionalTestCase;

/**
 * TODO
 */
public class $transportidNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        //TODO You'll need to edit this file to configure the properties specific to your transport
        return "$transportid-namespace-config.xml";
    }

    public void test$transportidConfig() throws Exception
    {
        $transportidConnector c = ($transportidConnector) muleContext.getRegistry().lookupConnector("$transportidConnector");
        assertNotNull(c);
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());

        //TODO Assert specific properties are configured correctly


    }
}
