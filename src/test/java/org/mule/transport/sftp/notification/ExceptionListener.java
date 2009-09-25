package org.mule.transport.sftp.notification;

import org.mule.AbstractExceptionListener;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionListener extends AbstractExceptionListener {

	private static final Logger logger = LoggerFactory.getLogger(ExceptionListener.class);

	private static Throwable lifecycleException = null;
	private static Throwable messagingException = null;
	private static Throwable routingException = null;
	private static Throwable standardException = null;

	@Override
	public void handleLifecycleException(Object component, Throwable e) {
		lifecycleException = e;
		logger.debug("handleLifecycleException: " + component + ", " + e);
	}

	@Override
	public void handleMessagingException(MuleMessage message, Throwable e) {
		messagingException = e;
		logger.debug("handleMessagingException: " + message + ", " + e);
	}

	@Override
	public void handleRoutingException(MuleMessage message, ImmutableEndpoint endpoint, Throwable e) {
		routingException = e;
		logger.debug("handleRoutingException: " + message + ", " + endpoint + ", " + e);
	}

	@Override
	public void handleStandardException(Throwable e) {
		standardException = e;
		logger.debug("handleStandardException: " + e);
	}
	
	public static void reset() {
		lifecycleException = null;
		messagingException = null;
		routingException = null;
		standardException = null;
	}

	public static Throwable getLifecycleException() {
		return lifecycleException;
	}

	public static Throwable getMessagingException() {
		return messagingException;
	}

	public static Throwable getRoutingException() {
		return routingException;
	}

	public static Throwable getStandardException() {
		return standardException;
	}
}
