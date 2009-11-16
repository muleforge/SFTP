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
import org.mule.transport.sftp.notification.SftpNotifier;

import java.io.InputStream;

/**
 * <code>SftpMessageReceiver</code> polls and receives files from an sftp
 * service using jsch. This receiver produces an InputStream payload, which can
 * be materialized in a MessageDispatcher or Component.
 */
public class SftpMessageReceiver extends AbstractPollingMessageReceiver
{

	private SftpReceiverRequesterUtil sftpRRUtil = null;

	public SftpMessageReceiver(SftpConnector connector, Service component,
							   InboundEndpoint endpoint, long frequency) throws CreateException
	{
		super(connector, component, endpoint);

		this.setFrequency(frequency);

		sftpRRUtil = new SftpReceiverRequesterUtil(endpoint);
	}

	public void poll() throws Exception
	{
		String[] files = sftpRRUtil.getAvailableFiles(false);

		if (files.length == 0)
		{
			logger.debug("No matching files found at endpoint " + endpoint.getEndpointURI());
		}

		for (String file : files)
		{
			routeFile(file);
		}
	}

	protected void routeFile(String path) throws Exception
	{
		// A bit tricky initialization of the notifier in this case since we don't have access to the message yet...
		SftpNotifier notifier = new SftpNotifier((SftpConnector) connector, new DefaultMuleMessage(null), endpoint, service.getName());

		InputStream inputStream = sftpRRUtil.retrieveFile(path, notifier);

		if (logger.isDebugEnabled())
		{
			logger.debug("Routing file: " + path);
		}

		MessageAdapter msgAdapter = connector.getMessageAdapter(inputStream);
		msgAdapter.setProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME, path);
		MuleMessage message = new DefaultMuleMessage(msgAdapter);

		// Now we have access to the message, update the notifier with the message
		notifier.setMessage(message);

		routeMessage(message, endpoint.isSynchronous());
	}

	public void doConnect() throws Exception
	{
		// no op
	}

	public void doDisconnect() throws Exception
	{
		// no op
	}

	protected void doDispose()
	{
		// no op
	}
}
