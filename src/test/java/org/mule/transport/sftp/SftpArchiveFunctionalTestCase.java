/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.transport.sftp;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;

/**
 * Test the archive features.
 */
public class SftpArchiveFunctionalTestCase extends AbstractSftpTestCase
{

	private static final long TIMEOUT = 15000;

	private static final String FILE1_TXT = "file1.txt";
	private static final String FILE2_TXT = "file2.txt";
	private static final String FILE3_TXT = "file3.txt";
	private static final String FILE4_TXT = "file4.txt";

	private static final String FILE2_TMP_REGEXP = "file2_.+";
	private static final String FILE3_TMP_REGEXP = "file3_.+";

	private static final String TMP_SENDING   = "tmp_sending";
	private static final String TMP_RECEIVING = "tmp_receiving";

	private static final String INBOUND_ENDPOINT1 = "inboundEndpoint1";
	private static final String INBOUND_ENDPOINT2 = "inboundEndpoint2";
	private static final String INBOUND_ENDPOINT3 = "inboundEndpoint3";
	private static final String INBOUND_ENDPOINT4 = "inboundEndpoint4";


	
	// Size of the generated stream - 2 Mb
	final static int SEND_SIZE = 1024 * 1024 * 2;
	
	private String archive = null;

	public SftpArchiveFunctionalTestCase() throws MuleException {
		
		ResourceBundle rb = ResourceBundle.getBundle("sftp-settings");
		archive = rb.getString("ARCHIVE");
		if (!archive.endsWith("/")) archive += "/";
	}

	protected String getConfigResources()
	{
		return "mule-sftp-archive-test-config.xml";
	}

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

