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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.sftp.notification.SftpNotifier;
import org.mule.util.FileUtils;

/**
 * <code>SftpMessageReceiver</code> polls and receives files from an sftp
 * service using jsch. This receiver produces an InputStream payload, which can
 * be materialized in a MessageDispatcher or Component.
 */
public class SftpMessageReceiver extends AbstractPollingMessageReceiver {

    // private String fileExtension;
    protected final FilenameFilter filenameFilter;

    private SftpUtil sftpUtil = null;
    
    public SftpMessageReceiver( SftpConnector connector, Service component,
            InboundEndpoint endpoint, long frequency ) throws CreateException {

        super( connector, component, endpoint );

        sftpUtil = new SftpUtil(connector, endpoint);
        
        this.setFrequency( frequency );

        if ( endpoint.getFilter() instanceof FilenameFilter ) {
            this.filenameFilter = ( FilenameFilter ) endpoint.getFilter();
        } else {
            this.filenameFilter = null;
        }

    }

    public void poll() throws Exception {

        String[] files = getAvailableFiles();

        if ( files.length == 0 ) {
            logger.debug( "No matching files found" );
        }

        for ( int i = 0; i < files.length; i++ ) {
            routeFile( files[i] );

        }

    }

    public void doConnect() throws Exception {
        // no op
    }

    public void doDisconnect() throws Exception {
        // no op
    }

    // Get files in directory configured on the endpoint
    protected String[] getAvailableFiles() throws Exception {
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
            ArrayList completedFiles = new ArrayList( files.length );

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
                    }
                }
                else
                {
                    completedFiles.add( files[i]);
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

	protected InputStream retrieveFile( String fileName, SftpNotifier notifier ) throws Exception {

        SftpConnector sftpConnector = ( SftpConnector ) connector;

        // Getting a new SFTP client dedicated to the SftpInputStream below
        SftpClient client = sftpConnector.createSftpClient( endpoint, notifier );

        // Check usage of tmpSendingDir
        String tmpSedningDir = sftpUtil.getTempDir();
        if (tmpSedningDir != null) {

        	// Check usage of unique names of files during transfer
        	boolean addUniqueSuffix = sftpUtil.isUseTempFileTimestampSuffix();
        	
        	client.createSftpDirIfNotExists(getEndpoint(), tmpSedningDir);
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

	protected void routeFile( String path ) throws Exception {

		// A bit tricky initialization of the notifier in this case since we don't have access to the message yet...
		SftpNotifier notifier = new SftpNotifier((SftpConnector)connector, new DefaultMuleMessage(null), endpoint, service.getName());
    	
    	InputStream inputStream = retrieveFile( path, notifier );

    	if (logger.isDebugEnabled()) logger.debug( "Routing file: " + path );

        MessageAdapter msgAdapter = connector.getMessageAdapter( inputStream );
        msgAdapter.setProperty( SftpConnector.PROPERTY_ORIGINAL_FILENAME, path );
        MuleMessage message = new DefaultMuleMessage( msgAdapter );
        
        // Now we have access to the message, update the notifier with the message
        notifier.setMessage(message);
        
        routeMessage( message, endpoint.isSynchronous() );
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
    protected boolean hasChanged(String fileName, SftpClient client, long fileAge, long sizeCheckDelayMs) throws Exception
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

    /*
     * (non-Javadoc)
     *
     * @see org.mule.transport.AbstractMessageReceiver#doDispose()
     */
    protected void doDispose() {
        // TODO Auto-generated method stub

    }
}
