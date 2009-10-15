package org.mule.transport.sftp.dataintegrity;

import org.mule.module.client.MuleClient;

/**
 * Verify that the original file is not lost if the outbound directory doesn't exist
 */
public class SftpNoWriteAccessToOutboundDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{
	private static final String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";
	private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

	@Override
	protected String getConfigResources()
	{
		return "dataintegrity/sftp-dataintegrity-common-config.xml";
	}

	/** No write access on the outbound directory. The source file should still exist */
	public void testNoWriteAccessToOutboundDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();

		// Delete the in & outbound directories
		initEndpointDirectory(INBOUND_ENDPOINT_NAME);
		initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);

		// change the chmod to "dr-x------" on the outbound-directory
		remoteChmod(muleClient, OUTBOUND_ENDPOINT_NAME, 00500);

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME), TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(4000);

		verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, true, false);
	}


}
