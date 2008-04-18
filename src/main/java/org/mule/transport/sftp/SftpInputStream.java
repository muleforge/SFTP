package org.mule.transport.sftp;

import java.io.IOException;
import java.io.InputStream;

/**
 * <code>SftpInputStream</code> wraps an sftp InputStream.  
 * 
 * 
 */

public class SftpInputStream extends InputStream
{
	InputStream is;
	SftpClient client;
	boolean autoDelete = true;
	String fileName;
	
	/**
	 * A special sftp InputStream.  The constuctor creates the InputStream by
	 * calling <code>SftpClient.retrieveFile(fileName)</code>.  The client passed
	 * in is destroyed when the stream is closed.
	 * 
	 * @param client The SftpClient instance.  Will be destroyed when stream closed.
	 * @param fileName name of the file to be retrieved
	 * @param autoDelete whether the file specified by fileName should be deleted
	 * 
	 * @return an instance of SftpInputStream
	 */
	public SftpInputStream(SftpClient client,String fileName,boolean autoDelete ) throws Exception
	{
		this.is = client.retrieveFile(fileName);
		this.client = client;
		this.fileName = fileName;
		this.autoDelete = autoDelete;
	}

	public int read(byte[] b) throws IOException
	{
		return is.read(b);
	}
	
	public int read() throws IOException
	{
		return is.read();
	}
	
	public int available() throws IOException
	{
		throw new IOException("Operation not supported by SftpInputStream");
	}
	
	public void close() throws IOException
	{
		//System.out.println("===========Closing SFTP client=====================");
		
		is.close();
		
		if( autoDelete )
		{
		    client.deleteFile(fileName);
		}
		client.disconnect();

		
	}



	

}
