/*
 * $Id: SftpSendReceiveFunctionalTestCase.java 77 2009-07-17 08:13:22Z elhoo $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.transport.sftp;

import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.api.MuleEventContext;
import org.mule.util.StringMessageUtils;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Test sending and receiving a very large message.
 * <p/>
 * This test will probably fail due to the standard timeout. According to http://www.mulesource.org/display/MULE2USER/Functional+Testing
 * the only way to change the timeout is "add -Dmule.test.timeoutSecs=XX either to the mvn command you use to run Mule or to the JUnit
 * test runner in your IDE."
 *
 * Tested with '-Dmule.test.timeoutSecs=300'
 */
public class SftpSendReceiveLargeFileFunctionalTestCase extends AbstractSftpTestCase
{
	private static final long TIMEOUT = 30000;

	// Size of the genereated stream - 200 Mb
	final static int SEND_SIZE = 1024 * 1024 * 200;

	// Uses the same config as SftpSendReceiveFunctionalTestCase
	protected String getConfigResources()
	{
		return "mule-send-receive-large-file-test-config.xml";
	}

	/**
	 * Test sending and receiving a large file.
	 *
	 */
	public void testSendAndReceiveLargeFile() throws Exception
	{
		MuleClient client = new MuleClient();

		// Do some cleaning so that the endpoint doesnt have any other files
		super.cleanupRemoteFtpDirectory(client, "inboundEndpoint");

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

					SftpInputStream sftpInputStream = (SftpInputStream) ftc.getLastReceivedMessage();
					BufferedInputStream bif = new BufferedInputStream(sftpInputStream);
					byte[] buffer = new byte[1024 * 4];

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

					latch.countDown();
			}
		};

		getFunctionalTestComponent("receiving").setEventCallback(callback);

		// InputStream that generates the data without using a file
		InputStream os = new InputStream()
		{
			int totSize = 0;

			public int read() throws IOException
			{
				totSize++;
				if (totSize <= SEND_SIZE)
				{
					return testByte;
				} else
				{
					return -1;
				}
			}
		};

		HashMap<String, String> props = new HashMap<String, String>(1);
		props.put(SftpConnector.PROPERTY_FILENAME, "bigfile.txt");

		logger.info(StringMessageUtils.getBoilerPlate("Note! If this test fails due to timeout please add '-Dmule.test.timeoutSecs=XX' to the mvn command!"));

		// Send the content using stream
		client.send("vm://test.upload", os, props);

		latch.await(TIMEOUT * 10, TimeUnit.MILLISECONDS);

		// Make sure that the file we received had the same size as the one we sent
		logger.info("Sent size: " + SEND_SIZE);
		logger.info("Received size: " + totalReceivedSize.intValue());

		assertEquals("The received file should have the same size as the sent file", SEND_SIZE, totalReceivedSize.intValue());
	}
}
