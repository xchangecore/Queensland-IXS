package com.saic.uicds.xmpp.communications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;

import com.saic.uicds.core.infrastructure.messages.PublishProductMessage;
import com.saic.uicds.xmpp.communications.NodeManagerImpl.NODE_ITEM_TYPE;
import com.saic.uicds.xmpp.extensions.interestgroupmgmt.InterestGrptManagementIQFactory;
import com.saic.uicds.xmpp.extensions.util.PubSubEventExtension;

/**
 * This class provides an interface to the XMPP storage of interest group related information <br />
 * <b>Todo:</b>
 * <ul>
 * <li>documenation - continue documenting</li>
 * </ul>
 * 
 * <pre>
 * InterestManagement interestManager = new InterestMangement();
 * 
 * // establish connection to server and login
 * interestManager.connect(&quot;username&quot;, &quot;password&quot;);
 * 
 * // get the list of current interest groups
 * 
 * </pre>
 * 
 */

public class InterestManagerImpl
    implements InterestManager {

    private Logger logger = Logger.getLogger(this.getClass());

    // Class to handle the pubsub event messages that are notfications of changes in
    // work products.
    protected class ListenerAdapter
        implements PacketListener {

        public ListenerAdapter() {

        }

        public void processPacket(Packet packet) {

            Pattern retractPattern = Pattern.compile("retract\\s+id=[\"']([\\w-]+)[\"']");

            // TODO: this may not be the right place for this logic
            // logger.debug("InterestManager:ListenerAdapter:processPacket: "+packet.toXML());
            PacketExtension ext = packet.getExtension("http://jabber.org/protocol/pubsub#event");
            if (ext != null && ext instanceof PubSubEventExtension) {
                // logger.debug("    GOT a PubSubEventExtension");
                PubSubEventExtension pubsub = (PubSubEventExtension) ext;

                // logger.debug("   pubsub: "+pubsub.toXML());
                // Handle item updates
                Iterator<com.saic.uicds.xmpp.extensions.util.Item> it = pubsub.getItems();
                while (it.hasNext()) {
                    String item = it.next().toXML();
                    // logger.debug("    ITEM: " + item);

                    // send out the node data to the Communications Service
                    PublishProductMessage message = new PublishProductMessage(item,
                        coreConnection.getServer());
                    org.springframework.integration.core.Message<PublishProductMessage> notification = (org.springframework.integration.core.Message<PublishProductMessage>) new GenericMessage<PublishProductMessage>(
                        message);
                    if (owningCoreWorkProductNotificationChannel != null) {
                        logger.info("********** Sending product publication to CommunicationsService");
                        owningCoreWorkProductNotificationChannel.send(notification);
                    } else {
                        logger.error("owningCoreWorkProductNotificationChannel is null");
                    }
                }

                // Handle item retracts
                it = pubsub.getRetracts();
                while (it.hasNext()) {
                    String xml = it.next().toXML();
                    Matcher m = retractPattern.matcher(xml);
                    if (!m.find()) {
                        logger.error("Retract for unknown item: " + xml);
                    }
                }

                // Handle node deletes
                it = pubsub.getDeletes();
                // TODO: should we delete nodes on resign or allow joined cores to keep data?
                while (it.hasNext()) {
                    // TODO: Do not always get the right <delete node> messages ... need more work
                    // here
                    // In the mean time, we will use implicit delete IQ to communicate node
                    // deletions
                    logger.debug("===> NODE DELETE: " + it.next().toXML());
                }
            }
        }
    }

    private CoreConnection coreConnection;

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#getCoreConnection()
     */
    public CoreConnection getCoreConnection() {

        return coreConnection;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#setCoreConnection(com.saic.uicds.xmpp.communications.CoreConnection)
     */
    public void setCoreConnection(CoreConnection coreConnection) {

        this.coreConnection = coreConnection;
    }

    // key = pubsub service name for a particular XMPP server
    private HashMap<String, NodeManager> nodeManagers;

    private ListenerAdapter listenerAdapter;

    private MessageChannel owningCoreWorkProductNotificationChannel;

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#getOwningCoreWorkProductNotificationChannel()
     */
    public MessageChannel getOwningCoreWorkProductNotificationChannel() {

        return owningCoreWorkProductNotificationChannel;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#setOwningCoreWorkProductNotificationChannel(org.springframework.integration.core.MessageChannel)
     */
    public void setOwningCoreWorkProductNotificationChannel(
        MessageChannel owningCoreWorkProductNotificationChannel) {

        this.owningCoreWorkProductNotificationChannel = owningCoreWorkProductNotificationChannel;
    }

    /**
     * Default constructor, must call initialize before using this object
     */
    public InterestManagerImpl() {

        listenerAdapter = new ListenerAdapter();
        nodeManagers = new HashMap<String, NodeManager>();
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#initialize()
     */
    @PostConstruct
    public void initialize() {

        logger.debug("Initialize called - coreConnection is initialized? " + coreConnection);
        if (!coreConnection.isConnected()) {
            coreConnection.initialize();
        }

        // create a node manager for the main XMPP connection
        if (nodeManagers.isEmpty()) {
            addNodeManager(coreConnection.getPubSubSvc());
        }

        assert (coreConnection != null);

        logger.debug("initialized done");
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#cleanup()
     */
    @PreDestroy
    public void cleanup() {

    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#getNodeManager(java.lang.String)
     */
    public NodeManager getNodeManager(String pubsubService) {

        NodeManager nodeManager = null;
        if (pubsubService != null) {
            if (nodeManagers.containsKey(pubsubService)) {
                nodeManager = nodeManagers.get(pubsubService);
            }
        }
        return nodeManager;
    }

    @Override
    public void addNodeManager(String pubsubService) {

        if (getNodeManager(pubsubService) == null) {
            // RDW should factory this for testing
            NodeManager nodeManager = new NodeManagerImpl(coreConnection);
            nodeManager.setPubsubService(pubsubService);
            nodeManagers.put(pubsubService, nodeManager);
        }
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#setNodeManager(com.saic.uicds.xmpp.communications.NodeManager)
     */
    public void setNodeManager(NodeManager nodeManager) {

        nodeManagers.put(nodeManager.getPubsubService(), nodeManager);
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#isInitialized()
     */
    public boolean isInitialized() {

        return (coreConnection != null);
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#subscribeToNode(java.lang.String)
     */
    public List<String> subscribeToNode(String pubsubService, String node) throws XMPPException {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).subscribeToNode(node, listenerAdapter);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return null;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#publishToNode(java.lang.String, java.lang.String)
     */
    @Override
    public boolean publishToNode(String pubsubService, String nodeName, String itemText) {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).publishToNode(nodeName, itemText);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#removeItem(java.lang.String, java.lang.String)
     */
    @Override
    public boolean removeItem(String pubsubService, String nodeName, String itemUUID) {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).removeItem(nodeName, itemUUID);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#retrieveNodeItem(java.lang.String, java.lang.String)
     */
    public String retrieveNodeItem(String pubsubService, String node, String wpID)
        throws IllegalStateException, IllegalArgumentException {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).retrieveNodeItem(node, wpID);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return null;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#getAllNodeItems(java.lang.String)
     */
    public ArrayList<String> getAllNodeItems(String pubsubService, String node)
        throws IllegalArgumentException {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).getAllNodeItems(node);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return new ArrayList<String>();
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#updateSubscriptionMap()
     */
    public void updateSubscriptionMap(String pubsubService) {

    	if (logger.isDebugEnabled())
    		logger.debug("updateSubscriptionMap: pubsubService="+pubsubService);
        if (nodeManagers.containsKey(pubsubService)) {
            nodeManagers.get(pubsubService).updateSubscriptionMap();
        } else {
            logger.error("No node manager for pubsub service: " + pubsubService);
        }
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#unsubscribeAll()
     */
    public void unsubscribeAll(String pubsubService) {

        if (nodeManagers.containsKey(pubsubService)) {
            nodeManagers.get(pubsubService).unsubscribeAll();
        } else {
            logger.error("No node manager for pubsub service: " + pubsubService);
        }
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#unsubscribeAll(java.lang.String)
     */
    public void unsubscribeAllForInterestGroup(String pubsubService, String uuid) {

        if (nodeManagers.containsKey(pubsubService)) {
            nodeManagers.get(pubsubService).unsubscribeAll(uuid);
        } else {
            logger.error("No node manager for pubsub service: " + pubsubService);
        }
    }

    // /* (non-Javadoc)
    // * @see com.saic.uicds.xmpp.communications.InterestManager#unsubscribe(java.lang.String)
    // */
    // private void unsubscribe(String node) {
    // nodeSubscriptionManager.unsubscribe(node);
    // }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#addMessageListener(org.jivesoftware.smack.PacketListener, org.jivesoftware.smack.filter.PacketFilter)
     */
    public void addMessageListener(PacketListener listener, PacketFilter filter) {

        PacketTypeFilter msgFilter = new PacketTypeFilter(Message.class);
        AndFilter andFilter = new AndFilter(msgFilter, filter);

        coreConnection.addPacketListener(listener, andFilter);
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#addIQListener(org.jivesoftware.smack.PacketListener, org.jivesoftware.smack.filter.PacketFilter)
     */
    public void addIQListener(PacketListener listener, PacketFilter filter) {

        PacketTypeFilter iqFilter = new PacketTypeFilter(IQ.class);
        AndFilter andFilter = new AndFilter(iqFilter, filter);

        coreConnection.addPacketListener(listener, andFilter);
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#refreshSubscriptions()
     */
    public void refreshSubscriptions(String pubsubService) {

        if (nodeManagers.containsKey(pubsubService)) {
            nodeManagers.get(pubsubService).updateSubscriptionMap();
        } else {
            logger.error("No node manager for pubsub service: " + pubsubService);
        }
    }

    public void sendProductPublicationStatusMessage(String requestingCore, String userID,
        String status) {

        logger.info("sendProductPublicationStatusMessage: requestingCore=" + requestingCore
            + " userID:" + userID + " status=[" + status + "]");

        String requestingCoreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(requestingCore);

        StringBuffer params = new StringBuffer();
        params.append(" userID='" + userID + "'");
        params.append(" owningCore='" + coreConnection.getCoreNameFromJID(getOwnJid()) + "'>");

        StringBuffer statustEntry = new StringBuffer();
        statustEntry.append("<ProductPublicationStatus>");
        statustEntry.append(status);
        statustEntry.append("</ProductPublicationStatus>");

        IQ msg = InterestGrptManagementIQFactory.createProductPublicationStatusMessage(
            requestingCoreJIDPlusResource, params.toString(), statustEntry.toString());

        // logger.debug(msg.toXML());

        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending product publication status message: " + e.getMessage());
            logger.debug("sendProductPublicationStatusMessage: " + msg.toXML());
        }
    }

    public void sendJoinedPublishProductRequestMessage(String interestGroupId, String owningCore,
        String productId, String productType, String act, String userID, String product) {

        logger.info("sendJoinedPublishProductRequestMessage: interestGroupID=" + interestGroupId
            + " owningCore:" + owningCore);

        String owningCoreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(owningCore);

        // TODO: Need the other stuff, e.g. name, description, lat/long ???
        StringBuffer params = new StringBuffer();
        params.append(" interestGroupId='" + interestGroupId + "'");
        params.append(" productId='" + productId + "'");
        params.append(" productType='" + productType + "'");
        params.append(" act='" + act + "'");
        params.append(" userID='" + userID + "'");
        params.append(" owningCore='" + owningCore + "'");
        params.append(" requestingCore='" + coreConnection.getCoreNameFromJID(getOwnJid()) + "'>");

        HashMap<String, String> config = new HashMap<String, String>();

        StringBuffer productEntry = new StringBuffer();
        productEntry.append("<ProductPayload>");
        productEntry.append(product);
        productEntry.append("</ProductPayload>");

        IQ msg = InterestGrptManagementIQFactory.createJoinedPublishProductRequestMessage(
            owningCoreJIDPlusResource, params.toString(), config, productEntry.toString());

        // logger.debug(msg.toXML());

        // Fire off the join request
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending join published product request message: " + e.getMessage());
            logger.debug("sendJoinedPublishProductRequestMessage: " + msg.toXML());
        }
    }

    public void sendJoinMessage(String coreJID, InterestGroup interestGroup,
        String interestGroupInfo, List<String> workProductTypesToShare) {

        logger.info("InterestManagement:sendJoinMessage to " + coreJID + " with interestGroupID="
            + interestGroup.interestGroupID);
        HashMap<String, String> config = new HashMap<String, String>();

        StringBuffer sb = new StringBuffer();

        sb.append("<workProductTypesToShare>");
        for (String workProductType : workProductTypesToShare) {
            sb.append("<item>");
            sb.append(workProductType);
            sb.append("</item>");
        }
        sb.append("</workProductTypesToShare>");

        sb.append("<info>");
        sb.append(interestGroupInfo);
        sb.append("</info>");

        StringBuffer interestGroupInfoBuffer = new StringBuffer();
        interestGroupInfoBuffer.append(" uuid='" + interestGroup.interestGroupID + "'");
        interestGroupInfoBuffer.append(" interestGroupType='" + interestGroup.interestGroupType
            + "'");
        interestGroupInfoBuffer.append(" owner='" + getOwnJid() + "'");

        String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);

        IQ msg = InterestGrptManagementIQFactory.createJoinMessage(coreJIDPlusResource,
            interestGroupInfoBuffer.toString(), config, sb.toString());

        // logger.debug(msg.toXML());

        // Fire off the join request
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending join message: " + e.getMessage());
            logger.debug("sendJoinMessage: " + msg.toXML());
        }
    }

    public void sendResignMessage(String coreJID, String interestGroupID, String interestGroupName) {

        logger.info("InterestManagement:sendResignMessage");
        IQ msg = InterestGrptManagementIQFactory.createResignMessage(coreJID, interestGroupID,
            interestGroupName, getOwnJid());

        // Fire off the resign message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending resign message: " + e.getMessage());
            logger.debug("sendResignMessage: " + msg.toXML());
        }

    }

    public void sendDeleteJoinedInterestGroupMessage(String coreJID, String interestGroupID) {

        logger.info("InterestManagement:sendDeleteJoinedInterestGroupMessage");
        String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        IQ msg = InterestGrptManagementIQFactory.createDeleteJoinedInterestGroupMessage(
            coreJIDPlusResource, interestGroupID);

        logger.debug(msg.toXML());

        // Fire off the delete interest group message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending delete joined interest group message: " + e.getMessage());
            logger.debug("sendDeleteJoinedInterestGroupMessage: " + msg.toXML());
        }

    }

    public void sendDeleteJoinedProductMessage(String coreJID, String productID) {

        logger.info("InterestManagement:sendDeleteJoinedProductMessage");
        String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        IQ msg = InterestGrptManagementIQFactory.createDeleteJoinedProductMessage(
            coreJIDPlusResource, productID);

        logger.debug(msg.toXML());

        // Fire off the delete interest group message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending delete joined product message: " + e.getMessage());
            logger.debug("sendDeleteJoinedProductMessage: " + msg.toXML());
        }

    }

    public void sendUpdateJoinMessage(String coreJID, String interestGroupID, String productType) {

        logger.info("InterestManagement:sendUpdateJoinMessage  interestGroupID=" + interestGroupID
            + " wpType=" + productType);
        String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        IQ msg = InterestGrptManagementIQFactory.createUpdateJoinMessage(coreJIDPlusResource,
            interestGroupID, productType);

        logger.debug(msg.toXML());

        // Fire off the update join message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending update join message: " + e.getMessage());
            logger.debug("sendUpdateJoinMessage: " + msg.toXML());
        }

    }

    public void sendResignMessage(String coreJID, String interestGroupID) {

        logger.info("InterestManagement:sendResignMessage");
        String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        IQ msg = InterestGrptManagementIQFactory.createResignMessage(coreJIDPlusResource,
            interestGroupID, "", getOwnJid());

        logger.debug(msg.toXML());

        // Fire off the resign message
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending resign message: " + e.getMessage());
            logger.debug("sendResignMessage: " + msg.toXML());
        }

    }

    public void sendResignRequestMessage(String coreJID, String interestGroupID,
        String interestGroupOwner) {

        logger.debug("InterestManagement:sendResignRequestMessage");
        String coreJIDPlusResource = coreConnection.getJIDPlusResourceFromCoreName(coreJID);
        IQ msg = InterestGrptManagementIQFactory.createResigRequestMessage(coreJIDPlusResource,
            interestGroupID, interestGroupOwner);

        // logger.debug(msg.toXML());

        // Fire off the resign request
        // Deal with acknowledgement asynchronously
        try {
            coreConnection.sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            logger.error("Error sending resign request message: " + e.getMessage());
            logger.debug("sendResignRequestMessage: " + msg.toXML());
        }

    }

    public String toString() {

        StringBuilder result = new StringBuilder();
        final String newLine = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " Object {");
        result.append(newLine);

        result.append(" Subscription Managers: ");
        for (String key : nodeManagers.keySet()) {
            result.append(nodeManagers.get(key));
            result.append(newLine);
        }

        result.append("}");

        return result.toString();
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#getOwnJid()
     */
    public String getOwnJid() {

        return coreConnection.getJID();
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#addNode(java.lang.String, java.lang.String, com.saic.uicds.xmpp.communications.NodeManager.NODE_ITEM_TYPE, java.lang.String)
     */
    public boolean addNode(String pubsubService, String folder, String topic, NODE_ITEM_TYPE type,
        String topicType) {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).addNode(folder, topic, type, topicType);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#addFolder(java.lang.String, java.lang.String)
     */
    public String addFolder(String pubsubService, String folder, String name) {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).addFolder(folder, name);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return null;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#addCollection(java.lang.String)
     */
    public boolean addCollection(String pubsubService, String interestGroupRoot) {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).addCollection(interestGroupRoot);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#removeNode(java.lang.String, java.lang.String)
     */
    public boolean removeNode(String pubsubService, String folder, String topic) {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).removeNode(folder, topic);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#removeNode(java.lang.String)
     */
    public boolean removeNode(String pubsubService, String node) {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).removeNode(node);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return false;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.InterestManager#getFolderContents(java.lang.String)
     */
    public DiscoverItems getFolderContents(String pubsubService, String interestGroupNode) {

        if (nodeManagers.containsKey(pubsubService)) {
            return nodeManagers.get(pubsubService).getChildrenNodes(interestGroupNode);
        }
        logger.error("No node manager for pubsub service: " + pubsubService);
        return null;
    }

}
