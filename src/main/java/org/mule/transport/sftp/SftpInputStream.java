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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.endpoint.ImmutableEndpoint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>SftpInputStream</code> wraps an sftp InputStream.
 */

public class SftpInputStream extends BufferedInputStream implements ErrorOccurredDecorator
{
	private final Log logger = LogFactory.getLog(getClass());

	private SftpClient client;
	private boolean autoDelete = true;
	private String fileName;
	private boolean errorOccured = false;
	private ImmutableEndpoint endpoint;

	/**
	 * A special sftp InputStream.  The constructor creates the InputStream by
	 * calling <code>SftpClient.retrieveFile(fileName)</code>.  The client passed
	 * in is destroyed when the stream is closed.
	 *
	 * @param client	 The SftpClient instance.  Will be destroyed when stream closed.
	 * @param is         The stream that should be used
     * @param fileName   name of the file to be retrieved
	 * @param autoDelete whether the file specified by fileName should be deleted
	 * @param endpoint   the endpoint associated to a specific client (connector) pool.
	 * @throws Exception if failing to retrieve internal input stream.
	 */
	public SftpInputStream(SftpClient client, InputStream is, String fileName, boolean autoDelete, ImmutableEndpoint endpoint) throws Exception
	{
    super(is);

		this.client = client;
		this.fileName = fileName;
		this.autoDelete = autoDelete;
		this.endpoint = endpoint;
	}

	public void close() throws IOException
	{
     try {
      super.close();

      if (autoDelete && !errorOccured)
      {
        client.deleteFile(fileName);
      }
    }
    catch (IOException e)
    {
      logger.error("Error occured while closing file " + fileName, e);
      throw e;
    }
    finally {
      // We should release the connection from the pool even if some error occurs here
      try
      {
        ((SftpConnector) endpoint.getConnector()).releaseClient(endpoint, client);
      } catch (Exception e)
      {
        logger.error(e.getMessage(), e);
      }
    }
	}

	public void setErrorOccurred()
	{
		if (logger.isDebugEnabled()) logger.debug("setErrorOccurred() called");
		this.errorOccured = true;
	}

  @Override
  public String toString() {
    return "SftpInputStream{" +
      "fileName='" + fileName + '\'' +
      " from endpoint=" + endpoint.getEndpointURI() +
      '}';
  }
}
