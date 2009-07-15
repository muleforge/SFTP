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


import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.AbstractConnector;
import org.mule.transport.file.FilenameParser;
import org.mule.transport.file.SimpleFilenameParser;


import java.util.Map;


/**
 * <code>SftpConnector</code> sends and receives file messages over sftp using jsch library
 * Improves on SFTP with VFS Connector in the following ways:
 * 1. Streams files instead of reading them into memory.  The SftpMessageReceiver is a "non-materializing stream receiver"
 *    which does not read the file to memory.  The SftpMessageDispatcher also never materializes the stream and delegates
 *    the jsch library for materialization.
 * 3. Uses jsch library directly instead of using VFS as middle-man. 
 * 3. More explicit connection lifefecyle management.  
 * 4. Leverages sftp stat to determine if a file size changes (simpler and also less memory intensive)
 * 
 */
public class SftpConnector extends AbstractConnector
{

    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";    
    public static final String PROPERTY_DIRECTORY = "directory";
    public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern";
    public static final String PROPERTY_FILENAME = "filename";
    public static final String PROPERTY_ORIGINAL_FILENAME = "originalFilename";
    public static final String PROPERTY_SELECT_EXPRESSION = "selectExpression";
    public static final String PROPERTY_FILE_EXTENSION = "fileExtension";
    public static final String PROPERTY_INCLUDE_SUBFOLDERS = "includeSubfolders";

    public static final int DEFAULT_POLLING_FREQUENCY = 1000;
    
    private FilenameParser filenameParser = new SimpleFilenameParser();

    private long pollingFrequency;
    private boolean autoDelete = true;
    private String outputPattern;

    public String getProtocol()
    {
        return "sftp";
    }

	public MessageReceiver createReceiver(Service component, InboundEndpoint endpoint) throws Exception
    {
                
        long polling = pollingFrequency;
         

        // Override properties on the endpoint for the specific endpoint
        String tempPolling = ( String ) endpoint.getProperty( PROPERTY_POLLING_FREQUENCY );
        if ( tempPolling != null ) 
        {
            polling = Long.parseLong( tempPolling );
        }
        
        if ( polling <= 0 ) 
        {
            polling = DEFAULT_POLLING_FREQUENCY;
        }
        logger.debug( "set polling frequency to " + polling );
        
        return serviceDescriptor.createMessageReceiver( this, component, endpoint,
                new Object[]{new Long( polling )} );
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }


	public FilenameParser getFilenameParser() 
	{
		return filenameParser;
	}

	public void setFilenameParser(FilenameParser filenameParser)
	{
		this.filenameParser = filenameParser;
	}

	public String getOutputPattern() 
	{
		return outputPattern;
	}

	public void setOutputPattern(String outputPattern) 
	{
		this.outputPattern = outputPattern;
	}

    public boolean isAutoDelete()
    {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete)
    {
        this.autoDelete = autoDelete;
     
    }

    public SftpClient createSftpClient(EndpointURI endpointURI)  throws Exception
    {
        SftpClient client = new SftpClient();
        
        final int uriPort = endpointURI.getPort();
        if (uriPort == 0)
        {
            logger.info("Cnnecting to host: " + endpointURI.getHost());
            client.connect(endpointURI.getHost());
        }
        else
        {
            logger.info("Cnnecting to host: " + endpointURI.getHost() + ", on port: " + String.valueOf(uriPort));
            client.connect(endpointURI.getHost(), endpointURI.getPort());
        }

        client.login(endpointURI.getUser(), endpointURI.getPassword());

        logger.info("Successfully connected to: " + endpointURI);
        
        client.changeWorkingDirectory(endpointURI.getPath());

        logger.info("Successfully changed working directory to: " + endpointURI.getPath());

        return client;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#doConnect()
     */
    protected void doConnect() throws Exception {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#doDisconnect()
     */
    protected void doDisconnect() throws Exception {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#doDispose()
     */
    protected void doDispose() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#doInitialise()
     */
    protected void doInitialise() throws InitialisationException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#doStart()
     */
    protected void doStart() throws MuleException {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#doStop()
     */
    protected void doStop() throws MuleException {
        // TODO Auto-generated method stub
        
    }

    




}
