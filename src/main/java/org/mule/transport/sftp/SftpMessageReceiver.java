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


import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractPollingMessageReceiver;


import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * <code>SftpMessageReceiver</code> polls and receives files from an sftp
 * service using jsch. This receiver produces an InputStream payload, which can
 * be materialized in a MessageDispatcher or Component.
 */
public class SftpMessageReceiver extends AbstractPollingMessageReceiver {

    // private String fileExtension;
    protected final FilenameFilter filenameFilter;

    public SftpMessageReceiver( SftpConnector connector, Service component,
            InboundEndpoint endpoint, long frequency ) throws CreateException {

        super( connector, component, endpoint );

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

            String[] files = client.listFiles();

            // Only return files that have completely been written and match
            // fileExtension
            ArrayList completedFiles = new ArrayList( files.length );

            for ( int i = 0; i < files.length; i++ ) {
                // Skip if no match
                if ( filenameFilter != null && !filenameFilter.accept( null, files[i] ) ) {
                    continue;
                }

                if(checkFileAge)
                {
                    // See if the file is still growing, leave it alone if it is
                    if ( !hasChanged( files[i], client, fileAge ) )
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

    protected InputStream retrieveFile( String fileName ) throws Exception {

        SftpConnector sftpConnector = ( SftpConnector ) connector;

        // Getting a new SFTP client dedicated to the SftpInputStream below
        SftpClient client = sftpConnector.createSftpClient( endpoint );

        // This special InputStream closes the SftpClient when the stream is
        // closed.
        // The stream will be materialized in a Message Dispatcher or Service
        // Component
        return new SftpInputStream( client, fileName, sftpConnector.isAutoDelete() );

    }

    protected void routeFile( String path ) throws Exception {
        InputStream inputStream = retrieveFile( path );

        logger.debug( "Routing file: " + path );

        MessageAdapter msgAdapter = connector.getMessageAdapter( inputStream );
        msgAdapter.setProperty( SftpConnector.PROPERTY_ORIGINAL_FILENAME, path );
        MuleMessage message = new DefaultMuleMessage( msgAdapter );
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
    protected boolean hasChanged(String fileName, SftpClient client, long fileAge) throws Exception
    {
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
        else
        {
            return false;
        }

        // The below doesnt work if the connection is fast and the file is written to slowly


        // Take consecutive file size snapshots de determine if file is still
        // being written
//        long fileSize1 = client.getSize(fileName);
//        long fileSize2 = client.getSize(fileName);
//
//        if (fileSize1 == fileSize2)
//        {
//            logger.info("File is stable (not growing), ready for retrieval: " + fileName);
//
//            return false;
//        } else
//        {
//            logger.info("File is growing, deferring retrieval: " + fileName);
//            return true;
//        }
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
