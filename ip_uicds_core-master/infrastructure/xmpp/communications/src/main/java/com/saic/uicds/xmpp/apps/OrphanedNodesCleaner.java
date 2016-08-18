package com.saic.uicds.xmpp.apps;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.BasicConfigurator;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.XMPPError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.saic.uicds.xmpp.communications.CommandWithReply;
import com.saic.uicds.xmpp.communications.CoreConnection;
import com.saic.uicds.xmpp.communications.InterestGroup;
import com.saic.uicds.xmpp.communications.InterestGroupManager;
import com.saic.uicds.xmpp.extensions.pubsub.PubSubIQFactory;

public class OrphanedNodesCleaner
    extends TimerTask {

    private Logger log = LoggerFactory.getLogger(OrphanedNodesCleaner.class);

    private CoreConnection coreConnection;
    private Map<String, String> roster;

    public InterestGroupManager interestGroupManager;
    private Map<String, InterestGroup> localReconciliationMap;

    private ArrayList<String> remoteReconciliationList;
    private ArrayList<String> remoteCoreList;

    public void setCoreConnection(CoreConnection conn) {

        coreConnection = conn;
    }

    public void setInterestGroupManager(InterestGroupManager igMgr) {

        interestGroupManager = igMgr;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {
            BasicConfigurator.configure();

            ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "contexts/applicationContext.xml" });

            OrphanedNodesCleaner orphanedNodesCleaner = (OrphanedNodesCleaner) context.getBean("orphanedNodesCleaner");
            orphanedNodesCleaner.cleanOrphanedNodes();

        } catch (Throwable e) {
            System.out.println("Failed to initialize OrphanedNodesCleaner from applicationContext.xml");
        }
    }

    public void cleanOrphanedNodes() {

        if (coreConnection.isConnected()) {

            try {
                // send a heartbeat to remote cores by updating our presence
                Presence presence = new Presence(Type.available, "Online", 50, Mode.available);
                coreConnection.sendPacket(presence);
            } catch (Exception e) {
                log.debug("Exception sending presence: " + e.getMessage());
            }

            // get a list of local joined interestGroups
            localReconciliationMap = interestGroupManager.getJoinedInterestGroups();

            // create a clean reconciliation list
            remoteReconciliationList = new ArrayList<String>();
            // create a clean "checked cores" liat
            remoteCoreList = new ArrayList<String>();

            try {
                roster = coreConnection.getRosterByName();
                if (roster == null) {
                    log.error("null roster returned");
                } else {
                    // for each jid in the roster, populate igReconciliation List
                    for (String entry : roster.keySet()) {
                        log.debug("Checking " + entry);
                        if (entry != null) {
                            if (!entry.equals(coreConnection.getJID())
                                && coreConnection.isCoreOnline(entry)) {
                                populateReconciliationList(entry);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Exception processing roster: " + e.getMessage());
            }

            // Reconcile
            // for each joined IG id in the local core
            try {
                int cleanedCount = 0;
                for (String id : localReconciliationMap.keySet()) {
                    // ... if it was not found on a remote core AND
                    // the remote core is in the "checked cores" list
                    log.debug("Reconciling: ");
                    log.debug("   local IG ID: " + id);
                    log.debug("   Found IG on remote core? "
                        + remoteReconciliationList.contains(id));
                    log.debug("   "
                        + ((InterestGroup) localReconciliationMap.get(id)).interestGroupOwner
                        + " is online? "
                        + remoteCoreList.contains(((InterestGroup) localReconciliationMap.get(id)).interestGroupOwner));
                    if (!remoteReconciliationList.contains(id)
                        && remoteCoreList.contains(((InterestGroup) localReconciliationMap.get(id)).interestGroupOwner)) {
                        log.info("Removing orphaned interest group: " + id);
                        interestGroupManager.deleteJoinedInterestGroup(id);
                        cleanedCount++;
                    } else {
                        log.debug("   Ignoring: " + id + "(does not appear to be orphaned)");
                    }
                }
                if (cleanedCount > 0) {
                    log.info("Cleaned " + cleanedCount + " orphaned incidents.");
                }
            } catch (Exception e) {
                log.debug("Exception reconciling incidents: " + e.getMessage());
            }

        } else {
            log.info("Connection to core not initialized. Is the core up?");
        }

    }

    private void populateReconciliationList(String jid) {

        //log.debug("start populateReconciliationList");
        // TODO: should use disco to get remote pubsub and use "pubsub.hostname" as fallback
        // IQ iq = PubSubIQFactory.retrieveSubscriptions("pubsub." +
        // jid.substring(jid.lastIndexOf("@")+1));
        IQ iq = PubSubIQFactory.discoverItems("pubsub." + jid.substring(jid.lastIndexOf("@") + 1),
            "/interestGroup");
        //log.debug("Sending: " + iq.toXML());

        CommandWithReply cmd = null;
        try {
            cmd = coreConnection.createCommandWithReply(iq);
            if (cmd == null) {
                log.error("null command returned from coreConnection");
                return;
            }
        } catch (XMPPException e) {
            log.error("Error creating command to discover items: " + e.getMessage());
            return;
        }

        int counter = 3;
        boolean success = cmd.waitForSuccessOrFailure();
        while (!success && counter > 0) {
            counter--;
            success = cmd.waitForSuccessOrFailure();
        }

        if (success) {

        } else {
            if (cmd.getErrorCode() == 0 && cmd.getErrorType() == null
                && cmd.getErrorCondition() == null) {
                log.error(cmd.getErrorMessage() + " When requesting incident nodes from "
                    + iq.getTo());
            } else {
                log.error("Error requesting incident nodes from " + iq.getTo());
                log.error("  message: " + cmd.getErrorMessage());
                log.error("     code: " + cmd.getErrorCode());
                log.error("     type: " + cmd.getErrorType());
                log.error("condition: " + cmd.getErrorCondition());
            }
            if (cmd.getResult() != null) {
                log.error(cmd.getResult().toXML());
            }
            if (cmd.getErrorType() == XMPPError.Type.AUTH) {
                log.error("   Not authorized for this action.");
            }
        }

        IQ iqResponse = cmd.getResult();
        if (iqResponse != null) {
            log.debug("Received: " + iqResponse.toXML());
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(iqResponse.toXML()));
                Document dom = builder.parse(is);

                XPathFactory xpFactory = XPathFactory.newInstance();
                XPath xpath = xpFactory.newXPath();

                XPathExpression xpError = xpath.compile("//error/remote-server-not-found");
                XPathExpression xpNodes = xpath.compile("//item/@node");

                // check for core not online
                Object result = xpError.evaluate(dom, XPathConstants.NODESET);
                NodeList nodes = (NodeList) result;
                if (nodes.getLength() > 0) {
                    log.debug("Remote core not found: " + jid);
                } else {
                    // otherwise, assume the remote core is authoritative
                    // and add it to the checked list
                    log.debug("Disco successful: " + jid);
                    remoteCoreList.add(jid);

                    // then evaluate the iq for interest groups.
                    result = xpNodes.evaluate(dom, XPathConstants.NODESET);
                    nodes = (NodeList) result;
                    if (nodes.getLength() == 0) {
                        log.debug("No nodes found on: " + jid);
                    } else {
                        for (int i = 0; i < nodes.getLength(); i++) {
                            // for each IG found, add to the list
                            if (nodes.item(i).getNodeValue().endsWith("_WorkProducts")) {
                                String igID = nodes.item(i).getNodeValue().replaceAll(
                                    "_WorkProducts", "");
                                remoteReconciliationList.add(igID);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("Exception parsing response: " + ex.getMessage());
            }
        } else {
            log.debug("Response was null.");
        }
    }

    @Override
    public void run() {

        cleanOrphanedNodes();
    }

}
