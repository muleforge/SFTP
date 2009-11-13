/*
 * $Id: SftpSendReceiveFunctionalTestCase.java 96 2009-10-31 20:55:41Z elhoo $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.transport.sftp;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.dataintegrity.AbstractSftpDataIntegrityTestCase;

import java.util.HashMap;

/**
 * Simple test to verify that the filter configuration works. Note that the transport uses an "early"
 * filter to improves the performance.
 */
public class SftpFilterTestCase extends AbstractSftpDataIntegrityTestCase
{
	private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";
	private static String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";

	protected String getConfigResources()
	{
		return "mule-sftp-filter-config.xml";
	}

	@Override
	protected void doSetUp() throws Exception
	{
		super.doSetUp();

		initEndpointDirectory(INBOUND_ENDPOINT_NAME);
		initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
	}


	public void testFilter() throws Exception
	{
		MuleClient muleClient = new MuleClient();

		// Send .xml file
		HashMap<String, String> xmlProps = new HashMap<String, String>(1);
		xmlProps.put(SftpConnector.PROPERTY_FILENAME, "file.xml");
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME), TEST_MESSAGE, xmlProps);

		// Send .txt file
		HashMap<String, String> txtProps = new HashMap<String, String>(1);
		txtProps.put(SftpConnector.PROPERTY_FILENAME, "file.txt");
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME), TEST_MESSAGE, txtProps);

		// TODO dont know any better way to wait for the above to finish? We cant use TestComponent here since we dont want all the files?
		Thread.sleep(4000);

		SftpClient outboundSftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);
		ImmutableEndpoint outboundEndpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_ENDPOINT_NAME);

		SftpClient inboundSftpClient = getSftpClient(muleClient, INBOUND_ENDPOINT_NAME);
		ImmutableEndpoint inboundEndpoint = (ImmutableEndpoint) muleClient.getProperty(INBOUND_ENDPOINT_NAME);

		assertFalse("The xml file should not be left in the inbound directory",
			super.verifyFileExists(inboundSftpClient, inboundEndpoint.getEndpointURI().getPath(), "file.xml"));
		assertTrue("The xml file should be in the outbound directory",
			super.verifyFileExists(outboundSftpClient, outboundEndpoint.getEndpointURI().getPath(), "file.xml"));

		assertTrue("The txt file should be left in the inbound directory",
			super.verifyFileExists(inboundSftpClient, inboundEndpoint.getEndpointURI().getPath(), "file.txt"));
		assertFalse("The txt file should not be in the outbound directory",
			super.verifyFileExists(outboundSftpClient, outboundEndpoint.getEndpointURI().getPath(), "file.txt"));
	}
}