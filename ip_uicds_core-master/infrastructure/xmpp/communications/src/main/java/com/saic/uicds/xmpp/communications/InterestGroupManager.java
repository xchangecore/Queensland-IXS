package com.saic.uicds.xmpp.communications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;

import com.saic.uicds.core.infrastructure.messages.DeleteJoinedInterestGroupMessage;
import com.saic.uicds.core.infrastructure.messages.JoinedInterestGroupNotificationMessage;
import com.saic.uicds.xmpp.communications.NodeManagerImpl.NODE_ITEM_TYPE;
import com.saic.uicds.xmpp.extensions.interestgroupmgmt.InterestGrpManagementEventFactory;
import com.saic.uicds.xmpp.extensions.interestgroupmgmt.InterestGrptManagementIQFactory;
import com.saic.uicds.xmpp.extensions.pubsub.PubSubIQFactory;

public class InterestGroupManager {

    private Logger log = Logger.getLogger(this.getClass());

    public static final String productsNodeSuffix = "_WorkProducts";

    private Object processSuspendedUpdatesLock = new Object();

    public enum CORE_STATUS {
        OWNED, JOIN_IN_PROGRESS, JOINED, RESIGN_IN_PROGRESS, RESIGNED, ERROR
    };

    // key is interestGroupID
    private HashMap<String, InterestGroup> ownedInterestGroups; // mostly GuardedBy("this")

    // key is interestGroupID.RemoteCore
    private HashMap<String, InterestGroup> joinedInterestGroups; // mostly GuardedBy("this")

    // key is interestGroupID, value is list of joining core names
    private HashMap<String, List<String>> joiningCores; // GuardedBy("this")

    private ArrayList<InterestGroup> failedJoins = new ArrayList<InterestGroup>();

    private CoreConnection coreConnection;

    public void setCoreConnection(CoreConnection c) {

        coreConnection = c;
    }

    public CoreConnection getCoreConnection() {

        return coreConnection;
    }

    private InterestManager interestManager;

    public void setInterestManager(InterestManager im) {

        interestManager = im;
    }

    public InterestManager getInterestManager() {

        return interestManager;
    }

    public HashMap<String, InterestGroup> getJoinedInterestGroups() {

        return joinedInterestGroups;
    }

    public HashMap<String, InterestGroup> getOwnedInterestGroups() {

        return ownedInterestGroups;
    }

    private MessageChannel joinedPublishProductNotificationChannel;

    public MessageChannel getJoinedPublishProductNotificationChannel() {

        return joinedPublishProductNotificationChannel;
    }

    public void setJoinedPublishProductNotificationChannel(
        MessageChannel joinedPublishProductNotificationChannel) {

        this.joinedPublishProductNotificationChannel = joinedPublishProductNotificationChannel;
    }

    private MessageChannel joinedInterestGroupNotificationChannel;

    public MessageChannel getJoinedInterestGroupNotificationChannel() {

        return joinedInterestGroupNotificationChannel;
    }

    public void setJoinedInterestGroupNotificationChannel(
        MessageChannel joinedInterestGroupNotificationChannel) {

        this.joinedInterestGroupNotificationChannel = joinedInterestGroupNotificationChannel;
    }

    private MessageChannel productPublicationStatusNotificationChannel;

    public MessageChannel getProductPublicationStatusNotificationChannel() {

        return productPublicationStatusNotificationChannel;
    }

    public void setProductPublicationStatusNotificationChannel(
        MessageChannel productPublicationStatusNotificationChannel) {

        this.productPublicationStatusNotificationChannel = productPublicationStatusNotificationChannel;
    }

    private MessageChannel deleteJoinedInterestGroupNotificationChannel;

    public MessageChannel getDeleteJoinedInterestGroupNotificationChannel() {

        return deleteJoinedInterestGroupNotificationChannel;
    }

    public void setDeleteJoinedInterestGroupNotificationChannel(
        MessageChannel deleteJoinedInterestGroupNotificationChannel) {

        this.deleteJoinedInterestGroupNotificationChannel = deleteJoinedInterestGroupNotificationChannel;
    }

    private MessageChannel deleteJoinedProductNotificationChannel;

    public MessageChannel getDeleteJoinedProductNotificationChannel() {

        return deleteJoinedProductNotificationChannel;
    }

    public void setDeleteJoinedProductNotificationChannel(
        MessageChannel deleteJoinedProductNotificationChannel) {

        this.deleteJoinedProductNotificationChannel = deleteJoinedProductNotificationChannel;
    }

    public synchronized void addToFailedJoins(InterestGroup interestGroup) {

        if (!failedJoins.contains(interestGroup)) {
            failedJoins.add(interestGroup);
        }
    }

    public synchronized List<InterestGroup> getFailedJoins() {

        return Collections.unmodifiableList(failedJoins);
    }

