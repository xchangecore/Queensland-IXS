package com.saic.uicds.xmpp.apps;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.saic.uicds.xmpp.communications.CoreConnection;

public class CleanupXMPPNodes {

    Logger logger = Logger.getLogger(CleanupXMPPNodes.class);

    CoreConnection coreConnection;

    public void setCoreConnection(CoreConnection c) {
        coreConnection = c;
    }

    CoreXMPPUtils coreXMPPUtils;

    public void setCoreXMPPUtils(CoreXMPPUtils c) {
        coreXMPPUtils = c;
    }

    /**
     * @param args
     */
    private void cleanNodes() {
        coreConnection.initialize();
        if (coreConnection.isConnected()) {
            coreXMPPUtils.deleteAllNodesRecursivly();
            logger.info("... DONE ...");
            coreConnection.disconnect();
        } else {
            logger.error("Cannot get connected to XMPP server");
        }
    }

    public static void main(String[] args) {

        BasicConfigurator.configure();

        try {
            ApplicationContext context = new ClassPathXmlApplicationContext(
                    new String[] { "contexts/applicationContext.xml" });

            CleanupXMPPNodes cleanupXMPPNodes = (CleanupXMPPNodes) context
                    .getBean("cleanupXMPPNodes");

            // // get the local host name
            // String localhost = null;
            // String fqhost = null;
            // try {
            // localhost = InetAddress.getLocalHost().getHostName();
            //        	
            // // logger.info("HostName: " + InetAddress.getLocalHost().getHostName());
            // // logger.info("HostAddress: " + InetAddress.getLocalHost().getHostAddress());
            // // logger.info("Name: " + InetAddress.getLocalHost().getCanonicalHostName());
            // } catch (UnknownHostException e) {
            // logger.error("Error getting host name: "+e.getMessage());
            // }
            //
            // // Create the properties files based on the localhost
            // Properties connectionProps = new Properties();
            // connectionProps.setProperty("debug","false");
            // connectionProps.setProperty("name", "Cleanup");
            // connectionProps.setProperty("server", localhost);
            // connectionProps.setProperty("servername", localhost);
            // connectionProps.setProperty("pubsubsvc", "pubsub." + localhost);
            // connectionProps.setProperty("port", "5222");
            // connectionProps.setProperty("username", "InterestManager");
            // connectionProps.setProperty("password", "seattle");
            // connectionProps.setProperty("resource", "cleanup");
            // connectionProps.setProperty("root", "/tester1");
            //		
            // Properties rosterProps = null;
            //		
            // // Create the CoreUtil
            // CoreUtil utils = new CoreUtil(connectionProps,rosterProps);

            // check that we were able to connect if not try the fully qualified host name
            // if (!coreUtil.isConnected()) {
            // connectionProps.setProperty("server", fqhost);
            // connectionProps.setProperty("servername", fqhost);
            // connectionProps.setProperty("pubsubsvc", "pubsub." + fqhost);
            // utils = new CoreUtil(connectionProps,rosterProps);
            // }

            cleanupXMPPNodes.cleanNodes();
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("==========> Error: Unable to initialize");
        }
    }

}
