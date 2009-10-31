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

import org.mule.module.client.MuleClient;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

/**
 * @author Lennart Häggkvist
 * <code>SftpFileAgeFunctionalTestCase</code> tests the fileAge functionality.
 */

public class SftpTempDirFunctionalTestCase extends AbstractSftpTestCase
{
	protected static final long TIMEOUT = 10000;
	private static final String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";
	private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
	private static final String TEMP_DIR = "uploading";

	private static final String fileName = "file.txt";
	// Map that is used to set the filename in the outbound directory
	private static final Map<String, String> fileNameProperties = new HashMap<String, String>();
	static
	{
		fileNameProperties.put("filename", fileName);
	}

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

		// Ensure that no other files exists
//		cleanupRemoteFtpDirectory(muleClient, OUTBOUND_ENDPOINT_NAME);
//		cleanupRemoteFtpDirectory(muleClient, INBOUND_ENDPOINT_NAME);

		// Delete the temp directory so that we can ensure that it is created
//		deleteRemoteDirectory(muleClient, OUTBOUND_ENDPOINT_NAME, TEMP_DIR);

		// Send an file to the SFTP server, which the inbound-endpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME) + "?connector=sftpCustomConnector", TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComp
		Thread.sleep(10000);

		try
		{
			SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);
			EndpointURI endpointURI = getUriByEndpointName(muleClient, OUTBOUND_ENDPOINT_NAME);

			sftpClient.changeWorkingDirectory(endpointURI.getPath() + "/" + TEMP_DIR);
		} catch (IOException f)
		{
			fail("The temp directory should have been created");
		}

		SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_ENDPOINT_NAME);
		assertTrue("The file should exist in the final destination", super.verifyFileExists(sftpClient, endpoint.getEndpointURI(), fileName));
		assertFalse("No file should exist in the temp directory", super.verifyFileExists(sftpClient, endpoint.getEndpointURI().getPath() + "/uploading", fileName));
	}

}
