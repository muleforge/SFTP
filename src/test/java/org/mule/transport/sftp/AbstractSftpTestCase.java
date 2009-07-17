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

import org.mule.tck.FunctionalTestCase;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;

import java.io.IOException;


/**
 *  @author Lennart Häggkvist
  * Date: Jun 8, 2009
 */
public abstract class AbstractSftpTestCase extends FunctionalTestCase
{

	/**
	 * Deletes all files in the directory, useful when testing to ensure that no files are in the way...
	 */
	protected void cleanupRemoteFtpDirectory(MuleClient muleClient, String endpointName) throws IOException
	{
		SftpClient sftpClient = getSftpClient(muleClient, endpointName);

		EndpointURI endpointURI = getUriByEndpointName(muleClient, endpointName);
		sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(endpointURI.getPath()));

		String[] files = sftpClient.listFiles();
		for (String file : files)
		{
			sftpClient.deleteFile(file);
		}
	}

	/**
	 * Deletes the <i>directoryName</i> under the endpoint path
	 */
	protected void deleteRemoteDirectory(MuleClient muleClient, String endpointName, String directoryName) throws IOException
	{
		SftpClient sftpClient = getSftpClient(muleClient, endpointName);

		EndpointURI endpointURI = getUriByEndpointName(muleClient, endpointName);

		// The deleteDirectory operation will fail if there are files in the directory
		try
		{
			sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(endpointURI.getPath() + "/" + directoryName));
			String[] files = sftpClient.listFiles();
			for (String file : files)
			{
				sftpClient.deleteFile(file);
			}
		} catch(Exception e)
		{
			// Ignore, this will occur if the directory didnt exist!
		}

		try
		{
			sftpClient.deleteDirectory(endpointURI.getPath() + "/" + directoryName);
		} catch(IOException e) {
			e.printStackTrace();
		}

		try
		{
			sftpClient.changeWorkingDirectory(endpointURI.getPath() + "/" + directoryName);
			fail("The directory should have been deleted");
		} catch (IOException e)
		{
			// Expected
		}
	}

	/**
	 * Creates the <i>directoryName</i> under the endpoint path
	 */
	protected void createRemoteDirectory(MuleClient muleClient, String endpointName, String directoryName) throws IOException
	{
		SftpClient sftpClient = getSftpClient(muleClient, endpointName);

		EndpointURI endpointURI = getUriByEndpointName(muleClient, endpointName);
		sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(endpointURI.getPath()));

		try
		{
			sftpClient.mkdir(directoryName);
		} catch(IOException e) {
			e.printStackTrace();
			// Expected if the directory didnt exist
		}

		try
		{
			sftpClient.changeWorkingDirectory(endpointURI.getPath() + "/" + directoryName);
		} catch (IOException e)
		{
			fail("The directory should have been created");
		}
	}

	protected EndpointURI getUriByEndpointName(MuleClient muleClient, String endpointName)
	{
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(endpointName);
		return endpoint.getEndpointURI();
	}

	/**
	 *
	 * @param muleClient
	 * @param endpointName
	 * @return the endpoint adress in the form 'sftp://user@host/path'
	 */
	protected String getAddressByEndpoint(MuleClient muleClient, String endpointName) {
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(endpointName);
		EndpointURI endpointURI = endpoint.getEndpointURI();

		return "sftp://" + endpointURI.getUser() + "@" + endpointURI.getHost() + endpointURI.getPath();
	}

	/**
	 * Returns a SftpClient that is logged in to the sftp server that the endpoint is configured against.
	 *
	 * @param muleClient
	 * @param endpointName
	 * @return
	 * @throws IOException
	 */
	protected SftpClient getSftpClient(MuleClient muleClient, String endpointName)
			throws IOException
	{
		SftpClient sftpClient = new SftpClient();
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(endpointName);
		EndpointURI endpointURI = endpoint.getEndpointURI();
		SftpConnector sftpConnector = (SftpConnector) endpoint.getConnector();

		sftpClient.connect(endpointURI.getHost());
		if(sftpConnector.getIdentityFile() != null)
		{
			assertTrue("Login failed", sftpClient.login(endpointURI.getUser(), sftpConnector.getIdentityFile(), sftpConnector.getPassphrase()));
		} else
		{
			assertTrue("Login failed", sftpClient.login(endpointURI.getUser(), endpointURI.getPassword()));
		}
		return sftpClient;
	}

	/**
	 * Checks if the file exists on the server
	 */
	protected boolean verifyFileExists(SftpClient sftpClient, EndpointURI endpointURI, String file) throws IOException
	{
		return verifyFileExists(sftpClient, endpointURI.getPath(), file);
	}

	protected boolean verifyFileExists(SftpClient sftpClient, String path, String file) throws IOException
	{
		sftpClient.changeWorkingDirectory(sftpClient.getAbsolutePath(path));
		String[] files = sftpClient.listFiles();

		for (String remoteFile : files)
		{
			if(file.equals(remoteFile)) {
				return true;
			}
		}
		return false;
	}

}
