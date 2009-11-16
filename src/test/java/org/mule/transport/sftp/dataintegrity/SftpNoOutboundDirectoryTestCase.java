package org.mule.transport.sftp.dataintegrity;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

/**
 * Verify that the original file is not lost if the outbound directory doesn't exist
 */
public class SftpNoOutboundDirectoryTestCase extends AbstractSftpDataIntegrityTestCase {

    private static final String ENDPOINT_NAME = "inboundEndpoint";

    @Override
    protected String getConfigResources() {
        return "dataintegrity/sftp-no-outbound-directory-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

        // Delete the in & outbound directories
		initEndpointDirectory(ENDPOINT_NAME);
    }

    /**
     * The outbound directory doesn't exist.
     * The source file should still exist
     */
    public void testNoOutboundDirectory() throws Exception {
        MuleClient muleClient = new MuleClient();

        // Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
        muleClient.dispatch(getAddressByEndpoint(muleClient, ENDPOINT_NAME), TEST_MESSAGE, fileNameProperties);

        // TODO dont know any better way to wait for the above to finish? We cant use the same as SftpFileAgeFunctionalTestCase
        //   for example since we dont have the TestComponent
        Thread.sleep(2000);

        SftpClient sftpClient = getSftpClient(muleClient, ENDPOINT_NAME);
        try {
            ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(ENDPOINT_NAME);
            assertTrue("The inbound file should still exist", super.verifyFileExists(sftpClient, endpoint.getEndpointURI(), FILE_NAME));
        } finally {
            sftpClient.disconnect();
        }
    }


}
