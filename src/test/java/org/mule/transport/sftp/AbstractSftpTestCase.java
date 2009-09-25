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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.mule.api.MuleEventContext;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.StringMessageUtils;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;


/**
 *  @author Lennart Häggkvist
  * Date: Jun 8, 2009
 */
public abstract class AbstractSftpTestCase extends FunctionalTestCase
{

	/**
	 * Deletes all files in the directory, useful when testing to ensure that no files are in the way...
	 */
	protected void cleanupRemoteFtpDirectory(MuleClient muleClient, String endpointName) throws IOException
	{
		SftpClient sftpClient = getSftpClient(muleClient, endpointName);

		EndpointURI endpointURI = getUriByEndpointName(muleClient, endpointName);
		sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(endpointURI.getPath()));

		String[] files = sftpClient.listFiles();
		for (String file : files)
		{
			sftpClient.deleteFile(file);
		}
	}

	/**
	 * Deletes the <i>directoryName</i> under the endpoint path
	 */
	protected void deleteRemoteDirectory(MuleClient muleClient, String endpointName, String directoryName) throws IOException
	{
		SftpClient sftpClient = getSftpClient(muleClient, endpointName);

		EndpointURI endpointURI = getUriByEndpointName(muleClient, endpointName);

		// The deleteDirectory operation will fail if there are files in the directory
		try
		{
			sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(endpointURI.getPath() + "/" + directoryName));
			String[] files = sftpClient.listFiles();
			for (String file : files)
			{
				sftpClient.deleteFile(file);
			}
		} catch(Exception e)
		{
			// Ignore, this will occur if the directory didnt exist!
		}

		try
		{
			sftpClient.deleteDirectory(endpointURI.getPath() + "/" + directoryName);
		} catch(IOException e) {
			e.printStackTrace();
		}

		try
		{
			sftpClient.changeWorkingDirectory(endpointURI.getPath() + "/" + directoryName);
			fail("The directory should have been deleted");
		} catch (IOException e)
		{
			// Expected
		}
	}

	/**
	 * Creates the <i>directoryName</i> under the endpoint path
	 */
	protected void createRemoteDirectory(MuleClient muleClient, String endpointName, String directoryName) throws IOException
	{
		SftpClient sftpClient = getSftpClient(muleClient, endpointName);

		EndpointURI endpointURI = getUriByEndpointName(muleClient, endpointName);
		sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(endpointURI.getPath()));

		try
		{
			sftpClient.mkdir(directoryName);
		} catch(IOException e) {
			e.printStackTrace();
			// Expected if the directory didnt exist
		}

		try
		{
			sftpClient.changeWorkingDirectory(endpointURI.getPath() + "/" + directoryName);
		} catch (IOException e)
		{
			fail("The directory should have been created");
		}
	}

	protected EndpointURI getUriByEndpointName(MuleClient muleClient, String endpointName) throws IOException
	{
		ImmutableEndpoint endpoint = getImmutableEndpoint(muleClient, endpointName);
		return endpoint.getEndpointURI();
	}

	/**
	 *
	 * @param muleClient
	 * @param endpointName
	 * @return the endpoint adress in the form 'sftp://user@host/path'
	 */
	protected String getAddressByEndpoint(MuleClient muleClient, String endpointName) {
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(endpointName);
		EndpointURI endpointURI = endpoint.getEndpointURI();

		return "sftp://" + endpointURI.getUser() + "@" + endpointURI.getHost() + endpointURI.getPath();
	}

	/**
	 * Returns a SftpClient that is logged in to the sftp server that the endpoint is configured against.
	 *
	 * @param muleClient
	 * @param endpointName
	 * @return
	 * @throws IOException
	 */
	protected SftpClient getSftpClient(MuleClient muleClient, String endpointName)
			throws IOException
	{
		SftpClient sftpClient = new SftpClient();
		ImmutableEndpoint endpoint = getImmutableEndpoint(muleClient, endpointName);

		EndpointURI endpointURI = endpoint.getEndpointURI();
		SftpConnector sftpConnector = (SftpConnector) endpoint.getConnector();

		sftpClient.connect(endpointURI.getHost());
		if(sftpConnector.getIdentityFile() != null)
		{
			assertTrue("Login failed", sftpClient.login(endpointURI.getUser(), sftpConnector.getIdentityFile(), sftpConnector.getPassphrase()));
		} else
		{
			assertTrue("Login failed", sftpClient.login(endpointURI.getUser(), endpointURI.getPassword()));
		}
		return sftpClient;
	}

	/**
	 * Checks if the file exists on the server
	 */
	protected boolean verifyFileExists(SftpClient sftpClient, EndpointURI endpointURI, String file) throws IOException
	{
		return verifyFileExists(sftpClient, endpointURI.getPath(), file);
	}

	protected boolean verifyFileExists(SftpClient sftpClient, String path, String file) throws IOException
	{
		sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(path));
		String[] files = sftpClient.listFiles();

		for (String remoteFile : files)
		{
			if(file.equals(remoteFile)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Base method for executing tests...
	 *
	 */
	protected void executeBaseTest(String inputEndpointName, String sendUrl, String filename, final int size, String receivingTestComponentName, long timeout) throws Exception
	{
		MuleClient client = new MuleClient();

		// Do some cleaning so that the endpoint doesn't have any other files
		cleanupRemoteFtpDirectory(client, inputEndpointName);

		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger loopCount = new AtomicInteger(0);
		final AtomicInteger totalReceivedSize = new AtomicInteger(0);

		// Random byte that we want to send a lot of
		final int testByte = 42;

		EventCallback callback = new EventCallback()
		{
			public void eventReceived(MuleEventContext context, Object component)
				throws Exception
			{
				logger.info("called " + loopCount.incrementAndGet() + " times");
				FunctionalTestComponent ftc = (FunctionalTestComponent) component;

					InputStream sftpInputStream = (InputStream) ftc.getLastReceivedMessage();
					BufferedInputStream bif = new BufferedInputStream(sftpInputStream);
					byte[] buffer = new byte[1024 * 4];

					try {
						int n;
						while (-1 != (n = bif.read(buffer)))
						{
							totalReceivedSize.addAndGet(n);
	
							// Simple check to verify the data...
							for (byte b : buffer)
							{
								if (b != testByte)
								{
									fail("Incorrect received byte (was '" + b + "', excepected '" + testByte + "'");
								}
							}
						}
					} finally {
						bif.close();
					}
						

					latch.countDown();
			}
		};

		getFunctionalTestComponent(receivingTestComponentName).setEventCallback(callback);

		// InputStream that generates the data without using a file
		InputStream os = new InputStream()
		{
			int totSize = 0;

			public int read() throws IOException
			{
				totSize++;
				if (totSize <= size)
				{
					return testByte;
				} else
				{
					return -1;
				}
			}
		};

		HashMap<String, String> props = new HashMap<String, String>(1);
		props.put(SftpConnector.PROPERTY_FILENAME, filename);

		logger.info(StringMessageUtils.getBoilerPlate("Note! If this test fails due to timeout please add '-Dmule.test.timeoutSecs=XX' to the mvn command!"));

		executeBaseAssertionsBeforeCall();

		// Send the content using stream
		client.send(sendUrl, os, props);

		latch.await(timeout, TimeUnit.MILLISECONDS);

		executeBaseAssertionsAfterCall(size, totalReceivedSize.intValue());
	}

	/**
	 * To be overridden by the test-classes if required
	 */
	protected void executeBaseAssertionsBeforeCall() {
	}	

	/**
	 * To be overridden by the test-classes if required
	 */
	protected void executeBaseAssertionsAfterCall(int sendSize, int receivedSize) {

		// Make sure that the file we received had the same size as the one we sent
		logger.info("Sent size: " + sendSize);
		logger.info("Received size: " + receivedSize);

		assertEquals("The received file should have the same size as the sent file", sendSize, receivedSize);
	}
	
	private ImmutableEndpoint getImmutableEndpoint(MuleClient muleClient,
			String endpointName) throws IOException {
		ImmutableEndpoint endpoint = null;

		Object o = muleClient.getProperty(endpointName);
		if (o instanceof ImmutableEndpoint) {
			// For Inbound and Outbound Endpoints
			endpoint = (ImmutableEndpoint)o;

		} else if (o instanceof EndpointBuilder) {
			// For Endpoint-references
			EndpointBuilder eb = (EndpointBuilder)o;
			try {
				endpoint = eb.buildInboundEndpoint();
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
		}
		return endpoint;
	}
}