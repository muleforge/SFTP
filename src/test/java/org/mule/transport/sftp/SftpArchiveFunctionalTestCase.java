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


/**
 * Test the archive features.
 */
public class SftpArchiveFunctionalTestCase extends AbstractSftpTestCase
{
	private static final long TIMEOUT = 15000;

	// Size of the generated stream - 2 Mb
	final static int SEND_SIZE = 1024 * 1024 * 2;

	public SftpArchiveFunctionalTestCase() {
		// Only start mule once for all tests below, save a lot of time...
		// TODO. Runs much faster but makes test3 to fail.
		// setDisposeManagerPerSuite(true);
	}
	
	protected String getConfigResources()
	{
		return "mule-sftp-archive-test-config.xml";
	}

	/**
	 * Test plain archive functionality with no extra features enabled 
	 */
	public void testArchive1() throws Exception
	{
		// TODO. Add some tests specific to this test, i.e. not only rely on the tests performed by executeTest().
		
		executeBaseTest("inboundEndpoint1", "vm://test.upload1", "file1.txt", SEND_SIZE, "receiving1", TIMEOUT);
	}

	/**
	 * Test archive functionality with full usage of temp-dir and creation of unique names of temp-files 
	 */
	public void testArchive2() throws Exception
	{
		// TODO. Add some tests specific to this test, i.e. not only rely on the tests performed by executeTest().

		executeBaseTest("inboundEndpoint2", "vm://test.upload2", "file2.txt", SEND_SIZE, "receiving2", TIMEOUT);
	}

	/**
	 * Test archive functionality with usage of temp-dir for inbound and outbound endpoints with creation of unique names of temp-files but not for the archive
	 */
	public void testArchive3() throws Exception
	{
		// TODO. Add some tests specific to this test, i.e. not only rely on the tests performed by executeTest().

		executeBaseTest("inboundEndpoint3", "vm://test.upload3", "file3.txt", SEND_SIZE, "receiving3", TIMEOUT);
	}

	/**
	 * Test archive functionality with usage of temp-dir for archive but not for inbound and outbound endpoints 
	 */
	public void testArchive4() throws Exception
	{
		// TODO. Add some tests specific to this test, i.e. not only rely on the tests performed by executeTest().

		executeBaseTest("inboundEndpoint4", "vm://test.upload4", "file4.txt", SEND_SIZE, "receiving4", TIMEOUT);
	}
}
