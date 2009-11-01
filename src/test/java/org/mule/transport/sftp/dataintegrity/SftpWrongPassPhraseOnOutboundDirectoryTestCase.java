package org.mule.transport.sftp.dataintegrity;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

/**
 * Verify that the original file is not lost if the password for the outbound endpoint is wrong
 */
public class SftpWrongPassPhraseOnOutboundDirectoryTestCase extends AbstractSftpDataIntegrityTestCase
{

	private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

	protected String getConfigResources()
	{
		return "dataintegrity/sftp-wrong-passphrase-config.xml";
	}

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

        // Delete the in & outbound directories
		initEndpointDirectory(INBOUND_ENDPOINT_NAME);
    }

	/**
	 * The outbound directory doesn't exist.
	 * The source file should still exist
	 * @throws Exception
	 */
	public void testWrongPassPhraseOnOutboundDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();

		// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
		muleClient.dispatch(getAddressByEndpoint(muleClient, INBOUND_ENDPOINT_NAME), TEST_MESSAGE, fileNameProperties);

		// TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
		//   for example since we dont have the TestComponent
		Thread.sleep(5000);

		SftpClient sftpClient = getSftpClient(muleClient, INBOUND_ENDPOINT_NAME);
		ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(INBOUND_ENDPOINT_NAME);
		assertTrue("The inbound file should still exist", super.verifyFileExists(sftpClient, endpoint.getEndpointURI(), FILE_NAME));
	}

}
