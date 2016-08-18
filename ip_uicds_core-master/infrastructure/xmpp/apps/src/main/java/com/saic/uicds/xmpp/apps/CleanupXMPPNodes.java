package com.saic.uicds.xmpp.apps;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.saic.uicds.xmpp.communications.CoreConnection;

public class CleanupXMPPNodes {

    static Logger logger = Logger.getLogger(CleanupXMPPNodes.class);

    CoreConnection coreConnection;

    public void setCoreConnection(CoreConnection c) {

        coreConnection = c;
    }

    CoreXMPPUtils coreXMPPUtils;

    public void setCoreXMPPUtils(CoreXMPPUtils c) {

        coreXMPPUtils = c;
    }

    private void cleanNodes() {

        coreConnection.initialize();
        if (coreConnection.isConnected()) {
            logger.info("Cleaning up all XMPP nodes from " + coreConnection.getPubSubSvc());
            coreXMPPUtils.deleteAllNodesRecursivly();
            logger.info("All XMPP nodes owned by " + coreConnection.getName()
                + " have been removed from " + coreConnection.getPubSubSvc());
            coreConnection.disconnect();
        } else {
            logger.error("Cannot get connected to XMPP server");
        }
    }

    private static String getContextFile(String[] args) {

        String contextFile = null;
        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-f")) {
                i++;
                if (args[i] != null) {
                    contextFile = args[i];
                }
            }
        }
        return contextFile;
    }

    private static void printUsage() {

        System.out.println("");
        System.out.println("This tool removes all XMPP nodes on a host.");
        System.out.println("The host name is defined in the context file cleanXMPPNodesContext.xml, ");
        System.out.println("which must be supplied when the tool is inviked.");
        System.out.println("");
        System.out.println("Usage: java -jar CleanupXMPPNodes.jar -f <contextFile>");
        System.out.println("");
        System.out.println("<Options>");
        System.out.println("  -f <contextFile> XMPP application context file");
        System.out.println("Eaxmple:");
        System.out.println("       $  java -jar CleanupXMPPNodes.jar -f /cygdrive/c/contexts/cleanXMPPNodesContext.xml");
        System.out.println("Or,");
        System.out.println("       $  java -jar CleanupXMPPNodes.jar -f ../contexts/cleanXMPPNodesContext.xml");
        System.out.println("");

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {

            if (args.length == 0) {
                printUsage();
            } else {
                String contextFile = getContextFile(args);
                if (contextFile == null) {
                    logger.error("====> Error: contextFile not defined <====");
                    printUsage();
                } else {
                    logger.info("Loading context from file " + contextFile);
                    ApplicationContext context = new FileSystemXmlApplicationContext(contextFile);

                    CleanupXMPPNodes cleanupXMPPNodes = (CleanupXMPPNodes) context.getBean("cleanupXMPPNodes");

                    cleanupXMPPNodes.cleanNodes();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("==========> Error: Unable to initialize");
        }
    }

}
