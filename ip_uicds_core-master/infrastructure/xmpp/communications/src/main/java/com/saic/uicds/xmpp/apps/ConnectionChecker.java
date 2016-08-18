package com.saic.uicds.xmpp.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import org.apache.log4j.BasicConfigurator;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.saic.uicds.xmpp.communications.CoreConnection;
import com.saic.uicds.xmpp.communications.InterestGroup;
import com.saic.uicds.xmpp.communications.InterestGroupManager;
import com.saic.uicds.xmpp.communications.InterestManager;
import com.saic.uicds.xmpp.communications.NodeManager;

public class ConnectionChecker
    extends TimerTask {

    private Logger log = LoggerFactory.getLogger(ConnectionChecker.class);

    private CoreConnection coreConnection;

    private InterestGroupManager interestGroupManager;

    private boolean runConnectionCheck = true;
    private boolean runSharedNodeCheck = true;
    private boolean runFailedJoinCheck = true;

    int waitCountToClearJoinInProgress = 2;

    // key = InterestGroupID value (key = core name value = count to clear join in progress)
    HashMap<String, HashMap<String, Integer>> waitCounterMap = new HashMap<String, HashMap<String, Integer>>();

    public void setCoreConnection(CoreConnection conn) {

        coreConnection = conn;
    }

    public void setInterestGroupManager(InterestGroupManager interestGroupManager) {

        this.interestGroupManager = interestGroupManager;
    }

    public int getWaitCountToClearJoinInProgress() {

        return waitCountToClearJoinInProgress;
    }

    public void setWaitCountToClearJoinInProgress(int waitCountToClearJoinInProgress) {

        this.waitCountToClearJoinInProgress = waitCountToClearJoinInProgress;
    }

    public boolean isRunConnectionCheck() {

        return runConnectionCheck;
    }

    public void setRunConnectionCheck(boolean runConnectionCheck) {

        this.runConnectionCheck = runConnectionCheck;
    }

    public boolean isRunSharedNodeCheck() {

        return runSharedNodeCheck;
    }

    public void setRunSharedNodeCheck(boolean runSharedNodeCheck) {

        this.runSharedNodeCheck = runSharedNodeCheck;
    }

    public boolean isRunFailedJoinCheck() {

        return runFailedJoinCheck;
    }

    public void setRunFailedJoinCheck(boolean runFailedJoinCheck) {

        this.runFailedJoinCheck = runFailedJoinCheck;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {
            BasicConfigurator.configure();

            ApplicationContext context = new ClassPathXmlApplicationContext(
                new String[] { "contexts/coreConnectionCheckerContext.xml" });

            ConnectionChecker connectionChecker = (ConnectionChecker) context.getBean("connectionChecker");

            if (connectionChecker != null) {

                InterestManager interestManager = (InterestManager) context.getBean("interestManager");
                if (interestManager == null) {
                    System.err.println("null interestManager");
                    System.exit(0);
                }

                InterestGroupManager interestGroupManager = (InterestGroupManager) context.getBean("interestGroupManager");
                if (interestGroupManager == null) {
                    System.err.println("null interestGroupManager");
                    System.exit(0);
                }

                DiscoverItems nodes = interestManager.getFolderContents(
                    interestGroupManager.getCoreConnection().getPubSubSvc(),
                    interestGroupManager.getCoreConnection().getInterestGroupRoot());

                Iterator<DiscoverItems.Item> iterator = nodes.getItems();
                while (iterator.hasNext()) {
                    Object n = iterator.next();
                    if (n instanceof DiscoverItems.Item) {
                        DiscoverItems.Item node = (DiscoverItems.Item) n;
                        String igID = node.getNode().replace(
                            InterestGroupManager.productsNodeSuffix, "");
                        InterestGroup ig = new InterestGroup();
                        ig.joinedCoreJIDMap = new HashMap<String, Boolean>();
                        ig.joinedCoreJIDMap.put("uicds@danzig.saic.com", Boolean.TRUE);
                        ig.interestGroupID = igID;
                        ig.interestGroupPubsubService = interestGroupManager.getCoreConnection().getPubSubSvc();
                        interestGroupManager.restoreOwnedInterestGroup(ig);
                        System.out.println("Node: " + igID);

                    } else {
                        System.err.println("Error casting to DiscoverItems.Item");
                    }

                }
                connectionChecker.checkSharedNodes();
                // connectionChecker.checkConnection();
            }

        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("Failed to initialize ConnectionChecker from applicationContext.xml");
        }
    }

    public void checkConnection() {

        try {
            log.debug("Checking core connection, connected: " + coreConnection.isConnected());
            if (coreConnection.isConnected()) {

                // Send a heartbeat presence
                coreConnection.sendHeartBeat();

                // Check that all the entries in the roster have handled subscriptions
                coreConnection.checkRoster();

            } else {
                log.error("CoreConnection is disconnected.  Attempting to reconnect ... ");
                coreConnection.connect();
                if (coreConnection.isConnected()) {
                    log.info("CoreConnection reconnection attempt was successful.  Sending presence.");
                    coreConnection.sendHeartBeat();
                } else {
                    log.error("CoreConnection reconnect attempt failed.  Will try again on next pass.");
                }
            }

        } catch (Exception e) {
            if (e instanceof XMPPException) {
                XMPPException xe = (XMPPException) e;
                log.error("XMPPException reconnecting to server: " + xe.getMessage());
                XMPPError error = xe.getXMPPError();
                logXMPPError(error);

            } else {
                log.error("Exception reconnecting to server: " + e.getMessage());
            }
        }

    }

    public void checkSharedNodes() {

        log.debug("Checking shared nodes");
        if (interestGroupManager == null) {
            log.error("null interestGroupManager");
            return;
        }

        // Get list of incidents that are owned by this core
        Map<String, InterestGroup> ownedIncidents = interestGroupManager.getInterestGroupList();

        // For each incident
        for (String igID : ownedIncidents.keySet()) {

            // if there is a new incident add it to the wait counter map
            if (!waitCounterMap.containsKey(igID)) {
                log.info("adding to wait map " + igID);
                waitCounterMap.put(igID, new HashMap<String, Integer>());
            }

            log.debug("checking " + igID);
            // Get the list of cores that are listed as shared to for this incident
            InterestGroup interestGroup = ownedIncidents.get(igID);

            Set<String> joinedCoreJIDs = null;
            if (interestGroup != null) {
                joinedCoreJIDs = interestGroup.joinedCoreJIDMap.keySet();
            } else {
                log.error("interest group was null");
                return;
            }

            // If there is a new core then add it with a new wait count
            for (String coreJID : joinedCoreJIDs) {
                if (!waitCounterMap.get(igID).containsKey(coreJID)) {
                    log.info("Adding " + coreJID + " to wait group for " + igID);
                    waitCounterMap.get(igID).put(coreJID,
                        new Integer(waitCountToClearJoinInProgress));
                }
            }

            // Get the list of nodes associated with this incident
            HashMap<String, String> workProductTypeNodeMap = new HashMap<String, String>();

            DiscoverItems nodes = interestGroupManager.getInterestManager().getFolderContents(
                coreConnection.getPubSubSvc(), igID + "_WorkProducts");
            Iterator<DiscoverItems.Item> iterator = nodes.getItems();
            log.info("Checking node subscriptions for incident: " + igID);
            while (iterator.hasNext()) {
                Object n = iterator.next();
                if (n instanceof DiscoverItems.Item) {
                    DiscoverItems.Item node = (DiscoverItems.Item) n;
                    String nodeName = node.getNode();
                    String nodeWPType = nodeName.replace("_" + igID, "");
                    // System.out.println("Adding " + nodeWPType + " node: " + nodeName);
                    log.debug("===> has node: " + nodeName + " of type " + nodeWPType);
                    workProductTypeNodeMap.put(nodeWPType, nodeName);
                } else {
                    log.error("Error casting to DiscoverItems.Item");
                }

            }

            // Find the Incident work product node
            String incidentNode = workProductTypeNodeMap.get("Incident");
            if (incidentNode != null) {
                log.debug("===> Checking incident work product node: " + incidentNode);

                // Get the list of subscriptions for this incident's Incident work product node
                NodeManager nm = interestGroupManager.getInterestManager().getNodeManager(
                    coreConnection.getPubSubSvc());

                if (nm == null) {
                    log.error("null NodeManager");
                    return;
                }

                List<String> subscribers = nm.getSubscribedJIDs(incidentNode);
                if (subscribers.isEmpty()) {
                    log.info("no subscribers");
                } else {
                    for (String subscriber : subscribers) {
                        log.info(subscriber + " is subscribed");
                    }
                }
                // If the core is not subscribed to the incident's Incident node then
                // try to ask the core to join the incident again and ignore other nodes
                // so that during the join (if successful) all other nodes will get subscribed:
                // For each core that is listed as subscribed to this incident
                ArrayList<String> jidsToRemove = new ArrayList<String>();
                if (joinedCoreJIDs.isEmpty()) {
                    log.info("no joined core JIDs");
                }
                for (String jid : joinedCoreJIDs) {

                    // If the core is not subscribed to the incident's Incident node then
                    // check that is is online and if it is then request it join the incident
                    // remove this core from the list of nodes that are listed as subscribed to the
                    // incident
                    // remove the incident's Incident node from the list of nodes associated with
                    // this
                    // incident
                    log.debug("===> Checking checking for node subscriber: " + jid);
                    if (!subscribers.contains(jid) && coreConnection.isCoreOnline(jid)
                        && joinInProgressIsCleared(igID)) {
                        log.error(jid
                            + " is listed as joined to the incident but is not subscribed to the incident pubsub node");
                        log.info("Attempting to share incident " + igID + " to " + jid);
                        interestGroupManager.shareInterestGroup(interestGroup.interestGroupID, jid,
                            interestGroup.interestGroupInfo, interestGroup.workProductTypes);
                        jidsToRemove.add(jid);
                    }
                }
                subscribers.removeAll(jidsToRemove);

                // End for each core

                // If there are any cores left
                // For each core that is listed as subscribed to this incident and is online
                if (!subscribers.isEmpty()) {

                    // Iterate through the other nodes (should not contain the Incident WP node) and
                    // verify that it is subscribed. If not request it to handle a new work product
                    // type.
                    for (String jid : subscribers) {
                        for (String wpTypeNode : workProductTypeNodeMap.keySet()) {
                            if (!wpTypeNode.equals("Incident")) {
                                log.debug("===> Checking work product type " + wpTypeNode
                                    + " for subscription by " + jid);
                                List<String> nodeSubscribers = nm.getSubscribedJIDs(incidentNode);
                                if (!nodeSubscribers.contains(jid)
                                    && coreConnection.isCoreOnline(jid)) {
                                    log.info("Asking " + jid
                                        + " to subscribe to work product type " + wpTypeNode
                                        + " for incident " + igID);
                                    interestGroupManager.getInterestManager().sendUpdateJoinMessage(
                                        jid, igID, wpTypeNode);

                                }
                            }
                        }
                    }

                } else {
                    log.debug("===> subscribers list is empty for " + igID);
                }
                // End for each core

            } else {
                log.error("No incident work product node found for " + igID);
            }

            // End for each incident
        }
    }

    public void checkFailedJoins() {

        log.debug("Checking for failed joins");
        interestGroupManager.retryFailedJoins();
    }

    /**
     * Check if we have waited long enough to see if all joins have completed. If we have then put
     * the interest group back to the OWNED state so that joins can be attempted again.
     * 
     * @param igID
     * @return
     */
    private boolean joinInProgressIsCleared(String igID) {

        log.info("checking to clear");
        boolean clear = true;
        for (String jid : waitCounterMap.get(igID).keySet()) {
            log.info("checking " + jid + " -> " + waitCounterMap.get(igID).get(jid));
            if (waitCounterMap.get(igID).get(jid) > 0) {
                clear = false;
                waitCounterMap.get(igID).put(jid, waitCounterMap.get(igID).get(jid) - 1);
            }

        }
        if (clear) {
            interestGroupManager.clearJoinInProgress(igID);
        }
        return clear;
    }

    private void logXMPPError(XMPPError error) {

        log.error("XMPP Error Message  : " + error.getMessage());
        log.error("XMPP Error Condition: " + error.getCondition());
        log.error("XMPP Error Type     : " + error.getType());
        log.error("XMPP Error Code     : " + error.getCode());
    }

    @Override
    public void run() {

        if (runConnectionCheck) {
            checkConnection();
        }

        // Check that incidents owned by this core and are listed in the Incident document
        // as shared to are subscribed to the correct nodes.
        if (runSharedNodeCheck) {
            checkSharedNodes();
        }

        // Check for failed joins
        if (runFailedJoinCheck) {
            checkFailedJoins();
        }
    }

}
