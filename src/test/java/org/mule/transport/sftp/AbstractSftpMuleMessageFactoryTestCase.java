/*
 * $Id: AbstractFileMuleMessageFactoryTestCase.java 17177 2010-05-05 12:55:17Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.sftp;

import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.InputStream;

public abstract class AbstractSftpMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    protected File tempFile;
    private File tmpDirectory;

    protected byte[] testBytes;
    protected String testString;
    protected InputStream testInputstream;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        createWorkDirectory();
        tempFile = File.createTempFile("simple", ".mule", tmpDirectory);
        testBytes = "testing bytes".getBytes();
        testString = "testing string";
    }

    private void createWorkDirectory()
    {
        // The working directory is deleted on tearDown (see
        // AbstractMuleTestCase.disposeManager)
        tmpDirectory = FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory(), "tmp");
        if (!tmpDirectory.exists())
        {
            assertTrue(tmpDirectory.mkdirs());
        }
    }

    @Override
    protected Object getValidTransportMessage()
    {
        return testBytes;
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return new File("fooFile");
    }
}
