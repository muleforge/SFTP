/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.providers.sftp;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * <code>TransformedStreamFunctionalTestCase</code> tests using the InputStreamToByteArray
 * transformer to materialize an InputStream to a byte[].  This use case is useful
 * when a MessageDispatcher or Component does not handle streams.
 * 
 * 
 */

public class TransformedStreamFunctionalTestCase extends FunctionalTestCase
{
	
    
	//Increase this to be bigger than expected download time
	protected static final long TIMEOUT = 500000;
	
    protected String getConfigResources()
    {
        return "mule-transformed-stream-test-config.xml";
    }

    
    //Place a file in the remote sftp directory specified in mule config above
    //Test is successful if the stream was converted to a byte[] 
    //Note that OOM will result if the file is large, since 
    public void testReceiveFileAsByteArray() throws Exception
    {
    	
        MuleClient client = new MuleClient();  
 
        UMOMessage m;  
        
        while( (m = client.receive("vm://test.download",TIMEOUT)) != null)
        {
            assertTrue(m.getPayload() instanceof byte[]);    	
        }       
        
       
            
    }

    

}
