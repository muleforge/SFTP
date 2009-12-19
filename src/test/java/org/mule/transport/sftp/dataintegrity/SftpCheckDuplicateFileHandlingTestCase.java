package org.mule.transport.sftp.dataintegrity;

import java.io.IOException;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.module.client.MuleClient;
import org.mule.transport.sftp.SftpClient;

/**
 * Test the three different types of handling when duplicate files (i.e. file names) are being transferred by SftpTransport. 
 * Available duplicate handling types are:
 * 
 * - SftpConnector.PROPERTY_DUPLICATE_HANDLING_THROW_EXCEPTION = "throwException"
 * - SftpConnectorPROPERTY_DUPLICATE_HANDLING_OVERWRITE = "overwrite" (currently not implemented)
 * - SftpConnector.PROPERTY_DUPLICATE_HANDLING_ASS_SEQ_NO = "addSeqNo"
 * 
 */
public class SftpCheckDuplicateFileHandlingTestCase extends AbstractSftpDataIntegrityTestCase {

  private static String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

  private static String OUTBOUND_ENDPOINT_NAME = "outboundEndpoint";

  private static String INBOUND_ENDPOINT_NAME2 = "inboundEndpoint2";

  private static String OUTBOUND_ENDPOINT_NAME2 = "outboundEndpoint2";

  @Override
  protected String getConfigResources() {
    return "dataintegrity/sftp-dataintegrity-duplicate-handling.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    Service s1 = muleContext.getRegistry().lookupService("serviceDuplicateHandlingRename");
    Service s2 = muleContext.getRegistry().lookupService("serviceDuplicateHandlingThrowException");
    s1.stop();
    s2.stop();
    
    // Set up the in & outbound directories
    initEndpointDirectory(INBOUND_ENDPOINT_NAME);
    initEndpointDirectory(OUTBOUND_ENDPOINT_NAME);

    // Set up the in & outbound directories
    initEndpointDirectory(INBOUND_ENDPOINT_NAME2);
    initEndpointDirectory(OUTBOUND_ENDPOINT_NAME2);

    s1.start();
    s2.start();
  }

	/**
	 * Try to transfer two files with the same name. The second file will be given a new name.
	 */
	public void testDuplicateChangeNameHandling() throws Exception {

		MuleClient muleClient = new MuleClient();
		SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME);

		try {

			// Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
			dispatchAndWaitForDelivery(new DispatchParameters(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME));

			// Make sure the file exists only in the outbound endpoint
			verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME, false, true);

			// Transfer the second file
			dispatchAndWaitForDelivery(new DispatchParameters(INBOUND_ENDPOINT_NAME, OUTBOUND_ENDPOINT_NAME));

			// Make sure a file still exists only in the outbound endpoint
			verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME,OUTBOUND_ENDPOINT_NAME, false, true);

			// Make sure a new file with name according to the notation is created
			ImmutableEndpoint endpoint = (ImmutableEndpoint) muleClient.getProperty(OUTBOUND_ENDPOINT_NAME);
			assertTrue("A new file in the outbound endpoint should exist", verifyFileExists(sftpClient, endpoint.getEndpointURI().getPath(), "file_1.txt"));

		} finally {
			sftpClient.disconnect();
		}
	}

  /**
   * Test transferring a duplicate file. The default handling of duplicates is to throw an exception.
   */
  public void testDuplicateDefaultExceptionHandling() throws Exception {

	MuleClient muleClient = new MuleClient();
    SftpClient sftpClient = getSftpClient(muleClient, OUTBOUND_ENDPOINT_NAME2);

    try {

      // Send an file to the SFTP server, which the inbound-outboundEndpoint then can pick up
	  dispatchAndWaitForDelivery(new DispatchParameters(INBOUND_ENDPOINT_NAME2, OUTBOUND_ENDPOINT_NAME2));

      verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME2, OUTBOUND_ENDPOINT_NAME2, false, true);

  	  Exception exception = dispatchAndWaitForException(new DispatchParameters(INBOUND_ENDPOINT_NAME2, OUTBOUND_ENDPOINT_NAME2), "sftp");
      assertNotNull(exception);
      assertTrue(exception instanceof IOException);
      assertEquals("Failure", exception.getMessage());

      verifyInAndOutFiles(muleClient, INBOUND_ENDPOINT_NAME2, OUTBOUND_ENDPOINT_NAME2, true, true);

    } finally {
    	sftpClient.disconnect();
    }
  }
}
