package org.mule.transport.sftp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.sftp.notification.SftpNotifier;
import org.mule.util.FileUtils;

/**
 * Contains reusable methods not directly related to usage of the jsch sftp library (they can be found in the class SftpClient).
 * 
 * @author Magnus Larsson
 *
 */
public class SftpReceiverRequesterUtil {

    private transient Log logger = LogFactory.getLog(getClass());

	private SftpConnector connector;
	private ImmutableEndpoint endpoint;
    private final FilenameFilter filenameFilter;
    private SftpUtil sftpUtil = null;

	public SftpReceiverRequesterUtil(ImmutableEndpoint endpoint) {
		this.endpoint = endpoint;
		this.connector = (SftpConnector)endpoint.getConnector();

		sftpUtil = new SftpUtil(endpoint);

		if ( endpoint.getFilter() instanceof FilenameFilter ) {
            this.filenameFilter = ( FilenameFilter ) endpoint.getFilter();
        } else {
            this.filenameFilter = null;
        }

	}

	// Get files in directory configured on the endpoint
    public String[] getAvailableFiles(boolean onlyGetTheFirstOne) throws Exception {
        // This sftp client instance is only for checking available files. This
        // instance cannot be shared
        // with clients that retrieve files because of thread safety

        // TODO: Get instance from a pool
        SftpClient client = null;

        try {
            SftpConnector sftpConnector = (SftpConnector) connector;
            client = sftpConnector.createSftpClient( endpoint );

            long fileAge = sftpConnector.getFileAge();
            boolean checkFileAge = sftpConnector.getCheckFileAge();

            // Override the value from the Endpoint?
            if(endpoint.getProperty(SftpConnector.PROPERTY_FILE_AGE) != null) {
                checkFileAge = true;
                fileAge = Long.valueOf((String) endpoint.getProperty(SftpConnector.PROPERTY_FILE_AGE));
            }

            // Get size check parameter
            long sizeCheckDelayMs = sftpUtil.getSizeCheckWaitTime();

            String[] files = client.listFiles();

            // Only return files that have completely been written and match
            // fileExtension
            List<String> completedFiles = new ArrayList<String>( files.length );

            for ( int i = 0; i < files.length; i++ ) {
                // Skip if no match
                if ( filenameFilter != null && !filenameFilter.accept( null, files[i] ) ) {
                    continue;
                }

                if(checkFileAge || sizeCheckDelayMs >= 0)
                {
                    // See if the file is still growing (either by age or size), leave it alone if it is
                    if ( !hasChanged( files[i], client, fileAge, sizeCheckDelayMs ) )
                    {
    //                    logger.debug("marking file [" + files[i] + "] as in transit.");
    //                    client.rename(files[i], files[i] + ".transtit");
    //                    completedFiles.add( files[i]  + ".transtit" );
                        completedFiles.add( files[i]);
                        if (onlyGetTheFirstOne) break;
                    }
                }
                else
                {
                    completedFiles.add( files[i]);
                    if (onlyGetTheFirstOne) break;
                }

            }

            return ( String[] ) completedFiles.toArray( new String[]{} );
        } catch ( Exception e ) {
            throw e;
        } finally {
        	if( client != null )
                client.disconnect();
        }

    }
	
	public InputStream retrieveFile( String fileName, SftpNotifier notifier ) throws Exception {

        SftpConnector sftpConnector = ( SftpConnector ) connector;

        // Getting a new SFTP client dedicated to the SftpInputStream below
        SftpClient client = sftpConnector.createSftpClient( endpoint, notifier );

        // Check usage of tmpSendingDir
        String tmpSedningDir = sftpUtil.getTempDir();
        if (tmpSedningDir != null) {

        	// Check usage of unique names of files during transfer
        	boolean addUniqueSuffix = sftpUtil.isUseTempFileTimestampSuffix();
        	
        	client.createSftpDirIfNotExists(endpoint, tmpSedningDir);
        	String tmpSendingFileName = tmpSedningDir + "/" + fileName;
        	
        	if (addUniqueSuffix) {
        		tmpSendingFileName = sftpUtil.createUniqueSuffix(tmpSendingFileName);
        	}
        	String fullTmpSendingPath = endpoint.getEndpointURI().getPath() + "/" + tmpSendingFileName;

        	if (logger.isDebugEnabled()) logger.debug("Move " + fileName + " to " + fullTmpSendingPath);
        	client.rename(fileName, fullTmpSendingPath);
        	fileName = tmpSendingFileName;
        	if (logger.isDebugEnabled()) logger.debug("Move done");
        }

        // Archive functionality...
        String archive                = sftpUtil.getArchiveDir();
        if (archive != "") {
        	String archiveTmpReceivingDir = sftpUtil.getArchiveTempReceivingDir();
            String archiveTmpSendingDir   = sftpUtil.getArchiveTempSendingDir();

            InputStream is =  new SftpInputStream( client, fileName, sftpConnector.isAutoDelete() );

        	// TODO ML FIX. Refactor to util-class...
        	int idx = fileName.lastIndexOf('/');
        	String fileNamePart = fileName.substring(idx + 1);
        	
        	// don't use new File() directly, see MULE-1112
            File archiveFile               = FileUtils.newFile(archive, fileNamePart);

            // Should temp dirs be used when handling the archive file?
            if (archiveTmpReceivingDir == "" || archiveTmpSendingDir == "") {
            	
	            return archiveFile(is, archiveFile);

            } else {
	            return archiveFileUsingTempDirs(
	            		archive, archiveTmpReceivingDir, archiveTmpSendingDir, 
	            		is, fileNamePart, archiveFile);
            }
        }
        
        // This special InputStream closes the SftpClient when the stream is
        // closed.
        // The stream will be materialized in a Message Dispatcher or Service
        // Component
        return new SftpInputStream( client, fileName, sftpConnector.isAutoDelete() );

    }

