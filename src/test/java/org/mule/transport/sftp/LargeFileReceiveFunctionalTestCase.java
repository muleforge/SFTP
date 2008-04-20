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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

/**
 * <code>LargeFileReceiveFunctionalTestCase</code> tests receiving a large file message
 * from an sftp service.  
 * 
 * 
 */

public class LargeFileReceiveFunctionalTestCase extends FunctionalTestCase
{
	   
	private static final Log logger = LogFactory.getLog(LargeFileReceiveFunctionalTestCase.class);
	
	//Increase this to be a little larger than expected download time
	protected static final long TIMEOUT = 500000;
	
    protected String getConfigResources()
    {
        return "mule-large-receive-test-config.xml";
    }

    
    //Downloads large file in the remote directory specified in config
    public void testReceiveLargeFile() throws Exception
    {
    	
        MuleClient client = new MuleClient();  

        
        MuleMessage m = client.request("vm://test.download", TIMEOUT);

        if( m!= null )
        {
        	String filename = (String) m.getProperty("originalFilename");
        	
        	InputStream inputStream = (InputStream) m.getPayload();
        	
        	FileOutputStream fos = new FileOutputStream("/tmp/" + filename);
        	
        	capture(inputStream,fos);
        }
    	
        //To do: match remote and local file sizes.  Manually check file in /tmp in meantime.
             

    }

	private void capture(InputStream inputStream, OutputStream outputStream)
	    throws Exception
	{

		try
		{

			byte[] buffer = new byte[1024];
			int len;
			while ((len = inputStream.read(buffer)) > 0)
			{
				//System.out.println("downloading chunk of " + len);
				outputStream.write(buffer, 0, len);
			}

		} finally
		{
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outputStream);
		}

    } 

}
