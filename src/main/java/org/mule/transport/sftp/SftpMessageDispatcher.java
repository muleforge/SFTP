/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;


import org.mule.transport.AbstractMessageDispatcher;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>SftpMessageDispatcher</code> dispatches files via sftp to a remote sftp service.
 * This dispatcher reads an InputStream, byte[], or String payload, which is then
 * streamed to the SFTP endpoint.
 */

public class SftpMessageDispatcher extends AbstractMessageDispatcher
{

	private SftpConnector connector;

    public SftpMessageDispatcher(OutboundEndpoint endpoint)
	{
        super(endpoint);
		connector = (SftpConnector) endpoint.getConnector();
	}

    protected void doConnect() throws Exception
    {
        //no op

    }
	protected void doDisconnect() throws Exception
	{
		//no op
	}

	protected MuleMessage doReceive(long l) throws Exception
	{
		throw new UnsupportedOperationException("doReceive");
	}

	protected void doDispose()
	{
		//no op
	}

	protected void doDispatch(MuleEvent event) throws Exception
	{


		Object data = event.transformMessage();
    String filename = (String) event
				.getProperty(SftpConnector.PROPERTY_FILENAME);

		SftpConnector sftpConnector = (SftpConnector) connector;

		//If no name specified, set filename according to output pattern specified on
		//endpoint or connector
        if (filename == null)
        {
        	MuleMessage message = event.getMessage();

            String outPattern = (String)endpoint.getProperty(SftpConnector.PROPERTY_OUTPUT_PATTERN);
            if (outPattern == null)
            {
                outPattern = message.getStringProperty(SftpConnector.PROPERTY_OUTPUT_PATTERN,
                connector.getOutputPattern());
            }
            filename = generateFilename(message, outPattern);
        }

        //byte[], String, or InputStream payloads supported.

		byte[] buf;
		InputStream inputStream;

		if (data instanceof byte[])
		{
			buf = (byte[]) data;
			inputStream = new ByteArrayInputStream(buf);
		}
		else if (data instanceof InputStream)
		{
			inputStream = (InputStream) data;

		}
		else if (data instanceof String)
		{
			inputStream = new ByteArrayInputStream(((String) data).getBytes());

		} else
		{
			throw new IllegalArgumentException("Unxpected message type: java.io.InputStream or byte[] expected ");

		}

		logger.info("Writing file to: " + endpoint.getEndpointURI());

		SftpClient client = null;
		boolean useTempDir = false;
		String tempDirAbs = null;

		try
		{

			client = sftpConnector.createSftpClient(endpoint);

			logger.info("Connection setup successful, writing file.");

            String tempDir = connector.getTempDir();
            useTempDir = connector.getUseTempDir();
            if(endpoint.getProperty(SftpConnector.TEMP_DIR) != null)
            {
                tempDir = (String) endpoint.getProperty(SftpConnector.TEMP_DIR);
                useTempDir = true;
            }

            if(useTempDir)
            {
                // Use a temporary directory when uploading
                tempDirAbs = endpoint.getEndpointURI().getPath() + "/" + tempDir;
                // Try to change directory to the temp dir, if it fails - create it
                try
                {
					// This method will throw an exception if the directory does not exist.
                    client.changeWorkingDirectory(tempDirAbs);
                } catch (IOException e)
                {
					logger.info("Got an exception when trying to change the working directory to the temp dir. " +
							"Will try to create the directory " + tempDirAbs);
					client.changeWorkingDirectory(endpoint.getEndpointURI().getPath());
					client.mkdir(tempDir);
					// Now it should exist!
					client.changeWorkingDirectory(tempDirAbs);
                }
            }

			// send file over sftp
			client.storeFile(filename, inputStream);

            if(useTempDir)
            {
                // Good when testing.. :)
//                Thread.sleep(5000);

                // Move the file to its final destination
                String destDir = endpoint.getEndpointURI().getPath();
                client.rename(filename, destDir + "/" + filename);
            }

            logger.info("Successfullt wrote file, done.");
		}
		catch (Exception e)
		{
		    logger.error("Unexpected exception attempting to write file, message was: " + e.getMessage(), e);
			if(inputStream != null)
			{
				// Ensure that the SftpInputStream knows about the error and dont delete the file
				((SftpInputStream) inputStream).setErrorOccured(true);
			}
			if(useTempDir && tempDirAbs != null)
            {
				// Cleanup the remote temp dir!
				try
				{
				client.changeWorkingDirectory(tempDirAbs);
				client.deleteFile(filename);
				} catch (Exception e2)
				{
					// do nothing
				}
			}
		    throw e;
		}
		finally
		{
		    if (client != null)
		    {
		        // If the connection fails, the client will be null, otherwise disconnect.
		        client.disconnect();
		    }
		    else
		    {
		        logger.warn("Unexpected null SFTPClient instance - operation probably failed ...");
		    }

			inputStream.close();

		}

	}

	protected MuleMessage doSend(MuleEvent event) throws Exception
	{
		doDispatch(event);
		return event.getMessage();
	}

	public Object getDelegateSession() throws Exception
	{
		return null;
	}

    private String generateFilename(MuleMessage message, String pattern)
    {
        if (pattern == null)
        {
            pattern = connector.getOutputPattern();
        }
        return connector.getFilenameParser().getFilename(message, pattern);
    }


}
