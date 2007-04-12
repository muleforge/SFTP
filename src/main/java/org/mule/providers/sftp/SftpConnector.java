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


import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.file.FilenameParser;
import org.mule.providers.file.SimpleFilenameParser;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOMessageReceiver;


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
public class SftpConnector extends AbstractServiceEnabledConnector
{

    public static final String PROPERTY_DIRECTORY = "directory";
    public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern";
    public static final String PROPERTY_FILENAME = "filename";
    public static final String PROPERTY_ORIGINAL_FILENAME = "originalFilename";
    public static final String PROPERTY_SELECT_EXPRESSION = "selectExpression";
    public static final String PROPERTY_FILE_EXTENSION = "fileExtension";
    public static final String PROPERTY_INCLUDE_SUBFOLDERS = "includeSubfolders";

    private FilenameParser filenameParser = new SimpleFilenameParser();

    private long pollingFrequency;
    private boolean autoDelete = true;
    private String outputPattern;

    public String getProtocol()
    {
        return "SFTP";
    }

	public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        return serviceDescriptor.createMessageReceiver(this, component, endpoint, new Object[]{});
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

    public SftpClient createSftpClient(UMOEndpointURI endpointURI)  throws Exception
    {
        SftpClient client = new SftpClient();
        
        client.connect(endpointURI.getHost());
        client.login(endpointURI.getUsername(), endpointURI.getPassword());

        logger.info("Successfully connected to: " + endpointURI);
        
        client.changeWorkingDirectory(endpointURI.getPath());
        
        return client;
    }

    




}
