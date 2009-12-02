package org.mule.transport.sftp.notification;

import org.mule.AbstractExceptionListener;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionListener extends AbstractExceptionListener {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionListener.class);

	@Override
	public void handleLifecycleException(Object component, Throwable e) {
		logger.debug("handleLifecycleException: " + component + ", " + e);
	}

	@Override
	public void handleMessagingException(MuleMessage message, Throwable e) {
		logger.debug("handleMessagingException: " + message + ", " + e);
	}

	@Override
	public void handleRoutingException(MuleMessage message, ImmutableEndpoint endpoint, Throwable e) {
		logger.debug("handleRoutingException: " + message + ", " + endpoint + ", " + e);
	}

	@Override
	public void handleStandardException(Throwable e) {
		logger.debug("handleStandardException: " + e);
	}
}
