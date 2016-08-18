package com.saic.uicds.xmpp.communications;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.saic.uicds.core.infrastructure.messages.CoreStatusUpdateMessage;

public class RosterManager {

    private Logger logger = Logger.getLogger(this.getClass());

    // Key is JID of core, value is the title for the core
    private static BiMap<String, String> rosterMap = new HashBiMap<String, String>();

    public class PresencePacketListener
        implements PacketListener {

        private RosterManager rosterManager;

        public PresencePacketListener(RosterManager rosterManager) {

            super();
            this.rosterManager = rosterManager;
        }

        public void processPacket(Packet pack) {

            // Ignore errors
            if (pack.getError() != null) {
                return;
            }

            logger.debug("Got presense: " + pack.toXML());

            if (pack instanceof Presence) {

                Presence presence = (Presence) pack;

                rosterManager.handlePresence(presence);
            }
        }

    }

    class CoreRosterListener
        implements RosterListener {

        RosterManager rosterManager;

        public CoreRosterListener(RosterManager rosterManager) {

            this.rosterManager = rosterManager;
        }

        // Ignored events public void entriesAdded(Collection<String> addresses) {}
        public void entriesDeleted(Collection<String> addresses) {

            logger.info("CoreRosterListener:entriesDeleted");
            for (Object o : addresses) {
                logger.info((String) o + " deleted");
                if (roster.contains((String) o)) {
                    logger.error("NO it's not");
                }
            }
        }

        public void entriesUpdated(Collection<String> addresses) {

            logger.info("CoreRosterListener:entriesUpdated");
            for (Object o : addresses) {
                String JID = (String) o;
                Presence.Type type = roster.getPresence(JID).getType();
                logger.info(JID + " updated to " + type);
            }
        }

        public void presenceChanged(Presence presence) {

            String fullJID = presence.getFrom();
            Presence.Type type = presence.getType();
            logger.info("CoreRosterListener:presenceChanged: " + fullJID + " to " + type);

            if (type == Presence.Type.available || type == Presence.Type.unavailable) {
                sendCoreStatusUpdate(fullJID, type.toString());
            }

        }

        public void entriesAdded(Collection<String> arg0) {

            logger.info("CoreRosterListener:entriesAdded");
            for (Object o : arg0) {
                logger.info((String) o + " added");
            }
        }
    };

    // TODO: look at if the knownCores is still needed. Can probably just use the rosterMap keys
    private CoreConnectionImpl connection;

    private Roster roster;

    @SuppressWarnings("unused")
    private String replaceHostname(String value) {

        String localhost = "";
        try {
            localhost = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value.replace("localhost", localhost);
    }

    /**
     * Constructor
     * 
     * @param con - CoreConnection to use
     */
    public RosterManager(CoreConnectionImpl con) {

        connection = con;

        if (!rosterMap.containsValue(connection.getServer().toLowerCase())) {
            logger.debug("RosterManager : adding to map: JID=" + connection.getJID() + " name="
                + connection.getServer());
            rosterMap.put(connection.getJID(), connection.getJID());
            // rosterMap.put(connection.getJID(), connection.getServer());
        }

        // Get the initial roster
        roster = connection.getRoster();

        // Check the roster against the configured known cores if configured
        // also don't mess with the owners roster
        if (roster != null) {
            checkRoster();
            printRoster();
        }

        // Add a listener
        roster.addRosterListener(new CoreRosterListener(this));

        // Set subscription mode to manual
        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);

        // Add a presence packet listener
        connection.addPacketListener(new PresencePacketListener(this), new PacketTypeFilter(
            Presence.class));

    }

