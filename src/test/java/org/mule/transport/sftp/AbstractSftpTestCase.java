package org.mule.transport.sftp;

import org.mule.tck.FunctionalTestCase;


/**
 * TODO
 * User: lh
 * Date: Jun 8, 2009
 */

public abstract class AbstractSftpTestCase extends FunctionalTestCase {

  /**
   * Deletes all files in the directory
   * TODO: how about recursive mode?
   */
  protected void cleanupFtpDirectory() {
//    EndpointURI endpointURI = null;
//    SftpClient client = new SftpClient();
//    try {
//      endpointURI = new MuleEndpointURI("sftp://user1:passwd1@localhost/data2");
//      client.connect(endpointURI.getHost());
//      assertTrue("Login failed", client.login(endpointURI.getUser(), endpointURI.getPassword()));
//      client.changeWorkingDirectory(endpointURI.getPath());
//      String[] files = client.listFiles();
//      for (String file : files) {
//        client.deleteFile(file);
//      }
//    } catch (IOException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    } catch (EndpointException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    }
  }
}
