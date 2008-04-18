package org.mule.providers.sftp.components;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.naming.event.EventContext;

import org.apache.commons.io.IOUtils;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.transport.sftp.LargeFileReceiveFunctionalTestCase;

public class ReceiveLargeFileTestHelperComponent implements org.mule.api.lifecycle.Callable
{

	
	public Object onCall(MuleEventContext context) throws Exception
    {
    	MuleMessage m = context.getMessage();
    	
    	String filename = (String) m.getProperty("originalFilename");
    	
    	String downloadPath =  LargeFileReceiveFunctionalTestCase.FILEPATH + filename;
    	
    	InputStream inputStream = (InputStream) m.getPayload();
    	
    	FileOutputStream fos = new FileOutputStream(downloadPath);
    	
    	capture(inputStream,fos);
    	
    	return downloadPath;
    	
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
