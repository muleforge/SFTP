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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.sftp.notification.SftpNotifier;

/**
 * <code>SftpMessageDispatcher</code> dispatches files via sftp to a remote sftp service.
 * This dispatcher reads an InputStream, byte[], or String payload, which is then
 * streamed to the SFTP endpoint.
 */

public class SftpMessageDispatcher extends AbstractMessageDispatcher
{

	private SftpConnector connector;
    private SftpUtil sftpUtil = null;

    public SftpMessageDispatcher(OutboundEndpoint endpoint)
	{
        super(endpoint);
		connector = (SftpConnector) endpoint.getConnector();
        sftpUtil = new SftpUtil(connector, endpoint);
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

		if(logger.isDebugEnabled())
		{
		    logger.debug("Writing file to: " + endpoint.getEndpointURI());
		}

		SftpClient client = null;
		boolean useTempDir = false;
		String tempDirAbs = null;

		try
		{
			String serviceName = (event.getService() == null) ? "UNKNOWN SERVICE" : event.getService().getName();
			SftpNotifier notifier = new SftpNotifier(connector, event.getMessage(), endpoint, serviceName);
			client = sftpConnector.createSftpClient(endpoint, notifier);

			if(logger.isDebugEnabled())
			{
			    logger.debug("Connection setup successful, writing file.");
			}
			
			// Duplicate Handling
            String destDir = endpoint.getEndpointURI().getPath();
			String transferFilename = client.duplicateHandling(destDir, filename, sftpUtil.getDuplicateHandling());
			
            String tempDir = connector.getTempDir();
            useTempDir = connector.getUseTempDir();
            if(endpoint.getProperty(SftpConnector.PROPERTY_TEMP_DIR) != null)
            {
                tempDir = (String) endpoint.getProperty(SftpConnector.PROPERTY_TEMP_DIR);
                useTempDir = true;
            }

            if(useTempDir)
            {
            	// TODO. ML FIX. Use SftpClient.createSftpDirIfNotExists() + changeWorkingDirectory once all tests pass!

            	// Use a temporary directory when uploading
                tempDirAbs = destDir + "/" + tempDir;
                // Try to change directory to the temp dir, if it fails - create it
                try
                {
					// This method will throw an exception if the directory does not exist.
                    client.changeWorkingDirectory(tempDirAbs);
                } catch (IOException e)
                {
					logger.info("Got an exception when trying to change the working directory to the temp dir. " +
							"Will try to create the directory " + tempDirAbs);
					client.changeWorkingDirectory(destDir);
					client.mkdir(tempDir);
					// Now it should exist!
					client.changeWorkingDirectory(tempDirAbs);
                }

                // Add unique file-name (if configured) for use during transfer to temp-dir
            	boolean addUniqueSuffix = sftpUtil.isUseTempFileTimestampSuffix();
            	if (addUniqueSuffix) {
            		transferFilename = sftpUtil.createUniqueSuffix(transferFilename);
            	}
            
            }

            
            
            // send file over sftp
			client.storeFile(transferFilename, inputStream);

            if(useTempDir)
            {
                // Good when testing.. :)
//                Thread.sleep(5000);

                // Move the file to its final destination
                client.rename(transferFilename, destDir + "/" + filename);
            }

            logger.info("Successfully wrote file '" + filename + "' to " + endpoint.getEndpointURI());
		}
		catch (Exception e)
		{
		    logger.error("Unexpected exception attempting to write file, message was: " + e.getMessage(), e);
			if(inputStream != null)
			{
				if(inputStream instanceof SftpInputStream)
				{
					// Ensure that the SftpInputStream knows about the error and dont delete the file
					((SftpInputStream) inputStream).setErrorOccured(true);

				} else if(inputStream instanceof SftpFileArchiveInputStream) {
					// Ensure that the SftpFileArchiveInputStream knows about the error and don't delete the file
					((SftpFileArchiveInputStream) inputStream).setErrorOccured(true);

				} else
				{
					logger.warn("Neither SftpInputStream nor SftpFileArchiveInputStream used, errorOccured=true could not be set. Type is " + inputStream.getClass().getName());
				}
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
					logger.error("Could not delete the file from the temp directory", e2);
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
//		    else
//		    {
//		        logger.warn("Unexpected null SFTPClient instance - operation probably failed ...");
//		    }

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
