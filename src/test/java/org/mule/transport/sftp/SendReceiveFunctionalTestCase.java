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
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.sftp.SftpConnector;
import org.mule.umo.UMOMessage;

/**
 * <code>SendReceiveFunctionalTestCase</code> tests sending an receiving multiple
 * small text files.  
 * 
 * 
 */

public class SendReceiveFunctionalTestCase extends FunctionalTestCase
{

	private static final long TIMEOUT = 30000;
 
	private ArrayList sendFiles;
	private ArrayList receiveFiles;
	
	
    protected String getConfigResources()
    {
        return "mule-send-receive-test-config.xml";
    }

      

    public void testSendAndReceiveSingleFile() throws Exception
    {
    	sendFiles = new ArrayList();
    	
    	sendFiles.add("file created on " + new Date());
    	
    	sendAndReceiveFiles();
    }
       
  
    public void testSendAndReceiveMultipleFiles() throws Exception
    {
    	sendFiles = new ArrayList();
    	
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());
    	sendFiles.add("file created on " + new Date());    	
    	
    	
    	sendAndReceiveFiles();
    }   
 
    
    //Test Mule-1477 (an old VFS Connector issue, but test anyway).
    public void testSendAndReceiveEmptyFile() throws Exception
    {
    	sendFiles = new ArrayList();
    	
    	sendFiles.add("");
    	
    	sendAndReceiveFiles();
    }

   
    protected void sendAndReceiveFiles() throws Exception
    {
        MuleClient client = new MuleClient();  
                
        for( int i = 0; i < sendFiles.size(); i++)
        {
            HashMap props = new HashMap(1);
            props.put(SftpConnector.PROPERTY_FILENAME,System.currentTimeMillis() + ".txt");
        	
            client.send("vm://test.upload",sendFiles.get(i),props);        	
        }
   
   
        
        UMOMessage m;
        
        receiveFiles = new ArrayList();
        
        while( (m = client.receive("vm://test.download",TIMEOUT)) != null)
        {

            assertTrue( m.getPayload() instanceof InputStream );
        	String fileText = getStringFromInputStream( (InputStream) m.getPayload() );
            assertNotNull(fileText);
        	
            
        	if( sendFiles.contains(fileText))
        	{
                receiveFiles.add(fileText);	
        	}      	
        }

        logger.debug("Number of files sent: " + sendFiles.size());
        logger.debug("Number of files received: " + receiveFiles.size());
        
        //This makes sure we received the same number of files we sent, and that
        //the content was a match (since only matched content gets on the 
        //receiveFiles ArrayList)
        assertTrue( sendFiles.size() == receiveFiles.size() );
        
      
    }
    
    protected String getStringFromInputStream(InputStream inputStream) throws Exception
    {
        try
        {
        	
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            	
                byte [] buffer = new byte[1024];
                int len;
                while( (len = inputStream.read(buffer)) > 0) 
                {
                	baos.write(buffer,0,len);
                } 


            return new String(baos.toByteArray());
        }
        finally
        {
            IOUtils.closeQuietly(inputStream);
        }
    }

}
