/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.transport.sftp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.mule.api.MuleEventContext;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>SendReceiveFunctionalTestCase</code> tests sending an receiving multiple
 * small text files.
 *
 *
 */

public class SftpSendReceiveFunctionalTestCase extends AbstractSftpTestCase
{

	private static final long TIMEOUT = 30000;

	private ArrayList<String> sendFiles;
	private ArrayList<String> receiveFiles;


    protected String getConfigResources()
    {
        return "mule-send-receive-test-config.xml";
    }


    public void testSendAndReceiveSingleFile() throws Exception
    {
    	sendFiles = new ArrayList<String>();

    	sendFiles.add("file created on " + new Date().getTime());

    	sendAndReceiveFiles();
    }



    //Test Mule-1477 (an old VFS Connector issue, but test anyway).
    public void testSendAndReceiveEmptyFile() throws Exception
    {
    	sendFiles = new ArrayList<String>();

    	sendFiles.add("");

    	sendAndReceiveFiles();
    }


    public void testSendAndReceiveMultipleFiles() throws Exception
    {
    	sendFiles = new ArrayList<String>();

    	sendFiles.add("file1");
    	sendFiles.add("file2");
    	sendFiles.add("file3");
    	sendFiles.add("file4");
    	sendFiles.add("file5");
    	sendFiles.add("file6");
    	sendFiles.add("file7");
    	sendFiles.add("file8");

    	sendAndReceiveFiles();
    }
    
    
    protected void sendAndReceiveFiles() throws Exception
    {
		final CountDownLatch latch = new CountDownLatch(sendFiles.size());
		final AtomicInteger loopCount = new AtomicInteger(0);

        MuleClient client = new MuleClient();

		// Do some cleaning so that the endpoint doesnt have any other files
		super.cleanupRemoteFtpDirectory(client, "inboundEndpoint");

		receiveFiles = new ArrayList<String>();

		EventCallback callback = new EventCallback()
		{
			public void eventReceived(MuleEventContext context, Object component)
				throws Exception
			{
				logger.info("called " + loopCount.incrementAndGet() + " times");
				FunctionalTestComponent ftc = (FunctionalTestComponent) component;

				String o = IOUtils.toString((SftpInputStream) ftc.getLastReceivedMessage());
				if (sendFiles.contains(o))
				{
					receiveFiles.add(o);
				} else
				{
					fail("The received file was not sent. Received: '" + o + "'");
				}

				latch.countDown();
			}
		};

		getFunctionalTestComponent("receiving").setEventCallback(callback);


		for (String sendFile : sendFiles)
		{
			HashMap<String, String> props = new HashMap<String, String>(1);
			props.put(SftpConnector.PROPERTY_FILENAME, sendFile + ".txt");

			client.send("vm://test.upload", sendFile, props);
		}

		latch.await(TIMEOUT, TimeUnit.MILLISECONDS);

        logger.debug("Number of files sent: " + sendFiles.size());
        logger.debug("Number of files received: " + receiveFiles.size());

        //This makes sure we received the same number of files we sent, and that
        //the content was a match (since only matched content gets on the
        //receiveFiles ArrayList)
        assertTrue( sendFiles.size() == receiveFiles.size() );
    }
}
