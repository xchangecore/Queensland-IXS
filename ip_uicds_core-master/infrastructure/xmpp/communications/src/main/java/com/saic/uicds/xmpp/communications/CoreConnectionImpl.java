package com.saic.uicds.xmpp.communications;

import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.springframework.integration.core.MessageChannel;

import com.saic.uicds.xmpp.communications.util.XmppUtils;

/**
 * This class provides an interface to an XMPP connection. This class is configured by a Spring
 * bean.
 * 
 * <br />
 * <b>Todo:</b>
 * <ul>
 * <li>documenation - continue documenting</li>
 * </ul>
 * 
 * <pre>
 * CoreConnection con = new CoreConnection();
 * 
 * </pre>
 * 
 * @see org.jivesoftware.smack.XMPPConnection
 * 
 */

public class CoreConnectionImpl
    implements CoreConnection {

    public static final int BAD_FORMAT_CODE = 400;
    public static final String NOT_WELLFORMED_MSG = "Packet XML was not well-formed";
    public static final String BAD_FORMAT_CONDITION = "bad-format";
    protected Properties connectionProperties = null;

    // Properties
    private String debug = "false";
    private String name = "Test Client";
    private String server = null;
    private String servername = null;
    private String port = "5222";
    private String username = null;
    private String password = null;
    private String resource = "test";
    private String pubsubsvc = "pubsub";
    private String jid = null;
    private String jidPlusResource = null;
    private String interestGroupRoot = "/interestGroup";
    private int waitTimeInSeconds = 5;

    // privates
    private ConnectionConfiguration config = null;
    protected XMPPConnection xmppConnection = null;
    private ServiceDiscoveryManager discoManager;
    private RosterManager rosterManager;

    private Logger logger = Logger.getLogger(this.getClass());

    // Manager to handle file transfers for the interest group
    InterestGroupFileManager fileManager = null;

    boolean connected = false;

    private MessageChannel coreStatusUpdateChannel;

    public MessageChannel getCoreStatusUpdateChannel() {

        return coreStatusUpdateChannel;
    }

    public void setCoreStatusUpdateChannel(MessageChannel coreStatusUpdateChannel) {

        this.coreStatusUpdateChannel = coreStatusUpdateChannel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#isConnected()
     */
    public boolean isConnected() {

        return connected;
    }

    public void setConnected(boolean connected) {

        this.connected = connected;
    }

    /**
     * Constructor - use default and roster properties files, defined in the applicationContext, for
     * configuration parameters.
     */
    @PostConstruct
    @Override
    public void initialize() {

        logger.info("CoreConnectionImpl.initilazed called: username: " + username);
        // Configure and connect;
        assert (coreStatusUpdateChannel != null);
        configure();
        connect();
        logger.info("CoreConnectionImpl.initilazed done");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#configure()
     */
    public void configure() {

        try {
            // Set the JID for this connection
            if (this.servername != null) {
                jid = this.username + "@" + this.servername;
            } else {
                jid = this.username + "@" + this.server;
            }

        } catch (Exception e) {
            logger.error("An error occurred reading the properties file.");
            System.exit(0);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#configure()
     */
    public void configureOwnerConnection(String coreName) {

        configure();

        // JID core connection to remote core = ownerCoreJID + "/" + joiningCoreName
        // e.g. joiningCoreName = "Core2", owningCoreName = "Core1", user = "uicds"
        // ===> JID = "uocds@Core1/Core2"
        jid += "/" + coreName;
        setResource(coreName);

    }

    private void loadProperties() {

        // set the property members for the connection
        setDebug(connectionProperties.getProperty("debug"));
        setName(connectionProperties.getProperty("name"));
        setServer(connectionProperties.getProperty("server"));
        setServername(connectionProperties.getProperty("servername"));
        setPort(connectionProperties.getProperty("port"));
        setUsername(connectionProperties.getProperty("username"));
        setPassword(connectionProperties.getProperty("password"));
        setResource(connectionProperties.getProperty("resource"));
        setPubSubSvc(connectionProperties.getProperty("pubsubsvc"));
        setInterestGroupRoot(connectionProperties.getProperty("root", "/interestGroup"));
    }

    public CoreConnectionImpl() {

    }

    public CoreConnectionImpl(Properties ownerConnectionProps, String coreName) {

        // This constructor is called to create a xmpp connection to a remote core

        // Set the properties
        this.connectionProperties = ownerConnectionProps;

        // Load the properties, configure, and connect
        loadProperties();

        // configure and connect
        configureOwnerConnection(coreName);
        connect();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#connect()
     */
    public void connect() {

        if (!connected) {
            try {
                // connect and login to server
                logger.info("Attempting to connect: " + getName() + "\n");
                logger.info("     server: " + this.getServer());
                logger.info("       port: " + this.getPort());
                logger.info("   username: " + this.getUsername());
                logger.info("   resource: " + this.getResource());

                XMPPConnection.DEBUG_ENABLED = this.getDebugBoolean();

                config = new ConnectionConfiguration(this.getServer(), this.getPortInt());

                // If xmppConnection is not created yet then create one. This may have been
                // configured via Spring or during testing setup.
                xmppConnection = new XMPPConnection(config);

                xmppConnection.connect();
                // logger.info("===> login as userName=" + this.getUsername() + " passwd=" +
                // this.getPassword() + " resource=" + this.getResource());
                xmppConnection.login(this.getUsername(), this.getPassword(), this.getResource());
                connected = true;

                // Set my resource priority
                setResourcePriority();

                // Create the roster manager
                logger.info("Instantiating RosterManger");
                rosterManager = new RosterManager(this);

                // Set the name of this connection to the roster name for this core
                if (rosterManager.getRosterNameFromJID(getJID()) != null) {
                    logger.info("====> Set connection name to "
                        + rosterManager.getRosterNameFromJID(getJID()));
                    setName(rosterManager.getRosterNameFromJID(getJID()));
                    logger.debug("Adding ourself: JID=" + getJID() + " name="
                        + rosterManager.getRosterNameFromJID(getJID()) + " to roster");
                    rosterManager.createEntry(getJID(),
                        rosterManager.getRosterNameFromJID(getJID()), null);
                }

                // Two different attempts to get things to shutdown cleanly
                // neither seems to work correctly, each call to disconnect hangs
                xmppConnection.addConnectionListener(new ConnectionCleanup(this.getServer(), this));

                // Obtain the ServiceDiscoveryManager associated with my XMPPConnection
                discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

                fileManager = new InterestGroupFileManager(this);
            } catch (XMPPException e) {
                e.printStackTrace();
                logger.error("CoreConnection XMPPException connecting: " + e);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("CoreConnection Exception connecting: " + e.getMessage() + " "
                    + e.toString());
                return;
            }
        }
    }

    //
    // /**
    // * Return the actual XMPP connection from the Smack library.
    // *
    // * @return connected instance of org.jivesoftware.smack.XMPPConnection
    // */
    // public XMPPConnection getConnection() {
    // return this.con;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#disconnect()
     */
    public void disconnect() {

        logger.info("CoreConnection.disconnect");
        if (xmppConnection == null) {
            logger.error("null connection at disconnect");
            return;
        }
        if (xmppConnection.isConnected()) {
            // TODO: might add a disconnect listener interface to and have the InterestManagement
            // class register and then do this unsubscribeAll if we really need to do this
            // interestManager.unsubscribeAll();
            xmppConnection.disconnect();
            connected = xmppConnection.isConnected();
        }
        // else already disconnected so cannot clean up
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.CoreConnection#sendPacketCheckWellFormed(org.jivesoftware.smack.packet.Packet)
     */
    public void sendPacketCheckWellFormed(Packet packet) throws XMPPException {

        checkForWellFormedPacket(packet);

        sendPacket(packet);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#sendPacket(org.jivesoftware.smack.
     * packet.Packet)
     */
    public void sendPacket(Packet packet) {

        xmppConnection.sendPacket(packet);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getConfiguration()
     */
    public ConnectionConfiguration getConfiguration() {

        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getFileManager()
     */
    public InterestGroupFileManager getFileManager() {

        return fileManager;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.XmppConnection#createCommandWithReply(org.jivesoftware.smack.packet.Packet)
     */
    public CommandWithReply createCommandWithReply(Packet packet) throws XMPPException {

        checkForWellFormedPacket(packet);

        return new CommandWithReplyImpl(this, packet);
    }

    private void checkForWellFormedPacket(Packet packet) throws XMPPException {

        // Throw an exception if the packet XML is not well formed
        if (!XmppUtils.isWellFormed(packet.toXML())) {
            XMPPError error = new XMPPError(BAD_FORMAT_CODE, XMPPError.Type.MODIFY,
                BAD_FORMAT_CONDITION, NOT_WELLFORMED_MSG, null);
            throw new XMPPException(error);
        }
    }

    protected void setResourcePriority() {

        xmppConnection.sendPacket(new Presence(Presence.Type.available, null, 77,
            Presence.Mode.available));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#addPacketListener(org.jivesoftware
     * .smack.PacketListener, org.jivesoftware.smack.filter.PacketFilter)
     */
    public void addPacketListener(PacketListener listener, PacketFilter packetFilter) {

        xmppConnection.addPacketListener(listener, packetFilter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#removePacketListener(org.jivesoftware
     * .smack.PacketListener)
     */
    public void removePacketListener(PacketListener listener) {

        xmppConnection.removePacketListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#createPacketCollector(org.jivesoftware
     * .smack.filter.PacketFilter)
     */
    public PacketCollector createPacketCollector(PacketFilter packetFilter) {

        return xmppConnection.createPacketCollector(packetFilter);
    }

    FileTransferManager getFileTransferManager() {

        return new FileTransferManager(xmppConnection);
    }

    Roster getRoster() {

        return xmppConnection.getRoster();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#addRosterEntry(java.lang.String,
     * java.lang.String)
     */
    public void addRosterEntry(String coreJID, String name) {

        String[] groups = null;
        rosterManager.createEntry(coreJID, name, groups);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#deleteRosterEntry(java.lang.String)
     */
    public void deleteRosterEntry(String coreJID) {

        rosterManager.removeEntry(coreJID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getDebug()
     */
    public String getDebug() {

        return this.debug;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getDebugBoolean()
     */
    public boolean getDebugBoolean() {

        return new Boolean(this.debug).booleanValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setDebug(java.lang.String)
     */
    public void setDebug(String value) {

        if (value != null && !value.equals(null))
            this.debug = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getName()
     */
    public String getName() {

        return this.name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setName(java.lang.String)
     */
    public void setName(String value) {

        if (value != null && !value.equals(null))
            this.name = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getServer()
     */
    public String getServer() {

        return this.server;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setServer(java.lang.String)
     */
    public void setServer(String value) {

        if (value != null && !value.equals(null)) {
            this.server = replaceHostname(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getServername()
     */
    public String getServername() {

        return this.servername;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setServername(java.lang.String)
     */
    public void setServername(String value) {

        if (!value.equals(null)) {

            this.servername = replaceHostname(value);

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getPort()
     */
    public String getPort() {

        return this.port;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getPortInt()
     */
    public int getPortInt() {

        return new Integer(this.port).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setPort(java.lang.String)
     */
    public void setPort(String value) {

        if (value != null && !value.equals(null))
            this.port = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getUsername()
     */
    public String getUsername() {

        return this.username;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setUsername(java.lang.String)
     */
    public void setUsername(String value) {

        if (value != null && !value.equals(null))
            this.username = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getJID()
     */
    public String getJID() {

        // logger.info("CoreConnectionImpl.getJID: jid=" + jid);
        return jid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getJIDPlusResource()
     */
    public String getJIDPlusResource() {

        return jidPlusResource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getPassword()
     */
    public String getPassword() {

        return this.password;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setPassword(java.lang.String)
     */
    public void setPassword(String value) {

        if (value != null && !value.equals(null))
            this.password = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getResource()
     */
    public String getResource() {

        return this.resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setResource(java.lang.String)
     */
    public void setResource(String value) {

        if (value != null && !value.equals(null)) {
            this.resource = value;
            if (this.servername != null) {
                jidPlusResource = this.username + "@" + this.servername;
            } else {
                jidPlusResource = this.username + "@" + this.server;
            }
            if (this.resource != null && this.resource != "") {
                jidPlusResource += "/" + this.resource;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getPubSubSvc()
     */
    public String getPubSubSvc() {

        return this.pubsubsvc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#setPubSubSvc(java.lang.String)
     */
    public void setPubSubSvc(String value) {

        if (value != null && !value.equals(null)) {
            this.pubsubsvc = replaceHostname(value);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getInterestGroupRoot()
     */
    public String getInterestGroupRoot() {

        return interestGroupRoot;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#setInterestGroupGroupRoot(java.lang
     * .String)
     */
    public void setInterestGroupRoot(String interestGroupRoot) {

        this.interestGroupRoot = interestGroupRoot;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#isCoreOnline(java.lang.String)
     */
    public boolean isCoreOnline(String coreName) {

        if (coreName == null) {
            return false;
        }
        return rosterManager.isCoreOnline(coreName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#isCoreInRoster(java.lang.String)
     */
    public boolean isCoreInRoster(String coreName) {

        return rosterManager.isCoreInRoster(coreName);
    }

    public void checkRoster() {

        rosterManager.checkRoster();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getRosterByName()
     */
    public Map<String, String> getRosterByName() {

        return rosterManager.getRosterByName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.dctd.uicds.xmpp.communications.CoreConnection#getRosterStatus()
     */
    public Map<String, String> getRosterStatus() {

        return rosterManager.getRosterStatus();
    }

    // public String getServerFromRosterName(String coreName) {
    // return rosterManager.getServerFromRosterName(coreName);
    // }
    //
    // public String getJIDFromRosterName(String coreName) {
    // return rosterManager.getJIDFromRosterName(coreName);
    // }
    //
    // public String getCoresRosterNameFromServerName(String coreServer) {
    // return rosterManager.getCoresRosterNameFromServerName(coreServer);
    // }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#getJIDFromCoreName(java.lang.String)
     */
    public String getJIDFromCoreName(String coreName) {

        return rosterManager.getJIDFromRosterName(coreName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#getJIDFromCoreName(java.lang.String)
     */
    public String getJIDPlusResourceFromCoreName(String coreName) {

        return rosterManager.getJIDFromRosterName(coreName) + "/" + this.resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.saic.dctd.uicds.xmpp.communications.CoreConnection#getCoreNameFromJID(java.lang.String)
     */
    public String getCoreNameFromJID(String coreJID) {

        return rosterManager.getRosterNameFromJID((org.jivesoftware.smack.util.StringUtils.parseBareAddress(coreJID)).toLowerCase());
    }

    /*
     * public void sendJoinMessage(String coreName, InterestGroup interestGroup) {
     * interestManager.sendJoinMessage(rosterManager.getJIDFromRosterName(coreName), interestGroup);
     * }
     * 
     * public void sendResignMessage(String coreName, InterestGroup interestGroup) {
     * interestManager.sendResignMessage(rosterManager.getJIDFromRosterName(coreName),
     * interestGroup); }
     * 
     * public void transferFileToCore(String coreName, String fileName) {
     * fileManager.transferFileToCore(rosterManager.getJIDFromRosterName(coreName) + "/manager",
     * fileName); }
     * 
     * public void sendResignRequestMessage(String coreName, InterestGroup interestGroup) {
     * interestManager.sendResignRequestMessage(rosterManager.getJIDFromRosterName(coreName),
     * interestGroup); }
     */

    private String replaceHostname(String value) {

        String localhost = "";
        try {
            localhost = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value.replace("localhost", localhost);
    }

    @Override
    public DiscoverInfo discoverNodeInfo(String node) {

        if (discoManager != null) {
            try {
                return discoManager.discoverInfo(pubsubsvc, node);
            } catch (XMPPException e) {
                XMPPError err = e.getXMPPError();
                if (err != null && err.getCode() == 404) {
                    if (!isConnected()) {
                        logger.error("XMPP Server not found (not connected)");
                    }
                } else {
                    logger.error("discovering info for node " + node);
                    if (err != null) {
                        logger.error("  message: " + err.getMessage());
                        logger.error("     code: " + err.getCode());
                        logger.error("     type: " + err.getType());
                    } else {
                        logger.error("  null XMPP error message");
                    }

                }
            }
        }
        return null;
    }

    @Override
    public DiscoverItems discoverNodeItems(String node) {

        if (discoManager != null) {
            try {
                return discoManager.discoverItems(pubsubsvc, node);
            } catch (XMPPException e) {
                XMPPError err = e.getXMPPError();
                if (err != null && err.getCode() == 404) {
                    if (!isConnected()) {
                        logger.error("XMPP Server not found (not connected)");
                    } else {
                        logger.error("XMPP Server not found error. pubsub."
                            + xmppConnection.getHost() + " may not be resolvable");
                    }
                } else {
                    logger.error("discovering items for node: " + node);
                    if (err != null) {
                        logger.error("  message: " + err.getMessage());
                        logger.error("     code: " + err.getCode());
                        logger.error("     type: " + err.getType());
                    } else {
                        logger.error(" null XMPP error message");
                    }

                }
            }
        }
        return null;
    }

    public void setWaitTimeInSeconds(int waitTimeInSeconds) {

        this.waitTimeInSeconds = waitTimeInSeconds;
    }

    @Override
    public int getWaitTimeInSeconds() {

        return this.waitTimeInSeconds;
    }

    public void sendHeartBeat() {

        if (this.isConnected()) {
            try {
                // send a heartbeat to remote cores by updating our presence
                Presence presence = new Presence(Type.available, "Online", 50, Mode.available);
                this.sendPacket(presence);
            } catch (Exception e) {
                if (e instanceof XMPPException) {
                    XMPPException xe = (XMPPException) e;
                    logger.error("XMPPException sending heartbeat: " + xe.getMessage());
                    XMPPError error = xe.getXMPPError();
                    logger.error("XMPP Error Message  : " + error.getMessage());
                    logger.error("XMPP Error Condition: " + error.getCondition());
                    logger.error("XMPP Error Type     : " + error.getType());
                    logger.error("XMPP Error Code     : " + error.getCode());

                } else {
                    logger.error("Exception sending heartbeat: " + e.getMessage());
                }
            }
        }
    }

}
