package org.mule.transport.sftp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Ensures that the file is moved to the archiveFile folder after a successful consumption of the file
 *
 * @author Magnus Larsson
 */
public class SftpFileArchiveInputStream extends FileInputStream
{
	/**
	 * logger used by this class
	 */
	private static final Log logger = LogFactory.getLog(SftpFileArchiveInputStream.class);

	private File file;
	private File archiveFile;
	private boolean errorOccured = false;

	public SftpFileArchiveInputStream(File file) throws FileNotFoundException
	{
		super(file);

		this.file = file;
		this.archiveFile = null;
	}

	public SftpFileArchiveInputStream(File file, File archiveFile) throws FileNotFoundException
	{
		super(file);

		this.file = file;
		this.archiveFile = archiveFile;
	}

	public void close() throws IOException
	{
		super.close();

		if (!errorOccured && archiveFile != null)
		{
			if (logger.isInfoEnabled())
			{
				logger.info("Move archiveTmpSendingFile (" + file + ") to archiveFolder (" + archiveFile + ")...");
			}
			FileUtils.moveFile(file, archiveFile);
		}
	}

	public void setErrorOccurred()
	{
		this.errorOccured = true;
	}
}