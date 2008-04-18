package org.mule.providers.sftp.components;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.mule.transport.sftp.LargeFileReceiveFunctionalTestCase;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;

public class ReceiveLargeFileTestHelperComponent implements Callable
{

	
	public Object onCall(UMOEventContext context) throws Exception
    {
    	UMOMessage m = context.getMessage();
    	
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
