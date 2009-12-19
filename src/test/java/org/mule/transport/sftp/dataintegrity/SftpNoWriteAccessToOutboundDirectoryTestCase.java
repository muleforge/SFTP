package org.mule.transport.sftp.dataintegrity;

import java.io.IOException;

import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

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

        @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

        // Delete the in & outbound directories
		initEndpointDirectory(INBOUND_ENDPOINT_NAME);
		initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);
    }

	/** No write access on the outbound directory. The source file should still exist */
	public void testNoWriteAccessToOutboundDirectory() throws Exception
	{
		MuleClient muleClient = new MuleClient();

        SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);

        try {
        	// change the chmod to "dr-x------" on the outbound-directory
            remoteChmod(muleClient, sftpClient, OUTBOUND_ENDPOINT_NAME, 00500);

            // Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
      	    Exception exception = dispatchAndWaitForException(new DispatchParameters(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME), "sftp");
            assertNotNull(exception);
            assertTrue(exception instanceof IOException);
            assertEquals("Permission denied", exception.getMessage());

            verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, true, false);
        } finally {
            sftpClient.disconnect();
        }
    }


}
