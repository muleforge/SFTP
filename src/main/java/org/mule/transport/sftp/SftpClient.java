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

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.*;
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
            wd = getAbsolutePath(wd);
			if(logger.isDebugEnabled())
			{
				logger.debug("Attempting to cwd to: " + wd);
			}
			c.cd(wd);
		}
		catch (SftpException e)
		{
			logger.error("CWD attempt to '" + wd + "' failed, message was: " + e.getMessage(), e);
			throw new IOException(e.getMessage());
		}
		return true;
	}

	/**
	 * Converts a relative path to an absolute path.
	 *
	 * Note! If this method is called twice or more on an absolute path the result will be wrong!
	 * Example, the endpoint-address  "sftp://user@srv//tmp/muletest1/foo/inbound"
	 * will first result in the path "/tmp/muletest1/foo/inbound" (correct), but the next
	 * will result in "/home/user/tmp/muletest1/foo/inbound".
	 * <p/>
	 *
	 * @param path
	 * @return
	 */
    String getAbsolutePath(String path)
    {
		if(path.startsWith("//")) {
			// This is an absolute path! Just remove the first /
			return path.substring(1);
		}

		if (!path.startsWith(home))
        {
            path = home + path;
        }

        if (path.startsWith("/~"))
        {
            path = home + path.substring(2, path.length());
        }
		// Now absolute!
        return path;
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
			setHome(c.pwd());
		} catch (JSchException e)
		{
			throw new IOException(e.getMessage());
		} catch (SftpException e)
    {
      throw new IOException(e.getMessage());
    }
    return true;
	}

	public boolean login(String user, String identityFile, String passphrase) throws IOException
	{
		// Lets first check that the identityFile exist
		if(!new File(identityFile).exists()) {
			throw new IOException("IdentityFile '" + identityFile + "' not found");
		}

		try
		{
			if (passphrase == null || "".equals(passphrase))
			{
				jsch.addIdentity(new File(identityFile).getAbsolutePath());
			} else
			{
				jsch.addIdentity(new File(identityFile).getAbsolutePath(), passphrase);
			}

			session = jsch.getSession(user, host);
			Properties hash = new Properties();
			hash.put(STRICT_HOST_KEY_CHECKING, "no");
			session.setConfig(hash);
			session.setPort(port);
			session.connect();
			if ((fingerPrint != null)
					&& !session.getHostKey().getFingerPrint(jsch).equals(fingerPrint))
			{
				throw new RuntimeException("Invalid Fingerprint");
			}
			Channel channel = session.openChannel(CHANNEL_SFTP);
			channel.connect();
			c = (ChannelSftp) channel;
			setHome(c.pwd());
		} catch (JSchException e)
		{
			logger.error("Error during login to " + user + "@" + host, e);
			throw new IOException(e.getMessage());
		} catch (SftpException e)
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
		String absolutePath  = getAbsolutePath(dest);
        try
        {
          if( logger.isDebugEnabled())
          {
            logger.debug("Will try to rename " + filename + " to " + absolutePath);
          }
            c.rename(filename, absolutePath);
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
			if (logger.isDebugEnabled())
			{
				logger.debug("Will try to delete " + fileName);
			}
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
			if(logger.isDebugEnabled())
			{
			logger.debug("Sending to SFTP service: Stream = " + stream + " , filename = " + fileName);
			}

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

	/**
	 * @param filename
	 * @return Number of seconds since the file was written to
	 * @throws IOException
	 */
	public long getLastModifiedTime(String filename) throws IOException
	{
		try
		{
			SftpATTRS attrs = c.stat("./" + filename);
			return attrs.getMTime() * 1000L;
		} catch (SftpException e)
		{
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Creates a directory
	 * @param directoryName
	 * @throws IOException
	 */
	public void mkdir(String directoryName) throws IOException
	{
		try
		{
			if(logger.isDebugEnabled())
			{
				logger.debug("Will try to create directory " + directoryName);
			}
			c.mkdir(directoryName);
		} catch (SftpException e)
		{
			// Dont throw e.getmessage since we only get "2: No such file"..
//			throw new IOException(e.getMessage());
			throw new IOException("Could not create the directory '" + directoryName + "'");
		}
	}

	public void deleteDirectory(String path) throws IOException
	{
		path = getAbsolutePath(path);
		try
		{
			if(logger.isDebugEnabled())
			{
				logger.debug("Will try to delete directory " + path);
			}
			c.rmdir(path);
		} catch (SftpException e)
		{
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Setter for 'home'
	 * @param home
	 */
	void setHome(String home)
	{
		this.home = home;
	}

	/**
	 *
	 * @return the ChannelSftp - useful for some tests
	 */
	ChannelSftp getChannelSftp()
	{
		return c;
	}
}