    public synchronized void removeAllFromFailedJoins(ArrayList<InterestGroup> groupsToRemove) {

        failedJoins.removeAll(groupsToRemove);
    }

    @PostConstruct
    public void initialize() {

        log.debug("xmpp/InterestGroupManager:initialize()");
        assert (coreConnection != null);
        assert (interestManager != null);

        interestManager.addIQListener(new InterestGrpGMgmtIQListener(this),
            new IQNamespacePacketFilter(InterestGrptManagementIQFactory.namespace));

        interestManager.addMessageListener(new InterestGrpMgmtEventListener(this),
            new PacketExtensionFilter(InterestGrpManagementEventFactory.ELEMENT_NAME,
                InterestGrpManagementEventFactory.NAMESPACE));

        createInterestGroupRoot();

        // Create a map to manage the locally created interest groups
        ownedInterestGroups = new HashMap<String, InterestGroup>();

        // Create a map to manage joined interest groups
        joinedInterestGroups = new HashMap<String, InterestGroup>();

        // Map for cores in the process of getting joined <uuid, core name>
        joiningCores = new HashMap<String, List<String>>();
    }

    public Map<String, InterestGroup> getInterestGroupList() {

        return Collections.unmodifiableMap(ownedInterestGroups);
    }

    private boolean createInterestGroupRoot() {

        // See if there is a /interest group node
        log.info("createInterestGroupRoot: " + coreConnection.getInterestGroupRoot());
        boolean hasInterestGroupRoot = false;
        DiscoverItems discoItems;
        log.info("===> perform discovery for pubsubsvc: " + coreConnection.getPubSubSvc());
        discoItems = coreConnection.discoverNodeItems("");

        if (discoItems != null) {
            // Get the discovered items of the queried XMPP entity
            Iterator<DiscoverItems.Item> it = discoItems.getItems();
            // Display the items of the remote XMPP entity
            while (it.hasNext()) {

                DiscoverItems.Item item = (DiscoverItems.Item) it.next();

                if (item.getName().equals(coreConnection.getInterestGroupRoot())) {
                    hasInterestGroupRoot = true;
                }
                // String[] splits = item.getNode().split("/");
                //
                // String rootNodeName = coreConnection.getInterestGroupRoot().substring(1,
                // coreConnection.getInterestGroupRoot().length());
                // if (splits.length == 2 && splits[1].equals(rootNodeName)) {
                // hasInterestGroupRoot = true;
                // }
            }
        }

        if (!hasInterestGroupRoot) {
            log.info("Add interestGroup root collection");
            hasInterestGroupRoot = interestManager.addCollection(coreConnection.getPubSubSvc(),
                coreConnection.getInterestGroupRoot());
            if (!hasInterestGroupRoot) {
                log.error("Root collection not created");
            }
        }
        return hasInterestGroupRoot;
    }

    public String createInterestGroup(InterestGroup interestGroup) {

        if (interestGroup != null) {
            log.info("createInterestGroup: interestGroupID=" + interestGroup.interestGroupID
                + " interestGroupType=" + interestGroup.interestGroupType);

            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;
            interestGroup.ownerProps = null;
            interestGroup.state = CORE_STATUS.OWNED;
            interestGroup.suspendUpdateProcessing = false;
            interestGroup.interestGroupInfo = "";

            // add to interest group status map
            if (log.isDebugEnabled())
            	log.debug("createInterestGroup: add interest group to map");
            ownedInterestGroups.put(interestGroup.interestGroupID, interestGroup);

            // create array to store future joining cores
            if (log.isDebugEnabled())
            	log.debug("createInterestGroup: create array to store future joining cores");
            addToJoiningCoresList(interestGroup);
            

            // Add the products node
            // log.debug("createInterestGroup: addNode " + interestGroup.interestGroupNode + " to ["
            // + coreConnection.getInterestGroupRoot() + "]");
            // interestManager.addNode(coreConnection.getInterestGroupRoot(),
            // interestGroup.interestGroupNode, NODE_ITEM_TYPE.ITEM_LIST, "");
            // log.debug("createInterestGroup: addFolder " + interestGroup.interestGroupNode +
            // " to ["
            // + coreConnection.getInterestGroupRoot() + "]");

            // add a node manager for the interest groups pubsub service
            if (log.isDebugEnabled())
            	log.debug("createInterestGroup: add a node manager for the interest groups pubsub service");
            interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

            // add a root folder
            // RDW - need to figure out what to do when the return from addFolder is null
            // i.e. the creation of the node failed.
            interestManager.addFolder(interestGroup.interestGroupPubsubService,
                coreConnection.getInterestGroupRoot(), interestGroup.interestGroupNode);

            // update the subscription map - DS - disable it - don't know how the subscription map in memory is used. ?????
            //log.debug("interestManager.updateSubscriptionMap("+interestGroup.interestGroupPubsubService+")");
            //interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);
            log.debug("interestGroupID returned="+interestGroup.interestGroupID);

            return interestGroup.interestGroupID;
        } else {
            return null;
        }
    }