        recursiveDeleteInLocalFilesystem(new File(archive));
        initEndpointDirectories(
            	new String[] {"receiving1", "receiving2", "receiving3", "receiving4"} , 
            	new String[] {INBOUND_ENDPOINT1, INBOUND_ENDPOINT2, INBOUND_ENDPOINT3, INBOUND_ENDPOINT4});
    }

	/**
	 * Test plain archive functionality with no extra features enabled
	 */
	public void testArchive1() throws Exception
	{
		executeBaseTest(INBOUND_ENDPOINT1, "vm://test.upload1", FILE1_TXT, SEND_SIZE, "receiving1", TIMEOUT);

		// Assert that the file now exists in the archive
		assertFilesInLocalFilesystem(archive, FILE1_TXT);

		// And that the file is gone from the inbound endpoint
		assertNoFilesInEndpoint(new MuleClient(), INBOUND_ENDPOINT1);
	}

	/**
	 * Test archive functionality with full usage of temp-dir and creation of unique names of temp-files
	 */
	public void testArchive2() throws Exception
	{
		executeBaseTest(INBOUND_ENDPOINT2, "vm://test.upload2", FILE2_TXT, SEND_SIZE, "receiving2", TIMEOUT);

		// Assert that the file now exists in the archive 
		// (with some unknown timestamp in the filename)
		// and that the tmp-archive-folders are empty
		assertFilesInLocalFilesystem(archive, new String[] {TMP_RECEIVING, TMP_SENDING, FILE2_TMP_REGEXP});
		assertNoFilesInLocalFilesystem(archive + "/" + TMP_RECEIVING);
		assertNoFilesInLocalFilesystem(archive + "/" + TMP_SENDING);

		// Assert that the file is gone from the inbound endpoint (including its tmp-folders)
		// Note that directories are not returned in this listing
		MuleClient mc = new MuleClient();
		assertNoFilesInEndpoint(mc, INBOUND_ENDPOINT2);
		assertNoFilesInEndpoint(new MuleClient(), INBOUND_ENDPOINT2, TMP_RECEIVING);
		assertNoFilesInEndpoint(new MuleClient(), INBOUND_ENDPOINT2, TMP_SENDING);
	}


	/**
	 * Test archive functionality with usage of temp-dir for inbound and outbound endpoints with creation of unique names of temp-files but not for the archive
	 */
	public void testArchive3() throws Exception
	{
		executeBaseTest(INBOUND_ENDPOINT3, "vm://test.upload3", FILE3_TXT, SEND_SIZE, "receiving3", TIMEOUT);

		// Assert that the file now exists in the archive 
		// (with some unknown timestamp in the filename)
		assertFilesInLocalFilesystem(archive, FILE3_TMP_REGEXP);
	
		// Assert that the file is gone from the inbound endpoint (including its tmp-folders)
		// Note that directories are not returned in this listing
		MuleClient mc = new MuleClient();
		assertNoFilesInEndpoint(mc, INBOUND_ENDPOINT3);
		assertNoFilesInEndpoint(new MuleClient(), INBOUND_ENDPOINT3, TMP_RECEIVING);
		assertNoFilesInEndpoint(new MuleClient(), INBOUND_ENDPOINT3, TMP_SENDING);
	}

	/**
	 * Test archive functionality with usage of temp-dir for archive but not for inbound and outbound endpoints
	 */
	public void testArchive4() throws Exception
	{
		executeBaseTest(INBOUND_ENDPOINT4, "vm://test.upload4", FILE4_TXT, SEND_SIZE, "receiving4", TIMEOUT);

		// Assert that the file now exists in the archive 
		// and that the tmp-archive-folders are empty
		assertFilesInLocalFilesystem(archive, new String[] {TMP_RECEIVING, TMP_SENDING, FILE4_TXT});
		assertNoFilesInLocalFilesystem(archive + "/" + TMP_RECEIVING);
		assertNoFilesInLocalFilesystem(archive + "/" + TMP_SENDING);		
		
		// Assert that the file is gone from the inbound endpoint
		MuleClient mc = new MuleClient();
		assertNoFilesInEndpoint(mc, INBOUND_ENDPOINT4);
	}
	
	/**
	 * Test error handling with plain archive functionality with no extra features enabled
	 */
	public void testCantWriteToArchive1() throws Exception
	{
		makeArchiveReadOnly();
		try {
			executeBaseTest(INBOUND_ENDPOINT1, "vm://test.upload1", FILE1_TXT, SEND_SIZE, "receiving1", TIMEOUT, "sftp");
			fail("Expected error");
		} catch (Exception e) {
	        assertNotNull(e);
	        assertTrue(e instanceof IOException);
	        assertEquals("Destination folder is not writeable: " + archive, e.getMessage() + File.separatorChar);
		}
		
		// Assert that file still exists in the inbound endpoint after the failure
		assertFilesInEndpoint(new MuleClient(), INBOUND_ENDPOINT1, FILE1_TXT);

		// Assert that no files exists in the archive after the error
		assertNoFilesInLocalFilesystem(archive);
	}

	/**
	 * Test error handling with archive functionality with full usage of temp-dir and creation of unique names of temp-files
	 */
	public void testCantWriteToArchive2() throws Exception
	{
		makeArchiveReadOnly();
		try {
			executeBaseTest(INBOUND_ENDPOINT2, "vm://test.upload2", FILE2_TXT, SEND_SIZE, "receiving2", TIMEOUT, "sftp");
			fail("Expected error");
		} catch (Exception e) {
	        assertNotNull(e);
	        assertTrue(e instanceof IOException);
	        assertTrue(e.getMessage().startsWith("Failed to create archive-tmp-receiving-folder: " + archive + TMP_RECEIVING));
		}

		// Assert that the file still exists in the inbound endpoint's tmp-folder after the failure
		// (with some unknown timestamp in the filename)
		// Note that directories are not returned in this listing
		MuleClient mc = new MuleClient();
		assertNoFilesInEndpoint(mc, INBOUND_ENDPOINT2);
		assertNoFilesInEndpoint(mc, INBOUND_ENDPOINT2, TMP_RECEIVING);
		assertFilesInEndpoint(mc, INBOUND_ENDPOINT2, TMP_SENDING, FILE2_TMP_REGEXP);

		// Assert that no files exists in the archive after the error
		assertNoFilesInLocalFilesystem(archive);
	}

	/**
	 * Test error handling with archive functionality with usage of temp-dir for inbound and outbound endpoints with creation of unique names of temp-files but not for the archive
	 */
	public void testCantWriteToArchive3() throws Exception
	{
		makeArchiveReadOnly();
		try {
			executeBaseTest(INBOUND_ENDPOINT3, "vm://test.upload3", FILE3_TXT, SEND_SIZE, "receiving3", TIMEOUT, "sftp");
			fail("Expected error");
		} catch (Exception e) {
	        assertNotNull(e);
	        assertTrue(e instanceof IOException);
	        assertEquals("Destination folder is not writeable: " + archive, e.getMessage() + File.separatorChar);
		}

		// Assert that the file still exists in the inbound endpoint's tmp-folder after the failure
		// (with some unknown timestamp in the filename)
		// Note that directories are not returned in this listing
		MuleClient mc = new MuleClient();
		assertNoFilesInEndpoint(mc, INBOUND_ENDPOINT3);
		assertNoFilesInEndpoint(mc, INBOUND_ENDPOINT3, TMP_RECEIVING);
		assertFilesInEndpoint(mc, INBOUND_ENDPOINT3, TMP_SENDING, FILE3_TMP_REGEXP);

		// Assert that no files exists in the archive after the error
		assertNoFilesInLocalFilesystem(archive);
	}

	/**
	 * Test error handling with archive functionality with usage of temp-dir for archive but not for inbound and outbound endpoints
	 */
	public void testCantWriteToArchive4() throws Exception
	{
		makeArchiveReadOnly();
		try {
			executeBaseTest(INBOUND_ENDPOINT4, "vm://test.upload4", FILE4_TXT, SEND_SIZE, "receiving4", TIMEOUT, "sftp");
			fail("Expected error");
		} catch (Exception e) {
	        assertNotNull(e);
	        assertTrue(e instanceof IOException);
	        assertTrue(e.getMessage().startsWith("Failed to create archive-tmp-receiving-folder: " + archive + TMP_RECEIVING));
		}

		// Assert that file still exists in the inbound endpoint after the failure
		assertFilesInEndpoint(new MuleClient(), INBOUND_ENDPOINT4, FILE4_TXT);

		// Assert that no files exists in the archive after the error
		assertNoFilesInLocalFilesystem(archive);
	}

	private void makeArchiveReadOnly() throws IOException {
		File archiveFolder = new File(archive);
		if (!archiveFolder.mkdirs()) throw new IOException("Failed to create archive-folder: " + archive);
		if (!archiveFolder.setWritable(false)) throw new IOException("Failed to make archive-folder readonly: " + archive);
	}
}