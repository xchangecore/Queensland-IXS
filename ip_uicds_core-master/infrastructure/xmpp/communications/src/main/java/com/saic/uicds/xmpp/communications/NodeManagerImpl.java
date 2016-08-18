/**
 * 
 */
package com.saic.uicds.xmpp.communications;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.saic.uicds.xmpp.extensions.pubsub.PubSubConstants;
import com.saic.uicds.xmpp.extensions.pubsub.PubSubIQFactory;
import com.saic.uicds.xmpp.extensions.util.NamespaceContextImpl;
import com.saic.uicds.xmpp.extensions.util.PubSubEventExtension;
import com.saic.uicds.xmpp.extensions.util.PubSubIQ;

/**
 * @author roger
 * 
 */
public class NodeManagerImpl
    implements NodeManager {

    public enum TOPIC_ITEM_TYPE {
        ITEM, ITEM_LIST
    };

    private Logger logger = Logger.getLogger(this.getClass());

    public enum NODE_ITEM_TYPE {
        ITEM, ITEM_LIST
    };

    public static class NodeSubscriptionInfo {
        public String subid;
        public String node;
        public String jid;
        public String affiliation;

        public NodeSubscriptionInfo(String jid, String node, String subid, String affiliation) {

            this.jid = jid;
            this.node = node;
            this.subid = subid;
            this.affiliation = affiliation;
        }

        public String toString() {

            StringBuilder result = new StringBuilder();
            final String newLine = System.getProperty("line.separator");

            result.append(this.getClass().getName() + " Object {");
            result.append(newLine);

            result.append("  Node Info: ");
            result.append("    jid: ");
            result.append(jid);
            result.append(newLine);
            result.append("    node: ");
            result.append(node);
            result.append(newLine);
            result.append("    subid: ");
            result.append(subid);
            result.append(newLine);
            result.append("    affiliation: ");
            result.append(affiliation);
            result.append(newLine);

            result.append("}");

            return result.toString();
        }
    }

    /**
     * Packet filter used internally to filter on subscriptions to a particular node.
     * 
     * @author roger
     * 
     */
    public class SubscriptionPacketFilter
        implements PacketFilter {
        private String nodeName;

        private List<Pattern> nodePatterns = new ArrayList<Pattern>();

        public SubscriptionPacketFilter() {

        }

        public void addNodeFilter(String node) {

            this.nodeName = node;
            if (logger.isDebugEnabled())
            	logger.debug("SubscriptionPacketFilter constructor : node=" + this.nodeName);
            try {
                String p1 = "node=[\"']" + this.nodeName + "[\"']";
                nodePatterns.add(Pattern.compile(p1));
            } catch (IllegalArgumentException e) {
                logger.error("ERROR: SubscriptionPacketFilter Illegal argument to Pattern.compile");
                throw e;
            }
        }

        public boolean accept(Packet packet) {

            if (logger.isDebugEnabled())
            	logger.debug("SubscriptionPacketFilter:accept nodeName= " + nodeName);
            // + "  ... evaluating: " + packet.toXML());
            boolean matchFound = false;
            // Only pass through messages from our connections pubsub service.
            if (packet instanceof Message && getPubsubService().equals(packet.getFrom())) {
                // Only for this node
                for (Pattern p : nodePatterns) {
                    matchFound = p.matcher(packet.toXML()).find();
                    if (matchFound) {
                        logger.debug("SubscriptionPacketFilter:accept evaluating result= "
                            + matchFound + " pubsub:" + getPubsubService() + " packet's from:"
                            + packet.getFrom());
                        logger.debug("SubscriptionPacketFilter:accept evaluating result= "
                            + matchFound + " pattern:" + p.pattern());
                        break;
                    }
                }
            }

            return matchFound;
        }
    }

    private CoreConnection coreConnection;
    private String pubsubService;

    private SubscriptionPacketFilter subscriptionPacketFilter;

    // Keyed by node name
    private HashMap<String, NodeSubscriptionInfo> subscriptions;

    private XPath pubsubXpath = null;
    private NamespaceContextImpl pubsubNameSpace = null;

    /**
     * Default constructor. Must call initialize before using this object.
     */
    public NodeManagerImpl() {

        subscriptions = null;
        coreConnection = null;
        subscriptionPacketFilter = null;
    }

    @Override
    public String getPubsubService() {

        return pubsubService;
    }

    public void setPubsubService(String pubsubService) {

        this.pubsubService = pubsubService;
    }

    /**
     * Constructor that calls initialize(CoreConnection conection). Object is valid to be used after
     * this constructor.
     * 
     * @param connection connection to the XMPP server.
     */
    public NodeManagerImpl(CoreConnection connection) {

        initialize(connection);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#isInitialized()
     */
    @Override
    public boolean isInitialized() {

        return (coreConnection != null && pubsubService != null);
    }

    /**
     * Initialize the object for use. Gets all the current subscriptions from the XMPP server for
     * the connections JID.
     * 
     * @param connection connection to the XMPP server
     */
    private void initialize(CoreConnection connection) {

    	if (logger.isDebugEnabled())
    		logger.debug("initialize");
        setConnection(connection);
        setPubsubService(connection.getPubSubSvc());

        subscriptionPacketFilter = new SubscriptionPacketFilter();

        subscriptions = new HashMap<String, NodeSubscriptionInfo>();

        XPathFactory xFactory = XPathFactory.newInstance();
        pubsubXpath = xFactory.newXPath();
        pubsubNameSpace = new NamespaceContextImpl();
        pubsubNameSpace.setNamespace("pubsub", "http://jabber.org/protocol/pubsub");

        pubsubXpath.setNamespaceContext(pubsubNameSpace);

        updateSubscriptionMap();
    }

    private void setConnection(CoreConnection connection) {

        if (connection != null) {
            this.coreConnection = connection;
        } else {
            return;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#updateSubscriptionMap()
     */
    @Override
    public synchronized void updateSubscriptionMap() {

    	if (logger.isDebugEnabled())
    		logger.debug("updateSubscriptionMap - start");
        // Clear out the current subscription information
        subscriptions.clear();

        // Get all the subscrptions for this current JID
        // and use those instead of creating new subscriptions
        if (logger.isDebugEnabled())
        	logger.debug("updateSubscriptionMap - Get all the subscription for this current JID="+pubsubService);
        IQ iq = PubSubIQFactory.retrieveSubscriptions(pubsubService);
        if (logger.isDebugEnabled())
        	logger.debug("updateSubscriptionMap - IQ=[" + iq.toXML() + "]");

        CommandWithReply command = null;
        try {
        	if (logger.isDebugEnabled())
        		logger.debug("updateSubscriptionMap: coreConnection.createCommandWithReply.");

            command = coreConnection.createCommandWithReply(iq);
            
        } catch (XMPPException e1) {
            logger.error("Error sending stanza to update subscriptions: " + e1.getMessage());
        }

        if (command.waitForSuccessOrFailure()) {
            String xml = "<?xml version='1.0'?>" + command.getResult().toXML();
            if (logger.isDebugEnabled())
            	//logger.debug("updateSubscriptionMap: reply=[" + xml + "]");
            	logger.debug("updateSubscriptionMap: populate subscriptions");
            try {
                Object res = pubsubXpath.evaluate("//pubsub:subscriptions", new InputSource(
                    new StringReader(xml)), XPathConstants.NODE);
                Node n = (Node) res;
                if (n != null) {
                    NodeList nl = n.getChildNodes();
                    Node nn = null;
                    for (int i = 0; i < nl.getLength(); i++) {
                        nn = nl.item(i);
                        if (nn.hasAttributes()) {
                            NamedNodeMap nnm = nn.getAttributes();
                            // Is this the node we want
                            Node jidNode = nnm.getNamedItem("jid");
                            Node nodeNode = nnm.getNamedItem("node");
                            Node subidNode = nnm.getNamedItem("subid");
                            Node affiliationNode = nnm.getNamedItem("affiliation");

                            String jid = null;
                            String node = null;
                            String subid = null;
                            String affiliation = null;
                            if (jidNode != null)
                                jid = jidNode.getNodeValue();
                            if (nodeNode != null)
                                node = nodeNode.getNodeValue();
                            if (subidNode != null)
                                subid = subidNode.getNodeValue();
                            if (affiliationNode != null)
                                affiliation = affiliationNode.getNodeValue();
/*
                            if (logger.isDebugEnabled())
                            	logger.debug("NodeSubscriptionManager:updateSubscriptionMap subscription: "
                            			+ jid + " " + node + " " + subid + " " + affiliation);
*/
                            NodeSubscriptionInfo item = new NodeSubscriptionInfo(jid, node, subid,
                                affiliation);
                            subscriptions.put(node, item);
                        }
                    }
                }
            } catch (XPathExpressionException e) {
                logger.error("ERROR evaluating xpath expression " + e.getMessage());
            }
        } else {
            // This means we have no subscriptions an empty <subscriptions>
            // element also means this
            if (command.getErrorType() == XMPPError.Type.CANCEL && command.getErrorCode() == 404) {
                subscriptions.clear();
            } else {
                logger.error("NodeSubscriptionManager error updating subscription map");
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
                logger.error("     type: " + command.getErrorType());
            }
        }
    	if (logger.isDebugEnabled())
    		logger.debug("updateSubscriptionMap - end");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#unsubscribeAll()
     */
    @Override
    public synchronized void unsubscribeAll() {

    	if (logger.isDebugEnabled())
    		logger.debug("unsubscribeALL()");
        // Update from server
        updateSubscriptionMap();

        Set<String> set = subscriptions.keySet();
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            NodeSubscriptionInfo nodeInfo = subscriptions.get(key);
            if (coreConnection.getJID().equals(nodeInfo.jid)) {
                unsubscribe(nodeInfo);
            }
        }

        // Update again from server
        updateSubscriptionMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#unsubscribeAll(java.lang.String)
     */
    @Override
    public synchronized void unsubscribeAll(String uuid) {

    	if (logger.isDebugEnabled())
    		logger.debug("unsubscribeALL. uuid="+uuid);
        // Update from server
        updateSubscriptionMap();

        String p1 = coreConnection.getInterestGroupRoot() + "/" + uuid;
        Pattern nodePattern = Pattern.compile(p1);
        Set<String> set = subscriptions.keySet();
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            NodeSubscriptionInfo nodeInfo = subscriptions.get(key);
            if (nodePattern.matcher(nodeInfo.node).find()
                && coreConnection.getJID().equals(nodeInfo.jid)) {
                unsubscribe(nodeInfo);
            }
        }

        // Update again from server
        updateSubscriptionMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#unsubscribe(java.lang.String)
     */
    @Override
    public synchronized void unsubscribe(String node) {

    	if (logger.isDebugEnabled())
    		logger.debug("unsubscribe. Node="+node);
        // Find the information about this node
        NodeSubscriptionInfo nodeInfo = subscriptions.get(node);
        if (nodeInfo != null) {
            unsubscribe(nodeInfo);
        }
    }

    private void unsubscribe(NodeSubscriptionInfo nodeInfo) {

    	if (logger.isDebugEnabled())
    		logger.debug("unsubscribe. NodeSubscriptionInfo");

        IQ iq = PubSubIQFactory.unsubscribeNode(pubsubService, nodeInfo.jid, nodeInfo.node,
            nodeInfo.subid);

        // logger.debug("UNSUB: "+iq.toXML());
        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            int counter = 3;
            boolean success = command.waitForSuccessOrFailure();
            while (!success && counter > 0) {
                counter--;
                success = command.waitForSuccessOrFailure();
            }
            if (success) {
                // logger.debug("Unsubscribing to node " + nodeInfo.node);
            } else {
                logger.error("NodeSubscriptionManager error unsubscribing node " + nodeInfo.node);
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to unsubscribe from node: " + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#isSubscribedTo(java.lang.String)
     */
    @Override
    public synchronized boolean isSubscribedTo(String node) {

    	if (logger.isDebugEnabled())
    		logger.debug("isSubscribedTo. Node="+node);

        Set<String> set = subscriptions.keySet();
        // System.out.println("looking for "+node);
        // for (String key : set) {
        // System.out.println(key);
        // }
        // if (set.contains(node)) System.out.println("OK");
        return set.contains(node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#getSubscriptionID(java.lang.String)
     */
    @Override
    public synchronized String getSubscriptionID(String interestGroupNode) {

    	if (logger.isDebugEnabled())
    		logger.debug("getSubscriptionID. interestGroupNode="+interestGroupNode);

        String id = null;
        if (subscriptions.containsKey(interestGroupNode)) {
            id = subscriptions.get(interestGroupNode).subid;
        }
        return id;
    }

    @Override
    public java.util.List<String> getSubscribedJIDs(String nodeName) {

    	if (logger.isDebugEnabled())
    		logger.debug("getSubscribedJID. nodeName="+nodeName);

        ArrayList<String> subscribers = new ArrayList<String>();

        IQ iq = PubSubIQFactory.getSubscriptionList(coreConnection.getPubSubSvc(), nodeName);
        System.out.println(iq.toXML());

        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            int counter = 3;
            boolean success = command.waitForSuccessOrFailure();
            while (!success && counter > 0) {
                counter--;
                success = command.waitForSuccessOrFailure();
            }
            if (success) {
                IQ result = command.getResult();
                System.out.println(result.toXML());
                String xml = result.getChildElementXML();
                if (result instanceof PubSubIQ) {
                    PubSubIQ pubSubIQ = (PubSubIQ) result;
                    if (pubSubIQ.hasSubscriptions()) {
                        Map<String, PubSubIQ.Subscription> subs = pubSubIQ.getSubscriptions();
                        for (String jid : subs.keySet()) {
                            subscribers.add(StringUtils.parseBareAddress(jid));
                        }
                    }
                }
            } else {
                logger.error("getSubscribedJIDs error unsubscribing node " + nodeName);
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to unsubscribe from node: " + e.getMessage());
        }

        return subscribers;
    };

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#subscribeToNode(java.lang.String,
     * org.jivesoftware.smack.PacketListener)
     */
    @Override
    public List<String> subscribeToNode(String node, PacketListener listener) throws XMPPException {

        logger.info("NodeSubscriptionManager:subscribeToNode node=" + node);
        // return list
        List<String> messages = new ArrayList<String>();

        // Make sure we have the latest subscriptions from the XMPP server
        updateSubscriptionMap();

        // If we're not already subscribed then do the subscription
        if (!isSubscribedTo(node)) {

            // Create the subscribe message
            logger.debug("Subscribing to node " + node + " from " + pubsubService);

            IQ iq = PubSubIQFactory.subscribeNode(pubsubService,
                coreConnection.getJIDPlusResource(), node, null);

            logger.debug("subscribeToNode:   OWNER Subscribing - request IQ:[\n\n*****\n"
                + iq.toXML() + "\n\n****\n");

            CommandWithReply command = null;
            try {
                command = coreConnection.createCommandWithReply(iq);
            } catch (XMPPException e) {
                // TODO: Fix error handling here
                throw e;
            }

            // Wait a couple of timeouts for subscriptions
            int counter = 3;
            boolean success = command.waitForSuccessOrFailure();
            while (!success && counter > 0) {
                counter--;
                success = command.waitForSuccessOrFailure();
            }

            if (success) {
                // Update the subscription filter and replace it in the packet listener
                subscriptionPacketFilter.addNodeFilter(node);
                coreConnection.addPacketListener(listener, subscriptionPacketFilter);

                logger.debug("    Subscribing to node " + node + " from " + pubsubService);
                logger.info("subscribeToNode: reply command" + command.getResult().toXML());

                // Save subscription information
                logger.debug("adding subscription to node[" + node + "] with subid="
                    + command.getSubscriptionID() + " to subacriptionMap");

                addToSubscriptions(node, command);

            } else {
                System.err.println("NodeSubscriptionManager error subscribing node " + node);
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
                logger.error("     type: " + command.getErrorType());
                logger.error("condition: " + command.getErrorCondition());
                if (command.getErrorType() == XMPPError.Type.AUTH) {
                    logger.error("   Not authorized for this action.");
                }
                throw new XMPPException(command.getXMPPError());
            }
        }
        // If already subscribed to node then get add the listener,
        // get the initial items on the node, and invoke the listener
        else {
            // Update the subscription filter and replace it in the packet listener
            subscriptionPacketFilter.addNodeFilter(node);
            coreConnection.addPacketListener(listener, subscriptionPacketFilter);

            logger.debug("    Subscribing to already subed node " + node + " from " + pubsubService);
        }

        try {
            logger.debug("getAllNodeItems for " + node);
            messages = getAllNodeItems(node);
            if (messages != null && !messages.isEmpty()) {
                logger.debug("Got MESSAGES in NodeSubscriptionManager:subscribeToNode");
                PubSubEventExtension pubsubExt = new PubSubEventExtension("event",
                    "http://jabber.org/protocol/pubsub#event");
                for (Object o : messages) {
                    String xml = (String) o;
                    // logger.info("     processing "+xml);
                    pubsubExt.addItem(xml);
                }
                // TODO: should make sure the full xml for this message is correct
                Message msg = new Message();
                msg.addExtension(pubsubExt);
                listener.processPacket(msg);
            }
        } catch (IllegalArgumentException e) {
            messages = new ArrayList<String>();
        } catch (IllegalStateException e) {
            logger.error("ERROR NodeSubscriptionManager:subscribeToNode getAllNodeItems failed: "
                + e);
        } catch (Exception e) {
            logger.error("ERROR NodeSubscriptionManager:subscribeToNode " + node + " : " + e);
            e.printStackTrace();
        }
        messages.clear();

        return messages;
    }

    private synchronized void addToSubscriptions(String node, CommandWithReply command) {

        subscriptions.put(node,
            new NodeSubscriptionInfo(coreConnection.getJID(), node, command.getSubscriptionID(),
                null));

        if (logger.isDebugEnabled())
        	logger.debug("===> subscribeToNode: subscription map contains key[" + node + "] is "
            + subscriptions.containsKey(node));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#publishToNode(java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean publishToNode(String nodeName, String itemText) {

        boolean success = false;

        // Create the publish stanza
        IQ iq = PubSubIQFactory.publishItem(pubsubService, nodeName, itemText);
        iq.setFrom(coreConnection.getJID());

    	if (logger.isDebugEnabled())
    		//logger.debug("publishToNode: requestIQ[\n****\n" + iq.toXML() + "\n]\n****\n");
    		logger.debug("publishToNode: nodeName " + nodeName);
    	
        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            success = command.waitForSuccessOrFailure();
            if (!success) {
                logger.error(iq.toXML());
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
                logger.error("     type: " + command.getErrorType());
                logger.error("condition: " + command.getErrorCondition());
                if (command.getErrorType() == XMPPError.Type.AUTH) {
                    logger.error("   Not authorized for this action.");
                }
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to publish a node: " + e.getMessage());
        }
        return success;
    }

    @Override
    public boolean removeItem(String nodeName, String itemUUID) {

    	if (logger.isDebugEnabled())
            logger.debug("removeItem: item " + itemUUID + " from node " + nodeName);

    	boolean removed = false;

        IQ iq = PubSubIQFactory.deleteItem(pubsubService, nodeName, itemUUID);
        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            if (command.waitForSuccessOrFailure()) {
                // logger.debug("Deleting item " + itemUUID + " from node " + nodeName);
                removed = true;
            } else {
                logger.error("Interest group error deleting item " + itemUUID + " from node "
                    + nodeName);
                if (command.getErrorType() == XMPPError.Type.AUTH) {
                    logger.error("   Not authorized for this action.");
                } else {
                    logger.error("  message: " + command.getErrorMessage());
                    logger.error("     code: " + command.getErrorCode());
                }
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to remove item: " + e.getMessage());
        }

        return removed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#getChildrenNodes(java.lang.String)
     */
    @Override
    public DiscoverItems getChildrenNodes(String parentNode) {
 
    	if (logger.isDebugEnabled())
    		logger.debug("getChildrenNodes. parentNode="+parentNode);
        DiscoverItems items = null;
        items = coreConnection.discoverNodeItems(parentNode);
        return items;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#retrieveNodeItem(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String retrieveNodeItem(String node, String itemID) throws IllegalStateException,
        IllegalArgumentException {

    	if (logger.isDebugEnabled())
    		logger.debug("retrieveNodeItem: item "+itemID+" from node "+node);
    	
        // Get the subscription ID
        if (subscriptions == null || node == null) {
            String error = "NodeSubscriptionManager:retrieveNodeItem error: ";
            if (subscriptions == null) {
                error += "subscriptions is null";
            }
            if (node == null) {
                error += "node is null";
            }
            throw new IllegalStateException(error);
        }
        // Get the subscription ID for this node
        String subid = getSubIDForNode(node);

        // Get all the current items.
        IQ iq = PubSubIQFactory.retrieveItem(pubsubService, node, subid, itemID);

        // logger.info("NodeManager.retrieveNodeItem: retrieving " + itemID + " from node " + node
        // + " requestIQ=[\n****\n" + iq.toXML() + "\n]\n****\n");

        // PacketCollector updates = coreConnection.createPacketCollector(new PacketIDFilter(
        // iq.getPacketID()));

        CommandWithReply command = null;
        try {
            command = coreConnection.createCommandWithReply(iq);
            int counter = 3;
            boolean success = command.waitForSuccessOrFailure();
            while (!success && counter > 0) {
                counter--;
                success = command.waitForSuccessOrFailure();
            }
            if (success) {
                // logger.info("Getting item from " + node);
                // logger.info("ReplyIQ=[" + cmd.result.toXML() + "]");

            } else {
                // System.err.println("NodeSubscriptionManager error getting items from node " +
                // node);
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
                logger.error("     type: " + command.getErrorType());
                logger.error("condition: " + command.getErrorCondition());
                if (command.getResult() != null) {
                    logger.error(command.getResult().toXML());
                }
                if (command.getErrorType() == XMPPError.Type.AUTH) {
                    logger.error("   Not authorized for this action.");
                }
            }

        } catch (XMPPException e) {
            logger.error("Error sending stanza to retrieve a node item: " + e.getMessage());
        }

        // Parse out returned items into a list
        ArrayList<String> messages = new ArrayList<String>();

        if (command != null) {
            IQ iqResponse = command.getResult();
            if (iqResponse != null) {

                if (iqResponse instanceof PubSubIQ) {
                    // logger.info("GOT a PubSubIQ from initial updates");
                    PubSubIQ pubsub = (PubSubIQ) iqResponse;
                    // logger.info("retrieveItem XML: " + iqResponse.toXML());
                    Iterator<com.saic.uicds.xmpp.extensions.util.Item> it = pubsub.getItems();
                    while (it.hasNext()) {
                        String item = it.next().toXML();
                        // logger.info("retrieveItem ITEM: " + item);
                        if (!item.isEmpty()) {
                            messages.add(item);
                        }
                    }
                } else {
                    messages.add(iqResponse.getChildElementXML());
                }
            }
        }

        if (messages.size() == 1) {
            return messages.get(0);
        } else {
            return null;
            /* maybe this is a valid behavior
            throw new IllegalStateException("zero or more than one items [id=" + itemID
                + " retrieved from node " + node, null);
            */
        }

    }

    private synchronized String getSubIDForNode(String node) {

    	if (logger.isDebugEnabled())
    		logger.debug("getSubIDForNode: node "+node);
    	
        String subid = null;
        if (subscriptions.containsKey(node)) {
            subid = subscriptions.get(node).subid;
        }
        if (subid == null || subid.length() == 0) {
            throw new IllegalArgumentException("not subscribed to node " + node);
        }
        return subid;
    }

    // TODO: fix concurrancy problem with using this call and creating Interest groups
    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#getAllNodeItems(java.lang.String)
     */
    @Override
    public ArrayList<String> getAllNodeItems(String node) throws IllegalArgumentException {

    	if (logger.isDebugEnabled())
    		logger.debug("getAllNodeItems: node "+node);
    	
        //System.out.println("NodeSubscriptionManager -  getting all items from node " + node);
        if (subscriptions == null || node == null) {
            String error = "NodeSubscriptionManager:getAllNodeItems error: ";
            if (subscriptions == null) {
                error += "subscriptions is null";
            }
            if (node == null) {
                error += "node is null";
            }
            throw new IllegalStateException(error);
        }

        // Get subid for this node
        String subid = getSubIDForNode(node);

        // Get all the current items.
        IQ iq = PubSubIQFactory.retrieveItems(pubsubService, node, subid, null);
        // logger.info(iq.toXML());

        CommandWithReply command = null;
        try {
            command = coreConnection.createCommandWithReply(iq);

            int counter = 3;
            boolean success = command.waitForSuccessOrFailure();
            while (!success && counter > 0) {
                counter--;
                success = command.waitForSuccessOrFailure();
            }

            if (success) {
                // logger.info("Getting items from " + node);
                // logger.info(iq.toXML());

            } else {
                System.err.println("NodeSubscriptionManager error getting items from node " + node);
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
                logger.error("     type: " + command.getErrorType());
                logger.error("condition: " + command.getErrorCondition());
                if (command.getResult() != null) {
                    logger.error(command.getResult().toXML());
                }
                if (command.getErrorType() == XMPPError.Type.AUTH) {
                    logger.error("   Not authorized for this action.");
                }
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to get node items: " + e.getMessage());
            return null;
        }

        // Parse out returned items into a list
        ArrayList<String> messages = new ArrayList<String>();

        IQ iqResponse = command.getResult();
        if (iqResponse != null) {

            if (iqResponse instanceof PubSubIQ) {
                // logger.info(
                // "GOT a PubSubIQ from initial updates in NodeSubscription:getAllNodeItems");
                PubSubIQ pubsub = (PubSubIQ) iqResponse;
                Iterator<com.saic.uicds.xmpp.extensions.util.Item> it = pubsub.getItems();
                while (it.hasNext()) {
                    // String item = it.next().toXML();
                    // logger.info("ITEM: "+item);
                    // logger.info("XML: "+iqResponse.toXML());
                    messages.add(it.next().toXML());
                }
            } else {
                messages.add(iqResponse.getChildElementXML());
            }

            // logger.info("CHILD ELEMENT: "+iqResponse.getChildElementXML());
            // ArbitraryPacketExtension arb = new
            // ArbitraryPacketExtension("event", "http://jabber.org/protocol/pubsub",
            // iqResponse.getChildElementXML());
            // Message msg = new Message();
            // msg.addExtension(arb);
            // messages.add(msg.toXML());

        }
        return messages;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#addNode(java.lang.String,
     * java.lang.String, com.saic.uicds.xmpp.communications.NodeManagerImpl.NODE_ITEM_TYPE,
     * java.lang.String)
     */
    @Override
    public boolean addNode(String folder, String topic, NODE_ITEM_TYPE type, String topicType) {
 
    	if (logger.isDebugEnabled())
    		logger.debug("addNode: topic "+topic+" of topicType "+topicType+" in folder "+folder);
    	

        // Configure top leaf nodes
        // TODO: Check pubsub#deliver and pubsub#include_body
        // maybe change deliver_payloads and get just the notification
        // and then retrieve the new payload by item id
        // Looks like it should never do this (openfire code Node.java:2029)

        boolean created = false;

        DataForm dataForm = new DataForm("submit");
        FormField formTypeField = new FormField("FORM_TYPE");
        formTypeField.setType("hidden");
        formTypeField.addValue("http://jabber.org/protocol/pubsub#node_config");
        dataForm.addField(formTypeField);

        // FormField sendItemsField = new FormField("pubsub#send_item_subscribe");
        // sendItemsField.setType(FormField.TYPE_BOOLEAN);
        // sendItemsField.addValue("false");
        // xData.addField(sendItemsField);

        FormField persistItemsField = new FormField("pubsub#persist_items");
        persistItemsField.addValue("1");
        dataForm.addField(persistItemsField);

        FormField sendItemsOnSubField = new FormField("pubsub#send_item_subscribe");
        sendItemsOnSubField.addValue("0");
        dataForm.addField(sendItemsOnSubField);

        FormField accessField = new FormField("pubsub#access_model");
        accessField.addValue("presence");
        dataForm.addField(accessField);

        if (type == NODE_ITEM_TYPE.ITEM) {
            FormField maxItemsField = new FormField("pubsub#max_items");
            maxItemsField.addValue("1");
            dataForm.addField(maxItemsField);
        }
        // TODO: is max value of short the right value here
        else {
            FormField maxItemsField = new FormField("pubsub#max_items");
            maxItemsField.addValue(Short.toString(Short.MAX_VALUE));
            dataForm.addField(maxItemsField);
        }

        if (topicType != null && topicType != "") {
            FormField typeField = new FormField("pubsub#type");
            typeField.addValue(topicType);
            dataForm.addField(typeField);
        }

        // Add leaf node
        IQ iq = PubSubIQFactory.createAssociatedNode(pubsubService, folder, topic,
            PubSubConstants.LEAF_NODE, dataForm);

        iq.setFrom(coreConnection.getJIDPlusResource());

        // logger.info("addNode: requestIQ[\n\n****\n" + iq.toXML() + "\n]****\n");

        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            if (command.waitForSuccessOrFailure()) {
                // logger.info("Creating node " + topic);
                // logger.info("replyIQ:" + gisdataCmd.result.toXML());
                created = true;

            } else {
                if ((command.getErrorCode() == 409)) {
                    // RDW - should make sure the node configuration is correct
                    // on the existing node
                    created = true;
                } else {
                    logger.error("Error creating node " + topic);
                    logger.error("  message: " + command.getErrorMessage());
                    logger.error("     code: " + command.getErrorCode());
                    logger.error("     type: " + command.getErrorType());
                    logger.error("condition: " + command.getErrorCondition());
                }
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to add a node: " + e.getMessage());
        }

        return created;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#removeNode(java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean removeNode(String folder, String topic) {

    	if (logger.isDebugEnabled())
    		logger.debug("removeNode: topic "+topic +" from folder "+folder);
    	
        boolean removed = false;
        String node = folder + "/" + topic;
        IQ iq = PubSubIQFactory.deleteNode(pubsubService, node);
        // logger.info("Deleting node " + node);
        // logger.info(iq.toXML());
        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            if (command.waitForSuccessOrFailure()) {
                removed = true;
            } else {
                logger.error("error deleting node " + node);
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
                logger.error("     type: " + command.getErrorType());
                logger.error("condition: " + command.getErrorCondition());
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to remove a node: " + e.getMessage());
        }
        return removed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#removeNode(java.lang.String)
     */
    public boolean removeNode(String node) {

    	if (logger.isDebugEnabled())
    		logger.debug("removeNode: node "+node);
    	

        boolean removed = false;
        IQ iq = PubSubIQFactory.deleteNode(pubsubService, node);
        // logger.info("Deleting node " + node);
        // logger.info(iq.toXML());
        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            if (command.waitForSuccessOrFailure()) {
                removed = true;
            } else {
                logger.error("error deleting node " + node);
                logger.error("  message: " + command.getErrorMessage());
                logger.error("     code: " + command.getErrorCode());
                logger.error("     type: " + command.getErrorType());
                logger.error("condition: " + command.getErrorCondition());
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to remove a node: " + e.getMessage());
        }
        return removed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#addFolder(java.lang.String,
     * java.lang.String)
     */
    @Override
    public String addFolder(String folder, String name) {

    	if (logger.isDebugEnabled())
    		logger.debug("addFolder: name "+name+" from folder "+folder);
    	
        String fullName = null;
        IQ iq = null;

        iq = PubSubIQFactory.createAssociatedNode(pubsubService, folder, name,
            PubSubConstants.COLLECTION_NODE, null);
        // logger.info("addFolder: command[" + iq.toXML() + "]");
        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            if (command.waitForSuccessOrFailure()) {
                fullName = folder + "/" + name;
                // logger.info("Creating node " + fullName);
                // logger.info(iq.toXML());
            } else {
                if ((command.getErrorCode() == 409)) {
                    fullName = folder + "/" + name;
                } else {
                    logger.error("error creating folder " + name + " in " + folder);
                    logger.error("  message: " + command.getErrorMessage());
                    logger.error("     code: " + command.getErrorCode());
                    logger.error("     type: " + command.getErrorType());
                    logger.error("condition: " + command.getErrorCondition());
                }
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to create a folder: " + e.getMessage());
        }
        return fullName;
    }

    public String toString() {

        StringBuilder result = new StringBuilder();
        final String newLine = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " Object {");
        result.append(newLine);

        result.append(" Subscriptions: ");
        // result.append(subscriptions.size());
        result.append(subscriptions);
        result.append(newLine);

        result.append("}");

        return result.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.NodeManager#addCollection(java.lang.String)
     */
    @Override
    public boolean addCollection(String interestGroupRoot) {

    	if (logger.isDebugEnabled())
    		logger.debug("addCollection: interestGroupRoot "+interestGroupRoot);
    	
        boolean created = false;
        // Create the create node stanza and send it
        IQ iq = PubSubIQFactory.createCollection(pubsubService, interestGroupRoot, null);
        // logger.info("addCollection: command[" + iq.toXML() + "]");

        CommandWithReply command;
        try {
            command = coreConnection.createCommandWithReply(iq);
            if (command.waitForSuccessOrFailure()) {
                created = true;
            } else {
                // If it already exists this is ok
                if (!(command.getErrorCode() == 409)) {
                    logger.error("Error creating " + coreConnection.getInterestGroupRoot()
                        + " node");
                } else {
                    created = true;
                }
            }
        } catch (XMPPException e) {
            logger.error("Error sending stanza to create add a collection: " + e.getMessage());
        }
        return created;
    }

}
