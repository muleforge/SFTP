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

import org.mule.tck.AbstractMuleTestCase;

/**
 * JUnit test for SftpClient
 *
 * @author Lennart Häggkvist
 */
public class SftpClientTestCase extends AbstractMuleTestCase
{
	public void testGetAbsolutePath() {
		SftpClient client = new SftpClient("hostName");
		client.setHome("/home/user");

		// Assuming address="sftp://user@host/PATH" and thus the path always start with "/"
		assertEquals("hostName", client.getHost());

		// Relative paths
		assertEquals("/home/user/foo", client.getAbsolutePath("/foo"));
		assertEquals("/home/user/foo/bar", client.getAbsolutePath("/foo/bar"));

		// Absolute path
		assertEquals("/opt/mule/files", client.getAbsolutePath("//opt/mule/files"));

		// Not desired, but since the path in the address starts with / this is unspecified...
		assertEquals("/home/userfoo", client.getAbsolutePath("foo"));

		// Since the the first getAbsolutePath call removes the // the next call will asume a relative path
		assertEquals("/home/user/opt/mule/files", client.getAbsolutePath(client.getAbsolutePath("//opt/mule/files")));

	}
}
