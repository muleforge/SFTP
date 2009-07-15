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
	boolean errorOccured = false;

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

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        return is.read(b, off, len);
    }

	public int available() throws IOException
	{
		throw new IOException("Operation not supported by SftpInputStream");
	}

	public void close() throws IOException
	{
		//System.out.println("===========Closing SFTP client=====================");

		is.close();


		if( autoDelete && !errorOccured)
		{
		    client.deleteFile(fileName);
		}
		client.disconnect();


	}


	public void setErrorOccured(boolean errorOccured)
	{
		this.errorOccured = errorOccured;
	}
}
