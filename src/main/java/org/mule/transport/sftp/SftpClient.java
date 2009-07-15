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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * <code>SftpClient</code> Wrapper around jsch sftp library.  Provides access to
 * basic sftp commands.
 *
 */

public class SftpClient
{

    private Log logger = LogFactory.getLog(getClass());

	public static final String CHANNEL_SFTP = "sftp";

	public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

	private ChannelSftp c;

	private JSch jsch;

	private Session session;

	private String host;

	private int port = 22;

	private String home;

	private String fingerPrint;

	public SftpClient()
	{
		jsch = new JSch();
	}

	public boolean changeWorkingDirectory(String wd) throws IOException
	{

		try
		{

            if (!wd.startsWith(home))
            {
                 wd = home + wd;
            }

			if (wd.startsWith("/~"))
			{
				 wd = home + wd.substring( 2, wd.length());
			}

			logger.info("Attempting to cwd to: " + wd);
			c.cd(wd);
		}
		catch (SftpException e)
		{
			e.printStackTrace();
			logger.error("CWD attempt failed, message was: " + e.getMessage(), e);
			throw new IOException(e.getMessage());
		}
		return true;
	}

	public boolean login(String user, String password) throws IOException
	{
		try
		{
			session = jsch.getSession(user, host);
			Properties hash = new Properties();
			hash.put(STRICT_HOST_KEY_CHECKING, "no");
			session.setConfig(hash);
			session.setPort(port);
			session.setPassword(password);
			session.connect();
			if ((fingerPrint != null)
					&& !session.getHostKey().getFingerPrint(jsch).equals(
							fingerPrint))
			{
				throw new RuntimeException("Invalid Fingerprint");
			}
			Channel channel = session.openChannel(CHANNEL_SFTP);
			channel.connect();
			c = (ChannelSftp) channel;
			home = c.pwd();
		} catch (JSchException e)
		{
			throw new IOException(e.getMessage());
		}
		return true;
	}

	public void connect(String uri) throws IOException
	{
		this.host = uri;
	}

	public void connect(String uri, int port) throws IOException
	{
		this.host = uri;
		this.port = port;
	}

	public boolean rename(String filename, String dest) throws IOException
	{
        try
        {
            c.rename(filename, dest);
        }
        catch (SftpException e)
        {
            throw new IOException(e.getMessage());
        }
        return true;
	}

	public boolean deleteFile(String fileName) throws IOException
	{
		try
		{
			c.rm(fileName);
		} catch (SftpException e)
		{
			throw new IOException(e.getMessage());
		}
		return true;
	}

	public void disconnect() throws IOException
	{
		if (c != null)
		{
			c.disconnect();
		}
		if ((session != null) && session.isConnected())
		{
			session.disconnect();
		}
	}

	public boolean isConnected()
	{
		return (c != null) && c.isConnected() && !c.isClosed()
				&& (session != null) && session.isConnected();
	}

	public String[] listFiles() throws IOException
	{
		try
		{
			String path = ".";
			java.util.Vector vv = null;
			vv = c.ls(path);
			if (vv != null)
			{
				ArrayList ret = new ArrayList();
				for (int ii = 0; ii < vv.size(); ii++)
				{
					Object obj = vv.elementAt(ii);
					if (obj instanceof com.jcraft.jsch.ChannelSftp.LsEntry)
					{
						LsEntry entry = (LsEntry) obj;
						if (!entry.getAttrs().isDir())
						{
							ret.add(entry.getFilename());
						}
					}
				}
				return (String[]) ret.toArray(new String[ret.size()]);
			}
		} catch (SftpException e)
		{
			throw new IOException(e.getMessage());
		}
		return null;
	}

	public boolean logout() throws IOException
	{
		return true;
	}

	public InputStream retrieveFile(String fileName) throws IOException
	{
		try
		{
			return c.get(fileName);
		} catch (SftpException e)
		{
			throw new IOException(e.getMessage() + ".  Filename is " + fileName);
		}
	}

	public OutputStream storeFileStream(String fileName) throws IOException
	{
		try
		{
			return c.put(fileName);
		} catch (SftpException e)
		{
			throw new IOException(e.getMessage());
		}
	}

	public boolean storeFile(String fileName, InputStream stream)
			throws IOException
	{
		try
		{
			logger.debug("Sending to SFTP service: Stream = " + stream + " , filename = " + fileName);

			c.put(stream, fileName);
		}
		catch (SftpException e)
		{
		    logger.error("Error writing data over SFTP service, error was: " + e.getMessage(), e);
			throw new IOException(e.getMessage());
		}

		return true;
	}

	public boolean storeFile(String fileNameLocal, String fileNameRemote)
			throws IOException
	{
		try
		{
			c.put(fileNameLocal, fileNameRemote);
		} catch (SftpException e)
		{
			throw new IOException(e.getMessage());
		}
		return true;
	}

	public long getSize(String filename) throws IOException
	{
		try
		{
			return c.stat("./" + filename).getSize();
		} catch (SftpException e)
		{
			throw new IOException(e.getMessage());
		}
	}

}