/*
 * $Id: NamespaceHandler.vm 10787 2008-02-12 18:51:50Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.config;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.transport.sftp.SftpConnector;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling <code><sftp:connector></code> elements.
 *
 */
public class SftpNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("connector", new OrphanDefinitionParser(SftpConnector.class, true));
    }
}