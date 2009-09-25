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

import org.apache.commons.lang.NotImplementedException;
import org.mule.transport.sftp.notification.ExceptionListener;


/**
 * Test the archive features.
 */
public class SftpDuplicateHandlingFunctionalTestCase extends AbstractSftpTestCase
{
	private static final long TIMEOUT = 10000;

	// TODO. Not very elegant solution, but the best I could figure out right now. Needs tp be improved over time...
	private boolean expectException = false;
	
	// Size of the generated stream - 2 Mb
	final static int SEND_SIZE = 1024 * 1024 * 2;

	public SftpDuplicateHandlingFunctionalTestCase() {
		// Only start mule once for all tests below, save a lot of time...
		// TODO. Runs much faster but makes test3 to fail.
		// setDisposeManagerPerSuite(true);

		// Increase the timeout of the test to 300 s
		logger.info("Timeout was set to: " + System.getProperty(PROPERTY_MULE_TEST_TIMEOUT, "-1"));
		System.setProperty(PROPERTY_MULE_TEST_TIMEOUT, "300000");
		logger.info("Timeout is now set to: " + System.getProperty(PROPERTY_MULE_TEST_TIMEOUT, "-1"));
	}

	protected String getConfigResources()
	{
		return "mule-sftp-duplicateHandling-test-config.xml";
	}
	
	@Override
	protected void doSetUp() throws Exception {
		super.doSetUp();
		ExceptionListener.reset();
		expectException = false;
	}
	

	/**
	 * Test 1 - test duplicate handling by throwing an exception 
	 */
	public void testDuplicateHandlingThrowException() throws Exception
	{
		// TODO. Add some tests specific to this test, i.e. not only rely on the tests performed by executeTest().
		
		executeBaseTest("inboundEndpoint1", "vm://test.upload1", "file1.txt", SEND_SIZE, "receiving1", TIMEOUT);
	}

	/**
	 * Test 2 - test duplicate handling by overwriting the existing file 
	 * Not yet implemented, so currently we check for a valid exception...
	 */
	public void testDuplicateHandlingOverwrite() throws Exception
	{
		// TODO. Add some tests specific to this test, i.e. not only rely on the tests performed by executeTest().
		
		expectException = true;
		assertNull(ExceptionListener.getStandardException());

		executeBaseTest("inboundEndpoint2", "vm://test.upload2", "file2.txt", SEND_SIZE, "receiving2", TIMEOUT);
		
		// Verify that a NotImplementedException exception was throwed...
		Throwable ex = ExceptionListener.getStandardException();
		assertNotNull(ex);
		assertTrue(ex instanceof NotImplementedException);
	}

	/**
	 * Test 3 - test duplicate handling by adding a sequence number to the new file 
	 */
	public void testDuplicateHandlingAddSeqNo() throws Exception
	{
		// TODO. Add some tests specific to this test, i.e. not only rely on the tests performed by executeTest().

		executeBaseTest("inboundEndpoint3", "vm://test.upload3", "file3.txt", SEND_SIZE, "receiving3", TIMEOUT);
	}

	/**
	 * To be overridden by the test-classes if required
	 */
	@Override
	protected void executeBaseAssertionsAfterCall(int sendSize, int receivedSize) {

		if (!expectException) {
			super.executeBaseAssertionsAfterCall(sendSize, receivedSize);
		}
	}


}
