package org.mule.transport.sftp;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.mule.api.endpoint.ImmutableEndpoint;

/**
 * Contains reusable methods not directly related to usage of the jsch sftp library.
 * 
 * @author magnus larsson
 *
 */
public class SftpUtil {

	private SftpConnector connector;
	private ImmutableEndpoint endpoint;

	public SftpUtil(SftpConnector connector, ImmutableEndpoint endpoint) {
		this.connector = connector;
		this.endpoint = endpoint;
	}
	
    public String createUniqueSuffix(String filename) {

		// TODO. Add code for handling no '.'
		int fileTypeIdx = filename.lastIndexOf('.');
		String fileType = filename.substring(fileTypeIdx); // Let the fileType include the leading '.'

		filename = filename.substring(0, fileTypeIdx); // Strip off the leading '/' from the filename
    	
    	SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String timstampStr = '_' + timestampFormatter.format(new Date());

		return filename + timstampStr + fileType;
	}
    
    
    public String getTempDir() {
        String tempDir = connector.getTempDir();

        // Override the value from the endpoint?
        Object v = endpoint.getProperty(SftpConnector.PROPERTY_ARCHIVE_TEMP_RECEIVING_DIR);
        if(v != null) {
        	tempDir = (String)v;
        }
        
        return tempDir;
	}
    
    public long getSizeCheckWaitTime() {
        long sizeCheckWaitTime = connector.getSizeCheckWaitTime();

        // Override the value from the endpoint?
        Object v = endpoint.getProperty(SftpConnector.PROPERTY_SIZE_CHECK_WAIT_TIME);
        if(v != null) {
        	sizeCheckWaitTime = Long.valueOf((String)v);
        }
        
        return sizeCheckWaitTime;
	}
    
    public String getArchiveDir() {
        String archiveDir = connector.getArchiveDir();

        // Override the value from the endpoint?
        Object v = endpoint.getProperty(SftpConnector.PROPERTY_ARCHIVE_DIR);
        if(v != null) {
        	archiveDir = (String)v;
        }
        
        return archiveDir;
	}
    
    public String getArchiveTempReceivingDir() {
        String archiveTempReceivingDir = connector.getArchiveTempReceivingDir();

        // Override the value from the endpoint?
        Object v = endpoint.getProperty(SftpConnector.PROPERTY_ARCHIVE_TEMP_RECEIVING_DIR);
        if(v != null) {
        	archiveTempReceivingDir = (String)v;
        }
        
        return archiveTempReceivingDir;
	}
    
    public String getArchiveTempSendingDir() {
        String archiveTempSendingDir = connector.getArchiveTempSendingDir();

        // Override the value from the endpoint?
        Object v = endpoint.getProperty(SftpConnector.PROPERTY_ARCHIVE_TEMP_SENDING_DIR);
        if(v != null) {
        	archiveTempSendingDir = (String)v;
        }
        
        return archiveTempSendingDir;
	}
    
    public boolean isUseTempFileTimestampSuffix() {
        boolean useTempFileTimestampSuffix = connector.isUseTempFileTimestampSuffix();

        // Override the value from the endpoint?
        Object v = endpoint.getProperty(SftpConnector.PROPERTY_USE_TEMP_FILE_TIMESTAMP_SUFFIX);
        if(v != null) {
        	useTempFileTimestampSuffix = Boolean.valueOf((String)v);
        }
        
        return useTempFileTimestampSuffix;
	}
	
    public String getDuplicateHandling() {
        String duplicateHandling = connector.getDuplicateHandling();

        // Override the value from the endpoint?
        Object v = endpoint.getProperty(SftpConnector.PROPERTY_DUPLICATE_HANDLING);
        if(v != null) {
        	duplicateHandling = (String)v;
        }
        
        return duplicateHandling;
	}
    
}