	private InputStream archiveFileUsingTempDirs(String archive,
			String archiveTmpReceivingDir, String archiveTmpSendingDir,
			InputStream is, String fileNamePart, File archiveFile)
			throws IOException, FileNotFoundException {
		
		File archiveTmpReceivingFolder = FileUtils.newFile(archive + '/' + archiveTmpReceivingDir);
		File archiveTmpReceivingFile   = FileUtils.newFile(archive + '/' + archiveTmpReceivingDir, fileNamePart);
		if (!archiveTmpReceivingFolder.exists()) {
			if (logger.isDebugEnabled()) logger.debug("Creates " + archiveTmpReceivingFolder.getAbsolutePath());
			archiveTmpReceivingFolder.mkdirs();
		}

		File archiveTmpSendingFolder   = FileUtils.newFile(archive + '/' + archiveTmpSendingDir);
		File archiveTmpSendingFile     = FileUtils.newFile(archive + '/' + archiveTmpSendingDir,   fileNamePart);
		if (!archiveTmpSendingFolder.exists()) {
			if (logger.isDebugEnabled()) logger.debug("Creates " + archiveTmpSendingFolder.getAbsolutePath());
			archiveTmpSendingFolder.mkdirs();
		}

		if (logger.isDebugEnabled()) logger.debug("Copy SftpInputStream to archiveTmpReceivingFile... " + archiveTmpReceivingFile.getAbsolutePath());
		FileUtils.copyStreamToFile(is, archiveTmpReceivingFile);
		
		// TODO. ML FIX. Should be performed before the sftp:delete - operation, i.e. in the SftpInputStream in the operation above...
		if (logger.isDebugEnabled()) logger.debug("Move archiveTmpReceivingFile (" + archiveTmpReceivingFile + ") to archiveTmpSendingFile (" + archiveTmpSendingFile + ")...");
		FileUtils.moveFile(archiveTmpReceivingFile, archiveTmpSendingFile);
		
		if (logger.isDebugEnabled()) logger.debug("Return SftpFileArchiveInputStream for archiveTmpSendingFile (" + archiveTmpSendingFile + ")...");
		return new SftpFileArchiveInputStream(archiveTmpSendingFile, archiveFile);
	}

	private InputStream archiveFile(InputStream is, File archiveFile)
			throws IOException, FileNotFoundException {
		if (logger.isDebugEnabled()) logger.debug("Copy SftpInputStream to archiveFile... " + archiveFile.getAbsolutePath());
		FileUtils.copyStreamToFile(is, archiveFile);
		
		if (logger.isDebugEnabled()) logger.debug("*** Return SftpFileArchiveInputStream for archiveFile...");
		return new SftpFileArchiveInputStream(archiveFile);
	}

    /**
     * Checks if the file has been changed.
     * <p/>
     * Note! This assumes that the time on both servers are synchronized!
     *
     * @param fileName The file to check
     * @param client instance of StftClient
     * @param fileAge How old the file should be to be considered "old" and not changed
     * @return true if the file has changed
     * @throws Exception Error
     */
    private boolean hasChanged(String fileName, SftpClient client, long fileAge, long sizeCheckDelayMs) throws Exception
    {
    	// Perform fileAge test if configured
    	// Note that for this to work it is required that the system clock on the mule server 
    	// is synchronised with the system clock on the sftp server
    	if(fileAge > 0) {
	    	long lastModifiedTime = client.getLastModifiedTime(fileName);
	        // TODO Can we get the current time from the other server?
	        long now = System.currentTimeMillis();
	        long diff = now - lastModifiedTime;
			// If the diff is negative it's a sign that the time on the test server and the ftps-server is not synchronized
	        if (diff < fileAge)
	        {
	            if (logger.isDebugEnabled())
	            {
	                logger.debug("The file has not aged enough yet, will return nothing for: "
	                        + fileName + ". The file must be " + (fileAge - diff) + "ms older, was " + diff);
	            }
	            return true;
	        }
    	}


        // Perform a size check with a short configurable latencey between the size-calls
        // Take consecutive file size snapshots to determine if file is still being written
        if (sizeCheckDelayMs > 0) {
	        logger.info("Perform size check with a delay of: " + sizeCheckDelayMs + " ms.");
	        long fileSize1 = client.getSize(fileName);
	        Thread.sleep(sizeCheckDelayMs); 
	        long fileSize2 = client.getSize(fileName);
	
	        if (fileSize1 == fileSize2)
	        {
	            logger.info("File is stable (not growing), ready for retrieval: " + fileName);
	        } else
	        {
	            logger.info("File is growing, deferring retrieval: " + fileName);
	            return true;
	        }
        }
       
        // None of file-change tests faile so we can retrieve this file
        return false;
    }

}