    private synchronized void addToJoiningCoresList(InterestGroup interestGroup) {

        joiningCores.put(interestGroup.interestGroupID, new ArrayList<String>());
    }

    private synchronized List<String> getJoiningCoresList(String interestGroupID) {

        return Collections.unmodifiableList(joiningCores.get(interestGroupID));
    }

    private synchronized void addJoiningCoreToInterestGroup(InterestGroup interestGroup,
        String joinedCore) {

        joiningCores.get(interestGroup.interestGroupID).add(joinedCore);
    }

    public synchronized void removeJoiningCoreFromInterestGroup(String interestGroupID, String core) {

        log.info("Removing " + core + " from " + interestGroupID);
        if (joiningCores.containsKey(interestGroupID)
            && joiningCores.get(interestGroupID).contains(core)) {
            joiningCores.get(interestGroupID).remove(core);
        }
    }

    public synchronized void clearJoinInProgress(String interestGroupID) {

        if (ownedInterestGroups.containsKey(interestGroupID)) {
            if (ownedInterestGroups.get(interestGroupID).state == CORE_STATUS.JOIN_IN_PROGRESS) {
                log.info("Clearing JOIN_IN_PROGRESS from " + interestGroupID);
                ownedInterestGroups.get(interestGroupID).state = CORE_STATUS.OWNED;
            }
        }

    }

    public void retryFailedJoins() {

        ArrayList<InterestGroup> successfulJoins = new ArrayList<InterestGroup>();
        List<InterestGroup> failures = getFailedJoins();
        if (failures != null && failures.size() > 0) {
            for (InterestGroup ig : failures) {
                log.info("Retrying failed join to " + ig.interestGroupID);
                try {
                    joinInterestGroup(ig, "", ig.interestGroupInfo);
                    successfulJoins.add(ig);
                } catch (XMPPException e) {
                    log.error("Error retrying join for " + ig.interestGroupID);
                }
            }
        }
        if (successfulJoins.size() > 0) {
            removeAllFromFailedJoins(successfulJoins);
        }
    }

