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

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.IOUtils;
import org.mule.api.MuleEventContext;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <code>SftpPoolingFunctionalTestCase</code> tests sending an receiving multiple
 * small text files.
 */

public class SftpPoolingFunctionalTestCase extends AbstractSftpTestCase
{
    private static final long TIMEOUT = 30000;

    private List<String> sendFiles;
    private List<String> receiveFiles;

    private int nrOfFiles = 100;

    protected String getConfigResources()
    {
        return "mule-pooling-test-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        initEndpointDirectory("inboundEndpoint");
    }

    public void testSftpConfig() throws Exception
    {
        SftpConnector c = (SftpConnector) muleContext.getRegistry().lookupConnector("sftp-pool");
        assertEquals(3, c.getMaxConnectionPoolSize());
        assertEquals(true, c.useConnectionPool());

        SftpConnector c2 = (SftpConnector) muleContext.getRegistry().lookupConnector("sftp-no-pool");
        assertEquals(false, c2.useConnectionPool());
    }

    public void testSendAndReceiveMultipleFiles() throws Exception
    {
        sendFiles = new ArrayList<String>();

        for (int i = 1; i <= nrOfFiles; i++)
        {
            sendFiles.add("file" + i);
        }
        sendAndReceiveFiles();
    }

    protected void sendAndReceiveFiles() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(sendFiles.size());
        final AtomicInteger loopCount = new AtomicInteger(0);

        MuleClient client = new MuleClient(muleContext);

        receiveFiles = new ArrayList<String>();

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {

                String filename = context.getMessage().getProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME,
                    null);
                SftpInputStream inputStream = null;
                try
                {
                    logger.info("called " + loopCount.incrementAndGet() + " times. Filename = " + filename);

                    // This is not thread safe! (it should be safe if
                    // synchronous="true" is used)
                    // FunctionalTestComponent ftc = (FunctionalTestComponent)
                    // component;
                    // inputStream = (SftpInputStream) ftc.getLastReceivedMessage();

                    // Use this instead!
                    inputStream = (SftpInputStream) context.getMessage().getPayload();
                    String o = IOUtils.toString(inputStream);
                    if (sendFiles.contains(o))
                    {
                        logger.info("The received file was added. Received: '" + o + "'");
                        receiveFiles.add(o);
                    }
                    else
                    {
                        fail("The received file was not sent. Received: '" + o + "'");
                    }

                    latch.countDown();
                }
                catch (IOException e)
                {
                    logger.error("Error occured while processing callback for file=" + filename, e);
                    throw e;
                }
                finally
                {
                    if (inputStream != null)
                    {
                        inputStream.close();
                    }
                }
            }
        };

        getFunctionalTestComponent("receiving").setEventCallback(callback);

        for (String sendFile : sendFiles)
        {
            HashMap<String, String> props = new HashMap<String, String>(1);
            props.put(SftpConnector.PROPERTY_FILENAME, sendFile + ".txt");

            client.dispatch("vm://test.upload", sendFile, props);
        }

        boolean done = latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        // assertTrue("The test should not time out", done);

        logger.debug("Number of files sent: " + sendFiles.size());
        logger.debug("Number of files received: " + receiveFiles.size());

        // This makes sure we received the same number of files we sent, and that
        // the content was a match (since only matched content gets on the
        // receiveFiles ArrayList)
        assertTrue("expected : " + sendFiles.size() + " but got " + receiveFiles.size(),
            sendFiles.size() == receiveFiles.size());
    }
}
