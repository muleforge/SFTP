/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.sftp;

import org.mule.MuleException;
import org.mule.config.i18n.MessageFactory;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * <code>SftpMessageDispatcher</code> dispatches files via sftp to a remote sftp service.
 * This dispatcher reads an InputStream, byte[], or String payload, which is then
 * streamed to the SFTP endpoint.
 */

public class SftpMessageDispatcher extends AbstractMessageDispatcher
{

	private SftpConnector connector;

	public SftpMessageDispatcher(UMOImmutableEndpoint umoImmutableEndpoint)
	{
		super(umoImmutableEndpoint);
		connector = (SftpConnector) umoImmutableEndpoint.getConnector();
	}

	protected void doConnect(UMOImmutableEndpoint umoImmutableEndpoint)
			throws Exception
	{
		//no op
	}

	protected void doDisconnect() throws Exception
	{
		//no op
	}

	protected UMOMessage doReceive(UMOImmutableEndpoint umoImmutableEndpoint,
			long l) throws Exception
	{
		throw new UnsupportedOperationException("doReceive");
	}

	protected void doDispose()
	{
		//no op
	}

	protected void doDispatch(UMOEvent event) throws Exception
	{

		Object data = event.getTransformedMessage();
		String filename = (String) event
				.getProperty(SftpConnector.PROPERTY_FILENAME, true);
		
		SftpConnector sftpConnector = (SftpConnector) connector;

		//If no name specified, set filename according to output pattern specified on 
		//endpoint or connector
        if (filename == null)
        {
        	UMOMessage message = event.getMessage();
        	
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
			throw new MuleException(
					MessageFactory.createStaticMessage("Unxpected message type: java.io.InputStream or byte[] expected "));

		}

		logger.info("Writing file to: " + endpoint.getEndpointURI());

		SftpClient client = null;

		try
		{

			client = sftpConnector.createSftpClient(endpoint
					.getEndpointURI());

			// send file over sftp
			client.storeFile(filename, inputStream);

		} 
		finally
		{

			client.disconnect();
			inputStream.close();

		}

	}

	protected UMOMessage doSend(UMOEvent event) throws Exception
	{
		doDispatch(event);
		return event.getMessage();
	}

	public Object getDelegateSession() throws UMOException
	{
		return null;
	}

    private String generateFilename(UMOMessage message, String pattern)
    {
        if (pattern == null)
        {
            pattern = connector.getOutputPattern();
        }
        return connector.getFilenameParser().getFilename(message, pattern);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractMessageDispatcher#doConnect()
     */
    protected void doConnect() throws Exception {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractMessageDispatcher#doReceive(long)
     */
    protected UMOMessage doReceive( long timeout ) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }
}
