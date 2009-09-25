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

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.sftp.notification.SftpNotifier;

import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;
 
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_GET_ACTION;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_PUT_ACTION;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_RENAME_ACTION;
import static org.mule.transport.sftp.notification.SftpTransportNotification.SFTP_DELETE_ACTION;

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
	private SftpNotifier notifier;

	private Session session;

	private String host;

	private int port = 22;

	private String home;

	private String fingerPrint;

	// Keep track of the current working directory for improved logging.
	private String currentDirectory = "";
	
	public SftpClient()
	{
		this(null);
	}
	
	public SftpClient(SftpNotifier notifier)
	{
		jsch = new JSch();
		this.notifier = notifier;
	}

	public boolean changeWorkingDirectory(String wd) throws IOException
	{
		currentDirectory = wd;

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
			String message = "Error '" + e.getMessage() + "' occured when trying to CDW to '" + wd + "'.";
//			logger.error(message, e);
			throw new IOException(message);
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
    public String getAbsolutePath(String path)
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
			logAndThrowLoginError(user, e);
		} catch (SftpException e)
	    {
			logAndThrowLoginError(user, e);
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
			logAndThrowLoginError(user, e);
		} catch (SftpException e)
		{
			logAndThrowLoginError(user, e);
		}
		return true;
	}

	private void logAndThrowLoginError(String user, Exception e) throws IOException {
		logger.error("Error during login to " + user + "@" + host, e);
		throw new IOException("Error during login to " + user + "@" + host + ": " + e.getMessage());
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
		// Notify sftp rename file action
		if (notifier != null) notifier.notify(SFTP_RENAME_ACTION, "from: " + currentDirectory + "/" + filename + " - to: " + dest);

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
		// Notify sftp delete file action
		if (notifier != null) notifier.notify(SFTP_DELETE_ACTION, currentDirectory + "/" + fileName);

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
		return listFiles(".");
	}

	public String[] listFiles(String path) throws IOException
	{
		try
		{
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
		// Notify sftp get file action
		long size = getSize(fileName);
		if (notifier != null) notifier.notify(SFTP_GET_ACTION, currentDirectory + "/" + fileName, size);

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

			// Notify sftp put file action
			if (notifier != null) notifier.notify(SFTP_PUT_ACTION, currentDirectory + "/" + fileName);

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
			throw new IOException(e.getMessage() + " (" + currentDirectory + "/" + filename + ")");
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
			throw new IOException("Could not create the directory '" + directoryName + "', caused by: " + e.getMessage());
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

	public void createSftpDirIfNotExists(InboundEndpoint endpoint, String newDir) throws IOException {
		String newDirAbs = endpoint.getEndpointURI().getPath() + "/" + newDir;

		String currDir = currentDirectory;
		
        // Try to change directory to the new dir, if it fails - create it
        try
        {
			// This method will throw an exception if the directory does not exist.
        	if (logger.isDebugEnabled()) logger.debug("CHANGE DIR FROM " + currentDirectory + " TO " + newDirAbs);
            changeWorkingDirectory(newDirAbs);
        } catch (IOException e)
        {
			logger.info("Got an exception when trying to change the working directory to the new dir. " +
					"Will try to create the directory " + newDirAbs);
			changeWorkingDirectory(endpoint.getEndpointURI().getPath());
			mkdir(newDir);

			// Now it should exist!
			changeWorkingDirectory(newDirAbs);
        } finally {
        	changeWorkingDirectory(currDir);
        	if (logger.isDebugEnabled()) logger.debug("DIR IS NOW BACK TO " + currentDirectory);
        }
	}

	public String duplicateHandling(String destDir, String filename, String duplicateHandling) throws IOException
	{
		if (duplicateHandling.equals(SftpConnector.PROPERTY_DUPLICATE_HANDLING_ASS_SEQ_NO)) {
			filename = createUniqueName(destDir, filename);

		} else if (duplicateHandling.equals(SftpConnector.PROPERTY_DUPLICATE_HANDLING_OVERWRITE)) {
			// TODO. ML FIX. Implement this!
			throw new NotImplementedException("Strategy " + SftpConnector.PROPERTY_DUPLICATE_HANDLING_OVERWRITE + " is not yet implemented");

		} else {
			// Nothing to do in the case of PROPERTY_DUPLICATE_HANDLING_THROW_EXCEPTION, if the file already exists then an error will be throwed...
		}
		
		return filename;
	}

	private String createUniqueName(String dir, String path) throws IOException {

		int fileIdx = 1;

		// TODO. Add code for handling no '.'
		int fileTypeIdx = path.lastIndexOf('.');
		String fileType = path.substring(fileTypeIdx); // Let the fileType include the leading '.'
		String filename = path.substring(0, fileTypeIdx);

		if (logger.isDebugEnabled()) logger.debug("Create a unique name for: " + path + " (" + dir + " - " + filename + " - " + fileType + ")");

		String uniqueFilename = filename;
		String[] existingFiles = listFiles(getAbsolutePath(dir));

		while (existsFile(existingFiles, uniqueFilename, fileType)) {
			uniqueFilename = filename + '_' + fileIdx++;
		}
		
		uniqueFilename = uniqueFilename + fileType;
		if (!path.equals(uniqueFilename) && logger.isInfoEnabled()) logger.info("A file with the original filename (" + path + ") already exists, new name: " + uniqueFilename); 
		if (logger.isDebugEnabled()) logger.debug("Unique name returned: " + uniqueFilename);
		return uniqueFilename;
	}

	private boolean existsFile(String[] files, String filename, String fileType) throws IOException {
		boolean existsFile = false;
		filename += fileType;
		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(filename)) {
				if (logger.isDebugEnabled()) logger.debug("Found existing file: " + files[i]);
				existsFile = true;
			}
		}
		return existsFile;
	}

}