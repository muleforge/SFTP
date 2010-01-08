package org.mule.transport.sftp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.mule.api.endpoint.ImmutableEndpoint;

/**
 * Contains reusable methods not directly related to usage of the jsch sftp library (they can be found in the class SftpClient).
 *
 * @author Magnus Larsson
 */
public class SftpUtil
{
	/** Logger */
	private static final Logger logger = Logger.getLogger(SftpUtil.class);

	private SftpConnector connector;
	private ImmutableEndpoint endpoint;

	private static final String DUPLICATE_HANDLING_DEFAULT = SftpConnector.PROPERTY_DUPLICATE_HANDLING_THROW_EXCEPTION;
	private static final boolean KEEP_FILE_ON_ERROR_DEFAULT = true;
	private static final boolean USE_TEMP_FILE_TIMESTAMP_SUFFIX_DEFAULT = false;
	private static final long SIZE_CHECK_WAIT_TIME_DEFAULT = -1;

	public SftpUtil(ImmutableEndpoint endpoint)
	{
		this.endpoint = endpoint;
		this.connector = (SftpConnector) endpoint.getConnector();
	}

	public String createUniqueSuffix(String filename)
	{

		// TODO. Add code for handling no '.'
		int fileTypeIdx = filename.lastIndexOf('.');
		String fileType = filename.substring(fileTypeIdx); // Let the fileType include the leading '.'

		filename = filename.substring(0, fileTypeIdx); // Strip off the leading '/' from the filename

		SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String timstampStr = '_' + timestampFormatter.format(new Date());

		return filename + timstampStr + fileType;
	}


	public String getTempDirInbound()
	{
		String endpointValue = (String) endpoint.getProperty(SftpConnector.PROPERTY_TEMP_DIR);
		if (endpointValue != null)
		{
			return endpointValue;
		}

		String connectorValue = connector.getTempDirInbound();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return null;
	}

	public boolean isUseTempDirInbound()
	{
		return getTempDirInbound() != null;
	}

	public String getTempDirOutbound()
	{
		String endpointValue = (String) endpoint.getProperty(SftpConnector.PROPERTY_TEMP_DIR);
		if (endpointValue != null)
		{
			return endpointValue;
		}

		String connectorValue = connector.getTempDirOutbound();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return null;
	}

	public boolean isUseTempDirOutbound()
	{
		return getTempDirOutbound() != null;
	}

	public void cleanupTempDir(SftpClient sftpClient, String transferFileName, String tempDir)
	{
		String tempDirAbs = sftpClient.getAbsolutePath(endpoint.getEndpointURI().getPath() + "/" + tempDir);
		try
		{
			sftpClient.changeWorkingDirectory(tempDirAbs);
			sftpClient.deleteFile(transferFileName);
		} catch (Exception e)
		{
			logger.error("Could not delete the file '" + transferFileName + "' from the temp directory '" + tempDirAbs + "'", e);
		}
	}

	public long getSizeCheckWaitTime()
	{
		Object endpointValue = endpoint.getProperty(SftpConnector.PROPERTY_SIZE_CHECK_WAIT_TIME);
		if (endpointValue != null)
		{
			return Long.valueOf((String) endpointValue);
		}

		Long connectorValue = connector.getSizeCheckWaitTime();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return SIZE_CHECK_WAIT_TIME_DEFAULT;
	}

	public String getArchiveDir()
	{
		String endpointValue = (String) endpoint.getProperty(SftpConnector.PROPERTY_ARCHIVE_DIR);
		if (endpointValue != null)
		{
			return endpointValue;
		}

		String connectorValue = connector.getArchiveDir();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return null;
	}

	public String getArchiveTempReceivingDir()
	{
		String endpointValue = (String) endpoint.getProperty(SftpConnector.PROPERTY_ARCHIVE_TEMP_RECEIVING_DIR);
		if (endpointValue != null)
		{
			return endpointValue;
		}

		String connectorValue = connector.getArchiveTempReceivingDir();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return null;
	}

	public String getArchiveTempSendingDir()
	{
		String endpointValue = (String) endpoint.getProperty(SftpConnector.PROPERTY_ARCHIVE_TEMP_SENDING_DIR);
		if (endpointValue != null)
		{
			return endpointValue;
		}

		String connectorValue = connector.getArchiveTempSendingDir();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return null;
	}

	public boolean isUseTempFileTimestampSuffix()
	{
		Object endpointValue = endpoint.getProperty(SftpConnector.PROPERTY_USE_TEMP_FILE_TIMESTAMP_SUFFIX);
		if (endpointValue != null)
		{
			return Boolean.valueOf((String) endpointValue);
		}

		Boolean connectorValue = connector.isUseTempFileTimestampSuffix();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return USE_TEMP_FILE_TIMESTAMP_SUFFIX_DEFAULT;
	}

	public String getDuplicateHandling()
	{
		String endpointValue = (String) endpoint.getProperty(SftpConnector.PROPERTY_DUPLICATE_HANDLING);
		if (endpointValue != null)
		{
			return endpointValue;
		}

		String connectorValue = connector.getDuplicateHandling();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return DUPLICATE_HANDLING_DEFAULT;
	}

	public String getIdentityFile()
	{
		String endpointValue = (String) endpoint.getProperty(SftpConnector.PROPERTY_IDENTITY_FILE);
		if (endpointValue != null)
		{
			return endpointValue;
		}

		String connectorValue = connector.getIdentityFile();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return null;
	}

	public String getPassphrase()
	{
		String endpointValue = (String) endpoint.getProperty(SftpConnector.PROPERTY_PASS_PHRASE);
		if (endpointValue != null)
		{
			return endpointValue;
		}

		String connectorValue = connector.getPassphrase();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return null;
	}

	/**
	 * Changes the directory to the temp-dir on the <b>outbound</b> endpoint. Will create the directory if it not already exists.
	 * <p/>
	 * Note, this method is synchronized because it in rare cases can be called from two threads at the same time and thus cause an error.
	 *
	 * @param sftpClient
	 * @param endpointDir
	 * @throws IOException
	 */
	public synchronized void cwdToTempDirOnOutbound(SftpClient sftpClient, String endpointDir) throws IOException
	{
		String tempDir = getTempDirOutbound();

		String tempDirAbs = sftpClient.getAbsolutePath(endpointDir + "/" + tempDir);

		// Try to change directory to the temp dir, if it fails - create it
		try
		{
			// This method will throw an exception if the directory does not exist.
			sftpClient.changeWorkingDirectory(tempDirAbs);
		} catch (IOException e)
		{
			logger.info("Got an exception when trying to change the working directory to the temp dir. " +
				"Will try to create the directory " + tempDirAbs);
			sftpClient.changeWorkingDirectory(endpointDir);
			sftpClient.mkdir(tempDir);
			// Now it should exist!
			sftpClient.changeWorkingDirectory(tempDirAbs);
		}
	}

	public boolean isKeepFileOnError()
	{
		Object endpointValue = endpoint.getProperty(SftpConnector.PROPERTY_KEEP_FILE_ON_ERROR);
		if (endpointValue != null)
		{
			return Boolean.valueOf((String) endpointValue);
		}

		Boolean connectorValue = connector.isKeepFileOnError();
		if (connectorValue != null)
		{
			return connectorValue;
		}

		return KEEP_FILE_ON_ERROR_DEFAULT;
  }
}
