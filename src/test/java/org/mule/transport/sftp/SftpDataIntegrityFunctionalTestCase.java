/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;

import org.mule.module.client.MuleClient;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.EndpointURI;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

/**
 * Tests that the source file (inbound) is not being deleted before the file
 * has been successfully written to the outbound directory.
 * <p/>
 * The tests assume that the following directories exists under the in & outbound endpoints:
 * <ul>
 * <li>dataintegrity-dirmissing-test-inbound
 * <li>dataintegrity-tempdir-test-outbound
 * <li>dataintegrity-test-outbound
 * <li>dataintegrity-tempdir-test-inbound
 * <li>dataintegrity-test-inbound
 * <li>dataintegrity-wrongpassprase-test-inbound
 * </ul>
 * <p/>
 * Tip: I used 'watch -n 1 find datai* -ls' on the linux server to watch what happens on the directories when I run these tests
 *
 * TODO MULE-4395: This test fails if the log is in DEBUG mode (ie. rootCategory=DEBUG). Probably due to MULE-4395.
 *   The problem can be recreated by adding 'log4j.logger.org.mule.routing.outbound=DEBUG' in log4j.properties
 *
 * TODO Fix something better than 'Thread.sleep(X);' - this sleep must probably be tuned to different computers (or setting it very high?)
 *       If a test fails in the class, test increase the sleep time....
 *
 *  * @author Lennart Häggkvist
 */
public class SftpDataIntegrityFunctionalTestCase extends AbstractSftpTestCase
{
	private static final String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";
	private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

	private static final String OUTBOUND_TEMPDIR_ENDPOINT_NAME = "outboundEndpointTempDir";
	private static final String INBOUND_TEMPDIR_ENDPOINT_NAME = "inboundEndpointTempDir";

	private static final String INBOUND_DIRMISSING_ENDPOINT_NAME = "inboundEndpointDirMissing";

	private static final String INBOUND_WRONGPASSPHRASE_ENDPOINT_NAME = "inboundEndpointWrongPasspraseOnOutbound";


	private static final String fileName = "file.txt";
	// Map that is used to set the filename in the outbound directory
	private static final Map<String, String> fileNameProperties = new HashMap<String, String>();
	private static final String TEMP_DIR = "uploading";

	static
	{
		fileNameProperties.put("filename", fileName);
	}

	protected String getConfigResources()
	{
		return "mule-sftp-data-integrity-config.xml";
	}

	/**
	 * setup method that should be run before all tests. The real setUp-method is final in the AbstractMuleTestCase
	 * class.
	 */
	public void setUpBeforeTest(MuleClient muleClient, String inboundEndpointName, String outboundEndpointName) throws IOException, SftpException
	{
		// RW - so that we can do initial cleanup
		remoteChmod(muleClient, outboundEndpointName, 00700);

		// Ensure that no other files exists
		cleanupRemoteFtpDirectory(muleClient, inboundEndpointName);
		cleanupRemoteFtpDirectory(muleClient, outboundEndpointName);
	}

	private void remoteChmod(MuleClient muleClient, String endpointName, int permissions) throws IOException, SftpException
	{
		SftpClient sftpClient = getSftpClient(muleClient, endpointName);
		ChannelSftp channelSftp = sftpClient.getChannelSftp();

		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(endpointName);
		EndpointURI endpointURI = endpoint.getEndpointURI();

		// RW - so that we can do initial cleanup
		channelSftp.chmod(permissions, sftpClient.getAbsolutePath(endpointURI.getPath()));
	}

	private void verifyInAndOutFiles(MuleClient muleClient, String inboundEndpointName, String outboundEndpointName,
									 boolean shouldInboundFileStillExist, boolean shouldOutboundFileExist) throws IOException
	{
		SftpClient sftpClientInbound = getSftpClient(muleClient, inboundEndpointName);
		ImmutableEndpoint inboundEndpoint = (ImmutableEndpoint) muleClient.getProperty(inboundEndpointName);

		SftpClient sftpClientOutbound = getSftpClient(muleClient, outboundEndpointName);
		ImmutableEndpoint outboundEndpoint = (ImmutableEndpoint) muleClient.getProperty(outboundEndpointName);

		if (shouldInboundFileStillExist)
		{
			assertTrue("The inbound file should still exist", super.verifyFileExists(sftpClientInbound,  inboundEndpoint.getEndpointURI(), fileName));
		} else
		{
			assertFalse("The inbound file should have been deleted", super.verifyFileExists(sftpClientInbound,  inboundEndpoint.getEndpointURI(), fileName));
		}

		if (shouldOutboundFileExist)
		{
			assertTrue("The outbound file should exist", super.verifyFileExists(sftpClientOutbound,  outboundEndpoint.getEndpointURI(), fileName));
		} else
		{
			assertFalse("The outbound file should have been deleted", super.verifyFileExists(sftpClientOutbound,  outboundEndpoint.getEndpointURI(), fileName));
		}
	}

