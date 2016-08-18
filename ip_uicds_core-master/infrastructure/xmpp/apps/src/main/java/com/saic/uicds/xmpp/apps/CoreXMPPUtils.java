package com.saic.uicds.xmpp.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;

import com.saic.uicds.xmpp.communications.CommandWithReply;
import com.saic.uicds.xmpp.communications.CoreConnection;
import com.saic.uicds.xmpp.communications.InterestManager;
import com.saic.uicds.xmpp.extensions.pubsub.PubSubIQFactory;

public class CoreXMPPUtils {

    private CoreConnection coreConnection;
    private InterestManager interestManager;
    public static String LEAF_NODE = "leaf";
    public static String COLLECTION_NODE = "collection";

    // private String startingNode = "";

    public class XMPPNode {
        public XMPPNode() {
            children = new HashSet<XMPPNode>();
        }

        public String name;
        public String type;
        public HashSet<XMPPNode> children;

        private int indents = 0;

        public String toString() {
            StringBuilder result = new StringBuilder();

            final String newLine = System.getProperty("line.separator");

            result.append(this.getClass().getName() + " Object {");
            result.append(newLine);

            printRecursive(this, result);

            result.append("}");

            return result.toString();
        }

        private void printRecursive(XMPPNode node, StringBuilder result) {
            final String newLine = System.getProperty("line.separator");

            result.append(indents++);
            result.append(" ");
            result.append(node.name);
            result.append(" : ");
            result.append(node.type);
            result.append(newLine);

            // leaf node
            if (node.children.size() == 0) {
                indents--;
                if (interestManager != null) {
                    ArrayList<String> items = interestManager.getAllNodeItems(coreConnection.getPubSubSvc(), node.name);
                    if (items != null) {
                        for (String item : items) {
                            result.append(item);
                            result.append(newLine);
                        }
                    }
                }
                return;
            }
            // collection node
            else {
                for (XMPPNode childNode : node.children) {
                    printRecursive(childNode, result);
                }
                indents--;
            }
        }
    }

    public void setCoreConnection(CoreConnection c) {
        coreConnection = c;
    }

    public void setInterestManager(InterestManager n) {
        interestManager = n;
    }

    public CoreXMPPUtils() {
    }

    public List<String> getAllNodesRecursivly() {
        ArrayList<String> nodeList = new ArrayList<String>();

        // Get the top level nodes
        Set<String> topNodes = getChildrenNodes("");

        // Work down each top level node and add nodes to the list when they are leaf nodes
        for (String topNode : topNodes) {
            Set<String> nodeSet = getNodesRecusivly(topNode);
            nodeList.addAll(nodeSet);
        }

        return nodeList;
    }

    public Set<String> getNodesRecusivly(String node) {
        HashSet<String> nodes = new HashSet<String>();

        // If a collection node then recurse, else add node
        if (!isLeaf(node)) {
            Set<String> subNodes = getChildrenNodes(node);
            if (subNodes.size() > 0) {
                for (String n : subNodes) {
                    subNodes = getNodesRecusivly(n);
                    nodes.addAll(subNodes);
                }
                nodes.add(node);
            } else {
                nodes.add(node);
            }
        } else {
            nodes.add(node);
        }

        return nodes;
    }

    public Set<String> getChildrenNodes(String collectionNode) {
        HashSet<String> nodeSet = new HashSet<String>();
        if (isCollection(collectionNode)) {
            DiscoverItems discovers = interestManager.getFolderContents(coreConnection.getPubSubSvc(), collectionNode);
            if (discovers != null) {
            	Iterator<DiscoverItems.Item> iterator = discovers.getItems();
            	while (iterator.hasNext()) {
            		DiscoverItems.Item item = (DiscoverItems.Item) iterator.next();
            		nodeSet.add(item.getNode());
            	}
            }
        }
        return nodeSet;
    }

    public Map<String, Set<String>> getNodeMap() {
        HashMap<String, Set<String>> nodeMap = new HashMap<String, Set<String>>();
        Set<String> topNodes = getChildrenNodes("");
        for (String node : topNodes) {
            System.out.println(node);
            nodeMap.put(node, getChildrenNodes(node));
        }
        return nodeMap;
    }

    public Set<XMPPNode> getNodeSet() {
        HashSet<XMPPNode> xmppNodeSet = new HashSet<XMPPNode>();

        // Get the top level nodes
        Set<String> topNodes = getChildrenNodes("");

        // Work down each top level node and add nodes to the list when they are leaf nodes
        for (String topNode : topNodes) {
            Set<XMPPNode> nodeSet = getXMPPNodesRecusivly(topNode);
            xmppNodeSet.addAll(nodeSet);
        }

        return xmppNodeSet;
    }

    public Set<XMPPNode> getXMPPNodesRecusivly(String node) {
        HashSet<XMPPNode> nodes = new HashSet<XMPPNode>();

        // If a collection node then recurse, else add node
        if (!isLeaf(node)) {
            XMPPNode xmppNode = new XMPPNode();
            xmppNode.name = node;
            xmppNode.type = COLLECTION_NODE;
            Set<String> subNodes = getChildrenNodes(node);
            if (subNodes.size() > 0) {
                for (String n : subNodes) {
                    Set<XMPPNode> xmppNodes = getXMPPNodesRecusivly(n);
                    xmppNode.children.addAll(xmppNodes);
                }
                nodes.add(xmppNode);
            } else {
                nodes.add(xmppNode);
            }
        } else {
            XMPPNode xmppNode = new XMPPNode();
            xmppNode.name = node;
            xmppNode.type = LEAF_NODE;
            nodes.add(xmppNode);
        }

        return nodes;
    }

    public boolean isLeaf(String node) {
        boolean is = false;
        DiscoverInfo info = coreConnection.discoverNodeInfo(node);
//            .getDiscoManager().discoverInfo(
//                    coreConnection.getPubSubSvc(), node);
        if (info != null) {
        	Iterator<org.jivesoftware.smackx.packet.DiscoverInfo.Identity> ids = info
        	.getIdentities();
        	while (ids.hasNext()) {
        		String type = ids.next().getType();
        		is = type.equalsIgnoreCase(LEAF_NODE);
        	}
        }
        return is;
    }

    public boolean isCollection(String node) {
        boolean is = false;
        DiscoverInfo info = coreConnection.discoverNodeInfo(node);
//            getDiscoManager().discoverInfo(
//                    coreConnection.getPubSubSvc(), node);
        if (info != null) {
        	Iterator<org.jivesoftware.smackx.packet.DiscoverInfo.Identity> ids = info
        	.getIdentities();
        	while (ids.hasNext()) {
        		String type = ids.next().getType();
        		is = type.equalsIgnoreCase(COLLECTION_NODE);
        	}
        }
        return is;
    }

    public void deleteAllNodesRecursivly() {
        List<String> nodes = getAllNodesRecursivly();
        for (String node : nodes) {
            IQ iq = PubSubIQFactory.deleteNode(coreConnection.getPubSubSvc(), node);
            CommandWithReply command;
			try {
				command = coreConnection.createCommandWithReply(iq);
            if (command.waitForSuccessOrFailure()) {
                System.out.println("Deleting node " + node);
            } else {
                if (command.getErrorCode() != 403) {
                    System.err.println("Interest group error deleting node " + node);
                    System.err.println("  message: " + command.getErrorMessage());
                    System.err.println("     code: " + command.getErrorCode());
                } else {
                    System.err.println("No permissions to delete: " + node + " skipping it");
                    continue;
                }
            }
			} catch (XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        }
    }
}
