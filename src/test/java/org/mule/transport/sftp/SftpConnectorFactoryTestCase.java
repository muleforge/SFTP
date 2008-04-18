/*
 * $Id: ConnectorFactoryTestCase.vm 11571 2008-04-12 00:22:07Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.$transportid;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;


public class $transportidConnectorFactoryTestCase extends AbstractMuleTestCase
{

    /* For general guidelines on writing transports see
       http://mule.mulesource.org/display/MULE/Writing+Transports */

    public void testCreateFromFactory() throws Exception
    {
        InboundEndpoint endpoint = muleContext.getRegistry()
                .lookupEndpointFactory().getInboundEndpoint(getEndpointURI());
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertTrue(endpoint.getConnector() instanceof $transportidConnector);
        assertEquals(getEndpointURI(), endpoint.getEndpointURI().getAddress());
    }
    
    public String getEndpointURI() 
    {
        // TODO return a valid endpoint URI string for your transport
        // i.e. tcp://localhost:1234
        throw new UnsupportedOperationException("getEndpointURI");
    }

}