	/**
	 * Happy path. The file is moved from inbound to outbound.
	 */
	public void testWriteAccessToOutboundDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();
		setUpBeforeTest(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME);

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME) + "?connector=sftpCustomConnector", TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(4000);

		verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, false, true);
	}


	/**
	 * No write access on the outbound directory. The source file should still exist
	 */
	public void testNoWriteAccessToOutboundDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();
		setUpBeforeTest(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME);

		// change the chmod to "dr-x------" on the outbound-directory
		remoteChmod(muleClient, OUTBOUND_ENDPOINT_NAME, 00500);

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME) + "?connector=sftpCustomConnector", TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(4000);

		verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, true, false);
	}

	/**
	 * No write access on the outbound directory and thus the TEMP directory cant be created.
	 * The source file should still exist
	 */
	public void testCantCreateTempDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();
		setUpBeforeTest(muleClient, INBOUND_TEMPDIR_ENDPOINT_NAME, OUTBOUND_TEMPDIR_ENDPOINT_NAME);

		// Delete the temp directory
		deleteRemoteDirectory(muleClient, OUTBOUND_TEMPDIR_ENDPOINT_NAME, TEMP_DIR);

		// change the chmod to "dr-x------" on the outbound-directory
		// --> the temp directory should not be able to be created
		remoteChmod(muleClient, OUTBOUND_TEMPDIR_ENDPOINT_NAME, 00500);

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_TEMPDIR_ENDPOINT_NAME) + "?connector=sftpCustomConnector", TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(2000);

		verifyInAndOutFiles(muleClient, INBOUND_TEMPDIR_ENDPOINT_NAME, OUTBOUND_TEMPDIR_ENDPOINT_NAME, true, false);
	}

	/**
	 * No write access on the outbound directory but write access to the TEMP directory.
	 * The source file should still exist and no file should exist in the TEMP directory.
	 */
	public void testCantWriteToFinalDestAfterTempDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();
		setUpBeforeTest(muleClient, INBOUND_TEMPDIR_ENDPOINT_NAME, OUTBOUND_TEMPDIR_ENDPOINT_NAME);

		// Delete and recreate the temp directory
		deleteRemoteDirectory(muleClient, OUTBOUND_TEMPDIR_ENDPOINT_NAME, TEMP_DIR);
		createRemoteDirectory(muleClient, OUTBOUND_TEMPDIR_ENDPOINT_NAME, TEMP_DIR);

		// change the chmod to "dr-x------" on the outbound-directory
		// --> the temp directory should not be able to be created
		remoteChmod(muleClient, OUTBOUND_TEMPDIR_ENDPOINT_NAME, 00500);

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_TEMPDIR_ENDPOINT_NAME) + "?connector=sftpCustomConnector", TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(4000);

		verifyInAndOutFiles(muleClient, INBOUND_TEMPDIR_ENDPOINT_NAME, OUTBOUND_TEMPDIR_ENDPOINT_NAME, true, false);

		SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_TEMPDIR_ENDPOINT_NAME);
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_TEMPDIR_ENDPOINT_NAME);
		assertFalse("The inbound file should not be left in the TEMP-dir", super.verifyFileExists(sftpClient, endpoint.getEndpointURI().getPath() + "/" + TEMP_DIR , fileName));
	}

	/**
	 * The outbound directory doesnt exist.
	 * The source file should still exist
	 */
	public void testNoOutboundDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();
		cleanupRemoteFtpDirectory(muleClient, INBOUND_DIRMISSING_ENDPOINT_NAME);

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_DIRMISSING_ENDPOINT_NAME) + "?connector=sftpCustomConnector", TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(2000);

		SftpClient sftpClient = getSftpClient(muleClient, INBOUND_DIRMISSING_ENDPOINT_NAME);
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(INBOUND_DIRMISSING_ENDPOINT_NAME);
		assertTrue("The inbound file should still exist", super.verifyFileExists(sftpClient, endpoint.getEndpointURI(), fileName));
	}

	/**
	 * The outbound directory doesnt exist.
	 * The source file should still exist
	 */
	public void testWrongPassphraseOnOutboundDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();
		cleanupRemoteFtpDirectory(muleClient, INBOUND_WRONGPASSPHRASE_ENDPOINT_NAME);

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_WRONGPASSPHRASE_ENDPOINT_NAME) + "?connector=sftpCustomConnector", TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(2000);

		SftpClient sftpClient = getSftpClient(muleClient, INBOUND_WRONGPASSPHRASE_ENDPOINT_NAME);
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(INBOUND_WRONGPASSPHRASE_ENDPOINT_NAME);
		assertTrue("The inbound file should still exist", super.verifyFileExists(sftpClient, endpoint.getEndpointURI(), fileName));
	}

}