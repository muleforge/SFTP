package org.mule.transport.sftp;

import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.sftp.notification.SftpNotifier;

/**
 * TODO. ML FIX. Based on a copy of the SftpReceiver - class. Common code needs to be refactored out to a common class.
 *
 * @author dfc0346
 *
 */
public class SftpMessageRequester extends AbstractMessageRequester {

    protected final FilenameFilter filenameFilter;

    public SftpMessageRequester(InboundEndpoint endpoint) {
		super(endpoint);

		if ( endpoint.getFilter() instanceof FilenameFilter ) {
            this.filenameFilter = ( FilenameFilter ) endpoint.getFilter();
        } else {
            this.filenameFilter = null;
        }
	}

	@Override
	protected MuleMessage doRequest(long timeout) throws Exception {
		String[] files = getAvailableFiles(true);
		
		if (files.length == 0) return null;
		
		String path = files[0];
		// TODO. ML FIX. Can't we figure out the current service???
		SftpNotifier notifier = new SftpNotifier((SftpConnector)connector, new DefaultMuleMessage(null), endpoint, endpoint.getName());

		InputStream inputStream = retrieveFile( path, notifier );

        logger.debug( "Routing file: " + path );

        MessageAdapter msgAdapter = connector.getMessageAdapter( inputStream );
        msgAdapter.setProperty( SftpConnector.PROPERTY_ORIGINAL_FILENAME, path );
        MuleMessage message = new DefaultMuleMessage( msgAdapter );

        // Now we can update the notifier with the message
        notifier.setMessage(message);
        return message;
	}

	// -------------------------------------
	//
	// Reusable code from SftpMessageReceiver
	//
	// -------------------------------------
	
    // Get files in directory configured on the endpoint
    protected String[] getAvailableFiles(boolean onlyGetTheFirstOne) throws Exception {
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
	
    protected InputStream retrieveFile( String fileName, SftpNotifier notifier ) throws Exception {

        SftpConnector sftpConnector = ( SftpConnector ) connector;

        // Getting a new SFTP client dedicated to the SftpInputStream below
        SftpClient client = sftpConnector.createSftpClient( endpoint, notifier );

        // This special InputStream closes the SftpClient when the stream is
        // closed.
        // The stream will be materialized in a Message Dispatcher or Service
        // Component
        return new SftpInputStream( client, fileName, sftpConnector.isAutoDelete() );

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
	
}