    private void handlePresence(Presence presence) {

        // Get the JID
        String bareJID = org.jivesoftware.smack.util.StringUtils.parseBareAddress(presence.getFrom());
        String fullJID = presence.getFrom();

        // Ignore presence messages from this core (all resources)
        if (!bareJID.equalsIgnoreCase(connection.getJID())) {

            // Check for any outstanding presence subscription requests and
            // respond if necessary and it is in our list of knownCores
            if (rosterMap.containsKey(bareJID.toLowerCase())) {
                logger.debug("found known core " + bareJID + " has presence " + presence.getType());
                checkPresence(bareJID, presence.getType(), presence.getFrom());
                sendCoreStatusUpdate(fullJID, presence.getType().toString());
            } else {
                logger.debug("presence from unknown core: " + bareJID);
                if (presence.getType() == Presence.Type.subscribe) {
                    sendSubscribeDeclinedResponse(bareJID);
                } else if (presence.getType() == Presence.Type.unsubscribed) {
                    // logger.debug(" Send decline acknowledge for "+core);
                    sendSubscribeDeclineAcknowledge(bareJID);
                    sendCoreStatusUpdate(fullJID, presence.getType().toString());
                }
            }
        }
    }

    public void checkRoster() {

        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            String coreJID = entry.getUser();
            String name = entry.getName();
            if (name == null) {
                name = coreJID;
            }

            // Add the entry to the local map if it is not there.
            if (!rosterMap.containsKey(coreJID)) {
                logger.debug("\n\n**** RosterManager:checkRoster adding  to map JID= " + coreJID
                    + " name=" + name);
                rosterMap.put(coreJID, name);

            }
            // Check the presence of cores in roster for needed response
            // but don't do anything if this is the local host
            if (!coreJID.equalsIgnoreCase(connection.getJID())) {

                if ((coreJID != null) && (name != null)) {

                    Presence.Type type = roster.getPresence(coreJID).getType();
                    logger.info("*** presence=" + type.toString() + " subscriptionType="
                        + entry.getType().toString());
                    String user = roster.getEntry(coreJID).getUser();
                    checkPresence(coreJID, type, user);
                    checkStatus(coreJID, roster.getEntry(coreJID).getType());
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void checkEntry(String coreJID, String name) {

        logger.info("RosterManager:checkEntry: " + coreJID + " with " + name);
        RosterEntry entry = roster.getEntry(coreJID);
        if (entry != null) {

            if ((entry.getName() == null)
                || (entry.getName() != null && !entry.getName().equalsIgnoreCase(name))) {
                logger.info("RosterManager:checkEntry updating name for entry " + coreJID + " to "
                    + name);
                try {
                    roster.createEntry(coreJID, name, null);
                } catch (XMPPException e) {
                    logger.error("RosterMangaer:createEntry error updating entry");
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void checkForCoresToRemove() {

        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            // If the entry is not in the rosterMap the remove it from the roster
            if (!rosterMap.containsKey(entry.getUser())) {
                // try {
                logger.info("RosterManager:checkForCoresToRemove removing " + entry.getUser());
                // roster.removeEntry(entry);
                removeEntry(entry.getUser());
                // }
                // catch (XMPPException e) {
                // logger.error("RosterManager:checkForCoresToRemove XMPPException: "+e.getMessage())
                // ;
                // }
            }
        }
    }

    private void checkPresence(String coreJID, Presence.Type type, String from) {

        if (type == null) {
            logger.debug(" type = null for " + coreJID);
            RosterEntry entry = roster.getEntry(from);
            if (entry.getType() == RosterPacket.ItemType.from) {
                logger.debug("     Send subscribe request " + coreJID);
                sendSubscribeRequest(coreJID);
            } else if (entry.getType() == RosterPacket.ItemType.to) {
                logger.debug("     Send subscribe request " + coreJID);
                sendSubscribeRequest(coreJID);
            }
        } else if (type == Presence.Type.subscribe) {
            logger.debug(" Send subscribed for " + coreJID);
            sendSubscribedResponse(coreJID);
        } else if (type == Presence.Type.subscribed) {
            logger.debug(" Send subscribe for " + coreJID);
            sendSubscribeRequest(coreJID);
        } else if (type == Presence.Type.unsubscribed) {
            logger.debug(" Send decline acknowledge for " + coreJID);
            sendSubscribeDeclineAcknowledge(coreJID);
        }
    }

    private void checkStatus(String coreJID, RosterPacket.ItemType type) {

        if (type == RosterPacket.ItemType.from) {
            logger.debug(" Send subscribe for " + coreJID);
            sendSubscribeRequest(coreJID);
        } else if (type == RosterPacket.ItemType.to) {
            logger.debug(" Send subscribed for " + coreJID);
            sendSubscribedResponse(coreJID);
        } else if (type == RosterPacket.ItemType.none) {
            logger.debug(" Send subscribe and subscribed for " + coreJID);
            sendSubscribeRequest(coreJID);
            sendSubscribedResponse(coreJID);
        }
    }

    private void sendSubscribeRequest(String core) {

        // // Send a roster IQ
        // RosterPacket.Item item = new RosterPacket.Item(core,core);
        // item.setItemType(RosterPacket.ItemType.both);
        // RosterPacket rp = new RosterPacket();
        // rp.addRosterItem(item);
        // rp.setTo(core);
        // connection.getConnection().sendPacket(rp);
        Presence p = new Presence(Presence.Type.subscribe);
        p.setTo(core);
        try {
            connection.sendPacketCheckWellFormed(p);
        } catch (XMPPException e) {
            logger.error("Error sending subscribe request: " + e.getMessage());
            logger.debug(p.toXML());
        }
    }

    private void sendSubscribedResponse(String core) {

        Presence p = new Presence(Presence.Type.subscribed);
        p.setTo(core);
        try {
            connection.sendPacketCheckWellFormed(p);
        } catch (XMPPException e) {
            logger.error("Error sending subscribed response: " + e.getMessage());
            logger.debug(p.toXML());
        }
    }

    private void sendSubscribeDeclinedResponse(String core) {

        Presence p = new Presence(Presence.Type.unsubscribed);
        p.setTo(core);
        try {
            connection.sendPacketCheckWellFormed(p);
        } catch (XMPPException e) {
            logger.error("Error sending subscribe declined response: " + e.getMessage());
            logger.debug(p.toXML());
        }
    }

    private void sendSubscribeDeclineAcknowledge(String core) {

        Presence p = new Presence(Presence.Type.unsubscribe);
        p.setTo(core);
        try {
            connection.sendPacketCheckWellFormed(p);
        } catch (XMPPException e) {
            logger.error("Error sending subscribe decline acknowledgement response: "
                + e.getMessage());
            logger.debug(p.toXML());
        }
    }

    /**
     * Check if the roster contains the given core. The input core name must be the exact name (case
     * sensitive) that was set in the XMPP roster that was set in the roster config service. If
     * multiple cores use different names for a core this method will fail if looking up a core name
     * from a different machine.
     * 
     * @param coreName the roster entry name for the core
     * @return true if the core is in the roster
     */
    public boolean isCoreInRoster(String coreName) {

        // get JID from core name
        String jid = getJIDFromRosterName(coreName);
        logger.info("isCoreInRoster: JID=[" + jid + "]");
        return (jid != null) ? roster.contains(jid) : false;
    }

    /**
     * Check if the given core is online. See limitations on the core name in the isCoreInRoster
     * comments.
     * 
     * @param coreName the roster entry name for the core
     * @return true if the core is available (online)
     */
    public boolean isCoreOnline(String coreName) {

        boolean found = false;
        if (isCoreInRoster(coreName)) {
            String jid = getJIDFromRosterName(coreName);
            logger.info("Presence for " + jid + " is " + roster.getPresence(jid));
            if (roster.getPresence(jid).getType() == Presence.Type.available) {
                found = true;
            }
        }
        return found;
    }

    /**
     * Print the roster
     */
    public void printRoster() {

        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            logger.info(entry.getUser() + " " + entry.getType() + " " + entry.getStatus());
        }
    }

    /**
     * Get the name for a core given the JID
     * 
     * @param coreJID interestmanager@server
     * @return
     */
    // public String getCoresRosterName(String coreJID) {
    // return roster.getEntry(coreJID).getName();
    // }
    //
    // public String getCoresRosterNameFromServerName(String coreServerName) {
    // return roster.getEntry("interestmanager@"+coreServerName).getName();
    // }
    //
    // public String getServerFromRosterName(String coreName) {
    // String server = null;
    // for (String key : rosterMap.keySet()) {
    // if (rosterMap.get(key).equalsIgnoreCase(coreName)) {
    // server = org.jivesoftware.smack.util.StringUtils.parseServer(key);
    // }
    // }
    // return server;
    // }
    public String getJIDFromRosterName(String coreName) {

        logger.info("getJIDFromRosterName for " + coreName);
        return rosterMap.inverse().get(coreName);
    }

    public String getRosterNameFromJID(String coreJID) {

        return rosterMap.get(coreJID.toLowerCase());
    }

    /**
     * Get a Map of the rosters keyed by roster name
     * 
     * @return Map with core name as key and core JID as value.
     */
    public Map<String, String> getRosterByName() {

        HashMap<String, String> map = new HashMap<String, String>();

        Set<String> cores = rosterMap.keySet();
        for (String core : cores) {
            map.put(rosterMap.get(core), core);
        }
        return map;
    }

    /**
     * Get a Map of all the cores in the roster and their status.
     * 
     * @return Map with core name as key and presence as value.
     */
    public Map<String, String> getRosterStatus() {

        HashMap<String, String> map = new HashMap<String, String>();
        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            if (entry.getUser() != null) {
                String coreJID = entry.getUser();
                String hostName = getHostNameFromJID(entry.getUser());
                if (hostName != null) {
                    logger.debug(" adding to message's map  name=" + coreJID + " status="
                        + roster.getPresence(entry.getUser()).getType().toString());
                    map.put(coreJID, roster.getPresence(entry.getUser()).getType().toString());
                } else {
                    logger.error("getRosterStatus - unable to get hostname from user's JID["
                        + entry.getUser() + "]");
                }
            }
        }
        return map;
    }

    private String getHostNameFromJID(String jid) {

        String hostName = null;
        int pos = jid.indexOf("@");
        if (pos > 0) {
            hostName = jid.substring(pos + 1);
        }
        return hostName;
    }

    public void createEntry(String JID, String name, String[] groups) {

        if (!rosterMap.containsKey(JID)) {
            logger.debug("createEntry - adding to map JID=" + JID + " name=" + name);
            rosterMap.put(JID, name);
            if (!JID.equalsIgnoreCase(connection.getJID())) {
                try {
                    roster.createEntry(JID, name, groups);
                    // Get presence of new entry
                    Presence.Type type = roster.getPresence(JID).getType();
                    RosterEntry rosterEntry = roster.getEntry(JID);
                    if (rosterEntry != null) {
                        String user = roster.getEntry(JID).getUser();
                        checkPresence(JID, type, user);
                        checkStatus(JID, roster.getEntry(JID).getType());
                    }
                } catch (XMPPException e) {
                    logger.error("RosterManager:createEntry: error creating roster entry: "
                        + e.getMessage());
                }
            }
        }
    }

    public void removeEntry(String JID) {

        RosterEntry entry = roster.getEntry(JID);
        try {
            roster.removeEntry(entry);
            if (rosterMap.containsKey(JID)) {
                rosterMap.remove(JID);
            }
        } catch (XMPPException e) {
            logger.error("RosterManager:deleteEntry: error removing roster entry: "
                + e.getMessage());
        }
    }

    public void sendCoreStatusUpdate(String fullJID, String coreStatus) {

        logger.debug("=====> RosterManager:sendCoreStatusUpdate: coreJID=" + fullJID
            + " coreStatus=" + coreStatus);

        String coreName = this.getHostNameFromJID(fullJID);
        if (coreName != null) {
            logger.info("RosterManager:sendCoreStatusUpdate - sending core status update: ["
                + fullJID + "," + coreStatus + "]");
            CoreStatusUpdateMessage msg = new CoreStatusUpdateMessage(fullJID, coreStatus);
            Message<CoreStatusUpdateMessage> update = new GenericMessage<CoreStatusUpdateMessage>(
                msg);
            if (connection.getCoreStatusUpdateChannel() != null){
            connection.getCoreStatusUpdateChannel().send(update);
            }
        } else {
            logger.error("sendCoreStatusUpdate - unable to get host name from jid=[" + fullJID
                + "]");
        }
    }

}