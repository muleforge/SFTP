/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.sftp;

import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

/**
 * Creates a SftpMessageDispatcher
 */
public class SftpMessageDispatcherFactory implements UMOMessageDispatcherFactory
{

    public UMOMessageDispatcher create(UMOImmutableEndpoint umoImmutableEndpoint) throws UMOException
    {
        return new SftpMessageDispatcher(umoImmutableEndpoint);
    }

}