    public void restoreOwnedInterestGroup(InterestGroup interestGroup) {

        log.info("restoreOwnedInterestGroup: interestGroupID=" + interestGroup.interestGroupID
            + " interestGroupType=" + interestGroup.interestGroupType);

        if (interestGroup != null) {
            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;
            interestGroup.ownerProps = null;
            interestGroup.suspendUpdateProcessing = false;
            interestGroup.interestGroupInfo = "";

            // Here the incident's sharing status is either Shared or None

            // add to interest group status map
            // log.debug("restoreOwnedInterestGroup: add interest group to map");
            interestGroup.state = CORE_STATUS.OWNED;

            ownedInterestGroups.put(interestGroup.interestGroupID, interestGroup);

            addToJoiningCoresList(interestGroup);

            interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

            // RDW - need to figure out what to do when the return from addFolder is null
            // i.e. the creation of the node failed.
            if (interestManager.addFolder(interestGroup.interestGroupPubsubService,
                coreConnection.getInterestGroupRoot(), interestGroup.interestGroupNode) == null) {
                log.error("cannot create node for restored owned interest group: "
                    + interestGroup.interestGroupNode);
            }

            // update the subscription map
            interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);

        }
    }

    public void restoreSharedInterestGroup(InterestGroup interestGroup, Set<String> sharedCoreList) {

        log.info("restoreSharedInterestGroup: interestGroupID=" + interestGroup.interestGroupID
            + " interestGroupType=" + interestGroup.interestGroupType);

        if (interestGroup != null) {
            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;
            interestGroup.ownerProps = null;
            interestGroup.suspendUpdateProcessing = false;
            interestGroup.interestGroupInfo = "";

            // Here the incident's sharing status is either Shared or None

            interestGroup.state = CORE_STATUS.JOIN_IN_PROGRESS;

            ownedInterestGroups.put(interestGroup.interestGroupID, interestGroup);

            addToJoiningCoresList(interestGroup);

            interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

            // RDW - need to figure out what to do when the return from addFolder is null
            // i.e. the creation of the node failed.
            interestManager.addFolder(interestGroup.interestGroupPubsubService,
                coreConnection.getInterestGroupRoot(), interestGroup.interestGroupNode);

            // update the subscription map
            interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);

            synchronized (this) {
                for (String joinedCore : sharedCoreList) {
                    List<String> joiningCoreList = getJoiningCoresList(interestGroup.interestGroupID);
                    if (joiningCoreList != null) {
                        addToJoiningCoresList(interestGroup);
                    }
                    addJoiningCoreToInterestGroup(interestGroup, joinedCore);

                    // Don't worry about attempting to share again
                    // we're only trying to re-populate internal data and re-producing events

                }
            }

        }
    }

    public void restoreJoinedInterestGroup(InterestGroup interestGroup, String ownerPropString) {

        log.info("restoreJoinedInterestGroup: interestGroupID=" + interestGroup.interestGroupID
            + " interestGroupType=" + interestGroup.interestGroupType);

        if (interestGroup != null) {
            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;
            interestGroup.ownerProps = null;
            interestGroup.suspendUpdateProcessing = false;
            interestGroup.interestGroupInfo = "";

            String wpTypeNode = "";
            boolean connectionRestoreSuccessful = true;
            try {

                // subscribe to the child nodes (specified workProductType's) at the owning
                // core

                interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

                for (String wpType : interestGroup.workProductTypes) {
                    wpTypeNode = wpType + "_" + interestGroup.interestGroupID;
                    log.debug("InterestGroupManger:restoreJoinedInterestGroup - subscribing to node:"
                        + wpTypeNode);
                    interestManager.subscribeToNode(interestGroup.interestGroupPubsubService,
                        wpTypeNode);
                }

                // Add the joined interest group
                interestGroup.state = CORE_STATUS.JOINED;
                String joinedKey = interestGroup.interestGroupID;
                joinedInterestGroups.put(joinedKey, interestGroup);
            } catch (XMPPException e) {
                log.error("restoreInterestGroup: error subscribing to ownner's node " + wpTypeNode);
                if (e.getXMPPError() != null) {
                    log.error("  message: " + e.getXMPPError().getMessage());
                    log.error("     code: " + e.getXMPPError().getCode());
                    log.error("     type: " + e.getXMPPError().getType());
                    log.error("condition: " + e.getXMPPError().getCondition());
                } else {
                    log.error("  null XMPPP error");
                }
                connectionRestoreSuccessful = false;

            } catch (Exception e) {
                log.error("restoreInterestGroup: error subscribing to owner's node " + wpTypeNode);
                e.printStackTrace();
                connectionRestoreSuccessful = false;
            }
            if (!connectionRestoreSuccessful) {
                // notify InterestGroupManagementComponent to delete the restored interest
                // group since we can't subscribe, i.e. the
                // interest group may have been deleted when we were down
                DeleteJoinedInterestGroupMessage message = new DeleteJoinedInterestGroupMessage();
                message.setInterestGroupID(interestGroup.interestGroupID);
                Message<DeleteJoinedInterestGroupMessage> notification = new GenericMessage<DeleteJoinedInterestGroupMessage>(
                    message);
                deleteJoinedInterestGroupNotificationChannel.send(notification);
            }
        }
    }

    public void deleteInterestGroup(String interestGroupID) {

        log.info("deleteInterestGroup: interestGroupID=" + interestGroupID);

        InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);
        if (interestGroup != null) {

            Set<String> joinedCoreJIDs = interestGroup.joinedCoreJIDMap.keySet();
            for (String joinedCoreJID : joinedCoreJIDs) {
                interestManager.sendDeleteJoinedInterestGroupMessage(joinedCoreJID, interestGroupID);
            }

            // delete all the work product type nodes
            for (String wpType : interestGroup.workProductTypes) {
                String wpTypeNode = wpType + "_" + interestGroupID;
                interestManager.removeNode(interestGroup.interestGroupPubsubService, wpTypeNode);
            }

            // Unsubscribe to all nodes for this interest group
            interestManager.unsubscribeAllForInterestGroup(
                interestGroup.interestGroupPubsubService, interestGroupID);

            // delete the interest group node
            interestManager.removeNode(interestGroup.interestGroupPubsubService,
                interestGroup.interestGroupNode);

            // add to interest group status map
            // log.debug("createInterestGroup: add interest group to map");
            synchronized (this) {
                ownedInterestGroups.remove(interestGroup);
            }

            // update the subscription map
            interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);

        }
    }

    public void deleteJoinedInterestGroup(String interestGroupID) {

        if (joinedInterestGroups.containsKey(interestGroupID)) {
            synchronized (this) {
                joinedInterestGroups.remove(interestGroupID);
            }
        }

        DeleteJoinedInterestGroupMessage message = new DeleteJoinedInterestGroupMessage();
        message.setInterestGroupID(interestGroupID);
        Message<DeleteJoinedInterestGroupMessage> notification = new GenericMessage<DeleteJoinedInterestGroupMessage>(
            message);
        getDeleteJoinedInterestGroupNotificationChannel().send(notification);

    }

    public void deleteWorkProduct(String wpID, String wpType, String interestGroupID) {

        log.info("deleteWorkProduct: interestGroupID=" + interestGroupID + " wpType=" + wpType
            + " wpID=" + wpID);

        InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);
        if (interestGroup != null) {

            Set<String> joinedCoreJIDs = interestGroup.joinedCoreJIDMap.keySet();
            for (String joinedCoreJID : joinedCoreJIDs) {
                interestManager.sendDeleteJoinedProductMessage(joinedCoreJID, wpID);
            }

            if (interestGroup.workProductTypes.contains(wpType)) {
                String wpTypeNode = wpType + "_" + interestGroupID;
                try {
                    if (interestManager.retrieveNodeItem(interestGroup.interestGroupPubsubService,
                        wpTypeNode, wpID) != null) {
                        interestManager.removeItem(interestGroup.interestGroupPubsubService,
                            wpTypeNode, wpID);
                    }
                } catch (IllegalStateException e) {
                    log.error("deleteWorkProduct: WorkProduct " + wpID + " does not exist: "
                        + e.getMessage());
                }
            }
        }
    }

    // public void publishInterestGroup(String interestGroupID, String interestGroupWPID,
    // String interestGroupWP, String productIDs[]) {
    // InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);
    // if (interestGroup != null) {
    // interestGroup.workProductIDs = productIDs;
    // }
    // publishWorkProduct(interestGroupID, interestGroupWPID, interestGroupWP);
    // }

    public boolean interestGroupExists(String interestGroupID) {

        InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);
        return (interestGroup != null);
    }

    public void publishWorkProduct(String interestGroupID, String wpID, String wpType, String wp) {

        log.debug("publishWorkProduct: interestGroupID=" + interestGroupID + "  wpID=" + wpID
            + " wpType=" + wpType);

        InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);

        if (interestGroup != null) {
            String wpTypeNode = wpType + "_" + interestGroupID;

            if (interestGroup.workProductTypes.contains(wpType)) {
            	if (log.isDebugEnabled())
            		log.debug("remove work product publication first. wpTypeNode="+wpTypeNode);
                // remove work product publication first if it exists in case this is an update
                try {
                    if (interestManager.retrieveNodeItem(interestGroup.interestGroupPubsubService,
                        wpTypeNode, wpID) != null) {
                        try {
                        	if (log.isDebugEnabled())
                        		log.debug("removeItem. wpID="+wpID);
                            interestManager.removeItem(interestGroup.interestGroupPubsubService,
                                wpTypeNode, wpID);
                        } catch (NoSuchElementException exception) {
                            log.error("publishWorkProduct: unexpected exception caught while attempting to remove existing product type node "
                                + wpTypeNode);
                            exception.printStackTrace();
                        } catch (IllegalStateException exception) {
                            log.error("publishWorkProduct: unexpected exception caught while attempting to remove existing product type node "
                                + wpTypeNode);
                            exception.printStackTrace();
                        }
                    } else {
                        log.debug("publishWorkProduct: Node: " + wpTypeNode + " doesn't contain "
                            + wpID);
                    }
                } catch (IllegalArgumentException e) {
                    log.error("publishWorkProduct: illegal argument to retrieveNodeItem, data will not be published to the node: "
                        + e.getMessage());
                    return;
                } catch (IllegalStateException e) {
                    log.error("publishWorkProduct: illegal state calling retrieveNodeItem, data will not be published to the node: "
                        + e.getMessage());
                    return;
                }

            } else {
                // create a new product type node
            	if (log.isDebugEnabled())
            		log.debug("publishWorkProduct: create a new product type node. wpTypeNode="+wpTypeNode);
                interestManager.addNode(interestGroup.interestGroupPubsubService,
                    interestGroup.interestGroupNode, wpTypeNode, NODE_ITEM_TYPE.ITEM_LIST, "");

                // update the subscription map
                // DS - 20130518 - Why do we need to update subscriptionMap (all) - ????
                // Disable for now
                /*
                if (log.isDebugEnabled())
                	log.debug("publichWorkProduct: update the subscription map");
                interestManager.updateSubscriptionMap(interestGroup.interestGroupPubsubService);
				*/
                // add new work product type
                if (log.isDebugEnabled())
                	log.debug("publishWorkProduct: add new work product type. wpType="+wpType);
                interestGroup.workProductTypes.add(wpType);

                // tell the join cores of the new product type if this a shareAllTypes incident
                // Note: this is needed since we no longer subscribe to the incident node (conflict
                // error), but to all product types
                // This will not be needed if the conflict error is resolved
                Set<String> joinedCoreJIDs = interestGroup.joinedCoreJIDMap.keySet();
                for (String joinedCoreJID : joinedCoreJIDs) {
                    if (interestGroup.joinedCoreJIDMap.get(joinedCoreJID) == true) {
                        // this incident was shared for all work product types with the joined core
                    	if (log.isDebugEnabled())
                    		log.debug("publishWorkProduct: sendUpdateJoinMessage. joinedCoreJID="+joinedCoreJID+
                    				", interestGroupID="+interestGroupID+", wpType="+wpType);
                        interestManager.sendUpdateJoinMessage(joinedCoreJID, interestGroupID,
                            wpType);
                    }
                }
            }

            if (log.isDebugEnabled())
            	log.debug("createItemXML. wpID="+wpID);
            String xmlItem = PubSubIQFactory.createItemXML(wp, wpID);
            // publish interest group work product to node
            if (log.isDebugEnabled())
            	//log.debug("publishWorkProduct: publish [" + xmlItem + "] to node " + wpTypeNode);
            	log.debug("publishWorkProduct: publish xmlItem to node " + wpTypeNode);

            interestManager.publishToNode(interestGroup.interestGroupPubsubService, wpTypeNode,
                xmlItem);
            if (log.isDebugEnabled())
            	log.debug("publishToNode complete");

        } else {
            log.error("Unable to find interestGroup:[" + interestGroupID + "] in map");
            // throw exception
        }
    }

    public String getWorkProduct(String interestGroupID, String wpID)
        throws IllegalArgumentException {

        InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);

        log.info("getWorkProduct for interestGroupID=" + interestGroupID + " wpID=" + wpID);

        String wp = null;
        if (interestGroup != null) {
            wp = interestManager.retrieveNodeItem(interestGroup.interestGroupPubsubService,
                interestGroup.interestGroupNode, wpID);
        } else {
            log.error("Unable to find interestGroup:[" + interestGroupID + "] in map");
            throw new IllegalArgumentException(
                "InterestGroupManager.getWorkProduct: unable to find interestGroup "
                    + interestGroupID + " in map", null);
        }
        return wp;
    }

    public void shareInterestGroup(String interestGroupID, String remoteCore,
        String interestGroupInfo, List<String> workProductTypesToShare)
        throws IllegalArgumentException, IllegalStateException {

        log.info("InterestGroupManager.shareInterestGroup - interestGroupID=" + interestGroupID
            + " with core=" + remoteCore);

        for (String workProductType : workProductTypesToShare) {
            log.info("===> type:" + workProductType);
        }

        InterestGroup interestGroup = ownedInterestGroups.get(interestGroupID);

        List<String> joiningCoreList = getJoiningCoresList(interestGroupID);

        if (interestGroup != null) {
            log.debug("remoteCore in joiningCores: " + joiningCoreList.contains(remoteCore));
            log.debug("interestGroupState: " + interestGroup.state);
        } else {
            log.error("null interestGroup");
        }

        if (interestGroup == null) {
            log.error("shareInterestGroup: Unable to find interestGroup:[" + interestGroupID
                + "] in ownedInterestGroups map");
            throw new IllegalArgumentException(
                "InterestGroupManager.shareInterestGroup: unable to find: " + interestGroupID
                    + " in map", null);
        } else if (joiningCoreList == null) {
            log.error("Unable to find joinedCores for: " + interestGroupID);
        } else if ((joiningCoreList.contains(remoteCore) == false)
            || ((interestGroup.state != CORE_STATUS.JOINED) && (interestGroup.state != CORE_STATUS.JOIN_IN_PROGRESS))) {

            if (coreConnection.isCoreOnline(remoteCore)) {

                // save the interest group info
                interestGroup.interestGroupInfo = interestGroupInfo;

                interestGroup.state = CORE_STATUS.JOIN_IN_PROGRESS;

                Boolean shareAllTypes;
                if (workProductTypesToShare.size() == 0) {
                    // tell remote core to subscribe to all types
                    shareAllTypes = true;
                    log.debug("shareInterestGroup: share all wpTypes");
                    for (String wpType : interestGroup.workProductTypes) {
                        workProductTypesToShare.add(wpType);
                    }
                } else {
                    // verify that all the specified product types have been received for the
                    // interest group
                    shareAllTypes = false;
                    for (String wpType : workProductTypesToShare) {
                        if (!interestGroup.workProductTypes.contains(wpType)) {
                            throw new IllegalArgumentException(
                                "InterestGroupManager.shareInterestGroup: unknown specified to-share work product  type : "
                                    + wpType + " for interest group " + interestGroupID, null);
                        }
                    }
                }

                // add to list of cores that are in the process of joining
                addJoiningCoreToInterestGroup(interestGroup, remoteCore);

                log.debug("===> sending join message with types to share:");
                for (String workProductType : workProductTypesToShare) {
                    log.info("===> type:" + workProductType);
                }

                // log.info("InterestGroupManager.shareInterestGroup - call sendJoinMessage");
                interestManager.sendJoinMessage(remoteCore, interestGroup, interestGroupInfo,
                    workProductTypesToShare);

                interestGroup.joinedCoreJIDMap.put(remoteCore, shareAllTypes);
                // interestGroup.joinedCoreJIDMap.put(remoteCoreJID, shareAllTypes);

            } else {
                log.error("InterestGroupManager.shareInterestGroup: " + remoteCore
                    + " is unavailable");
                throw new IllegalStateException("InterestGroupManager.shareInterestGroup: "
                    + remoteCore + " is unavailable");
            }
        }
    }

    /**
     * Join an interest group. This core cannot own the interest group.
     * 
     * @param interestGroupUUID - UUID of the interest group
     * @return a new instance of the InterestGroup class representing the interest group
     */

    // TODO: add interestGroupWPID
    // public boolean joinInterestGroup(String interestGroupID, String interestGroupName, String
    // interestGroupWPID,
    // String owner, Properties ownerConnectionProperties, String[] workProducts) {
    public boolean joinInterestGroup(InterestGroup interestGroup, String xmlPropsStr,
        String interestGroupInfo) throws XMPPException {

        log.info("InterestGroupManger:joinInterestGroup " + interestGroup.interestGroupID);

        boolean joined = false;
        // Don't join if we own it or are already joined
        // TODO: Is the core JID in the joinedKey really needed ????
        // String joinedKey = interestGroup.interestGroupID + "." + coreConnection.getJID();
        String joinedKey = interestGroup.interestGroupID;
        if (!isInterestGroupOwned(interestGroup.interestGroupID)
            && !isInterestGroupJoined(joinedKey)) {

            // Create a local representation of the InterestGroup
            // and have it create the nodes and populate the
            // initial atom entry

            interestGroup.interestGroupNode = interestGroup.interestGroupID + productsNodeSuffix;

            // RDW remove ownerProps from InterestGroup (also interestGroupAtomEntryText?)
            interestGroup.state = CORE_STATUS.JOINED;
            interestGroup.interestGroupInfo = "";
            interestGroup.suspendUpdateProcessing = false;

            // Add the joined interest group
            joinedInterestGroups.put(joinedKey, interestGroup);

            // Add a node manager for this pubsub service
            interestManager.addNodeManager(interestGroup.interestGroupPubsubService);

            // subscribe to the child nodes (specified workProductType's) at the owning core
            for (String wpType : interestGroup.workProductTypes) {
                String wpTypeNode = wpType + "_" + interestGroup.interestGroupID;
                log.debug("InterestGroupManger:joinInterestGroup - subscribing to node:"
                    + wpTypeNode);
                try {
                    interestManager.subscribeToNode(interestGroup.interestGroupPubsubService,
                        wpTypeNode);
                } catch (XMPPException e) {
                    log.error("joinInterestGroup: Error subscribing to owner's node " + wpTypeNode);
                    joinedInterestGroups.remove(joinedKey);
                    interestGroup.interestGroupInfo = interestGroupInfo;
                    addToFailedJoins(interestGroup);
                    throw e;
                } catch (Exception e) {
                    log.error("joinInterestGroup: Error subscribing to owner's node " + wpTypeNode);
                    joinedInterestGroups.remove(joinedKey);
                    interestGroup.interestGroupInfo = interestGroupInfo;
                    addToFailedJoins(interestGroup);
                    return false;
                }

            }

            // TODO: should we send some confirmation back to owning core???

            // Tell COMMS about the new interestGroup here
            log.debug("joinInterestGroup - notify Comms of joined interest group id="
                + interestGroup.interestGroupID);
            JoinedInterestGroupNotificationMessage joinedInterestGroupMessage = new JoinedInterestGroupNotificationMessage();
            joinedInterestGroupMessage.setInterestGroupID(interestGroup.interestGroupID);
            joinedInterestGroupMessage.setOwner(interestGroup.interestGroupOwner);
            joinedInterestGroupMessage.setOwnerProperties(xmlPropsStr);
            joinedInterestGroupMessage.setInterestGroupType(interestGroup.interestGroupType);
            joinedInterestGroupMessage.setInterestGroupInfo(interestGroupInfo);
            joinedInterestGroupMessage.setJoinedWPTypes(interestGroup.workProductTypes);
            Message<JoinedInterestGroupNotificationMessage> notification = new GenericMessage<JoinedInterestGroupNotificationMessage>(
                joinedInterestGroupMessage);
            joinedInterestGroupNotificationChannel.send(notification);

            joined = true;

        } else {
            if (isInterestGroupOwned(interestGroup.interestGroupID)) {
                throw new IllegalArgumentException(
                    "InterestGroupManager:joinInterestGroup trying to join "
                        + interestGroup.interestGroupID + " but this core owns it");
            } else if (isInterestGroupJoined(joinedKey)) {
                log.error("Requested to join an interest group that is already joined to.");
            }
        }
        // log.info("InterestGroupManger:joinInterestGroup END");

        return joined;
    }

    public void updateJoinInterestGroup(String interestGroupID, String productType) {

        log.info("InterestGroupManger:updateJoinInterestGroup " + interestGroupID);

        InterestGroup interestGroup = joinedInterestGroups.get(interestGroupID);
        if (interestGroup != null) {
            String wpTypeNode = productType + "_" + interestGroup.interestGroupID;
            try {
                // InterestManager ownerIM = getOwnerInterestManager(interestGroup.ownerProps);
                // ownerIM.subscribeToOwnersNode(wpTypeNode, interestGroup.ownerProps);
                // RDW
                interestManager.subscribeToNode(interestGroup.interestGroupPubsubService,
                    wpTypeNode);
            } catch (Exception e) {
                log.error("updateJoinInterestGroup: Error subscribing to owner's node "
                    + wpTypeNode);
                return;
            }
            interestGroup.workProductTypes.add(productType);
        }
    }

    public void requestJoinedPublishProduct(String interestGroupID, String owningCore,
        String productId, String productType, String act, String userID, String product) {

        interestManager.sendJoinedPublishProductRequestMessage(interestGroupID, owningCore,
            productId, productType, act, userID, product);
    }

    public void sendProductPublicationStatus(String requestingCore, String userID, String status) {

        interestManager.sendProductPublicationStatusMessage(requestingCore, userID, status);
    }

    /**
     * Returns true if this core owns the interest group with the given uuid.
     * 
     * @param uuid unique id of an interest group
     * @return true if interest group is owned by this core
     */
    public synchronized boolean isInterestGroupOwned(String uuid) {

        return (ownedInterestGroups.containsKey(uuid) && ownedInterestGroups.get(uuid) != null);
    }

    /**
     * Returns true if this core is joined to the interest group with the given uuid.
     * 
     * @param uuid uuid unique id of an interest group
     * @return true if this core is joined to the interest group
     */
    // TODO: is the core name in the joined key really needed???
    public synchronized boolean isInterestGroupJoined(String joinedkey) {

        return (joinedInterestGroups.containsKey(joinedkey) && joinedInterestGroups.get(joinedkey) != null);
    }

    public Object getProcessSuspendedUpdatesLock() {

        return processSuspendedUpdatesLock;
    }

    public InterestGroup getOwnedInterestGroup(String interestGroupID) {

        return this.ownedInterestGroups.get(interestGroupID);
    }

    public InterestGroup getJoinedInterestGroup(String interestGroupID, String coreName) {

        return this.ownedInterestGroups.get(interestGroupID + "." + coreName);
    }

    public synchronized boolean isCoreJoining(String coreName, String interestGroupID)
        throws IllegalArgumentException {

        boolean found = false;
        if (interestGroupID != null && joiningCores.containsKey(interestGroupID)) {
            if (getJoiningCoresList(interestGroupID).contains(coreName)) {
                found = true;
            }
        } else {
            throw new IllegalArgumentException(
                "InterestGroupManager:isCoreJoining unknown interest group");
        }
        return found;
    }

    /**
     * Called when a request is received from the owner of an interest group that this core should
     * resign from the interest group. This method should not be called on a core that owns the
     * interest group.
     * 
     * @param interestGroupID UUID of the interest group
     * @param to owning core
     * @param packetId id attribute of resign request message
     * @param xml XML text of original resign message
     * @throws IllegalArgumentException
     */
    public void resignFromInterestGroup(String interestGroupID, String to, String packetId,
        String xml) throws IllegalArgumentException {

        // Make sure the interest group name is not empty or null
        if (interestGroupID == null || interestGroupID.length() == 0) {
            throw new IllegalArgumentException("InterestGroup UUID cannot be empty");
        }

        InterestGroup interestGroup = joinedInterestGroups.get(interestGroupID);
        if (interestGroup == null) {
            throw new IllegalArgumentException("InterestGroup with uuid " + interestGroupID
                + " not found.");
        }

        log.info("resignFromInterestGroup resigning from interest group ID=" + interestGroupID);

        // Unsubscribe to all nodes for this interest group
        interestManager.unsubscribeAllForInterestGroup(interestGroup.interestGroupPubsubService,
            interestGroupID);

        // remove from mangement
        synchronized (this) {
            joinedInterestGroups.remove(interestGroupID);
        }

        // Let owner know that we have resigned
        // (String owningJID, String uuid)
        IQ msg = InterestGrptManagementIQFactory.createResignConfirmMessage(to, packetId, xml);
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            log.error("Error sending resign from interest group message: " + e.getMessage());
            log.debug("resignFromInterestGroup message: " + msg.toXML());
        }

    }

}
