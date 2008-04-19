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
	
	//Where to save the file locally
	public static final String FILEPATH = "/tmp/";
    
	//Increase this to be a little larger than expected download time
	protected static final long TIMEOUT = 500000;
	
    protected String getConfigResources()
    {
        return "mule-large-receive-test-config.xml";
    }

    
    //Place one or more large files in a remote sftp directory.  This file
    //will be downloaded to FILEPATH.
    public void testReceiveLargeFile() throws Exception
    {
    	
        MuleClient client = new MuleClient();  
        
        //Download each file, stop when no other files are received.
        while( client.request("vm://test.download", TIMEOUT) != null)
        {
            ;     	
        }
        
        //We've passed the test if there's no OOM :)
             

    }

    

}
