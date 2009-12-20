/*
 * $Id: SftpLargeReceiveFunctionalTestCase.java 60 2008-04-24 22:42:00Z quoc $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.transport.sftp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
 
/**
 * @author Lennart Häggkvist, Magnus Larsson
 * <code>SftpTempDirFunctionalTestCase</code> tests the tempDir functionality.
 */

public class SftpTempDirFunctionalTestCase extends AbstractSftpTestCase
{
	protected static final long TIMEOUT = 10000;
	private static final String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";
	private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
	private static final String TEMP_DIR = "uploading";

	protected String getConfigResources()
	{
		return "mule-sftp-temp-dir-config.xml";
	}

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
        initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
    }

	public void testTempDir() throws Exception
	{
		MuleClient muleClient = new MuleClient();

		DispatchParameters p = new DispatchParameters(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME);
		p.setSftpConnector("sftpCustomConnector");
		dispatchAndWaitForDelivery(p);

        SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);
        try
		{
			EndpointURI endpointURI = getUriByEndpointName(muleClient, OUTBOUND_ENDPOINT_NAME);

			sftpClient.changeWorkingDirectory(endpointURI.getPath() + "/" + TEMP_DIR);
		} catch (IOException f)
		{
			fail("The temp directory should have been created");
		} finally {
            sftpClient.disconnect();
        }

        try {
            sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);
            ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_ENDPOINT_NAME);
            assertTrue("The file should exist in the final destination", super.verifyFileExists(sftpClient, endpoint.getEndpointURI(), FILE_NAME));
            assertFalse("No file should exist in the temp directory", super.verifyFileExists(sftpClient, endpoint.getEndpointURI().getPath() + "/uploading", FILE_NAME));
        } finally {
            sftpClient.disconnect();
        }
    }

}
