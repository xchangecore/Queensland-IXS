package com.saic.uicds.xmpp.communications;

import java.util.Map;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;

public interface CoreConnection
    extends XmppConnection {

    public void initialize();

    /**
     * Internal method to configure the connection with a properties file.
     * 
     * @param propsFile properties file to use for configuration values
     */
    public abstract void configure();

    public abstract ConnectionConfiguration getConfiguration();

    public abstract InterestGroupFileManager getFileManager();

    public abstract String getDebug();

    public abstract boolean getDebugBoolean();

    public abstract void setDebug(String value);

    public abstract String getName();

    public abstract void setName(String value);

    public abstract String getServer();

    public abstract void setServer(String value);

    public abstract String getServername();

    public abstract void setServername(String value);

    public abstract String getPort();

    public abstract int getPortInt();

    public abstract void setPort(String value);

    public abstract String getUsername();

    public abstract void setUsername(String value);

    public abstract String getJIDPlusResource();

    public abstract String getPassword();

    public abstract void setPassword(String value);

    public abstract String getResource();

    public abstract void setResource(String value);

    public abstract String getInterestGroupRoot();

    public abstract void setInterestGroupRoot(String interestGroupRoot);

    public abstract boolean isCoreOnline(String coreName);

    public abstract boolean isCoreInRoster(String coreName);

    public abstract void checkRoster();

    public abstract Map<String, String> getRosterByName();

    public abstract Map<String, String> getRosterStatus();

    public abstract String getJIDFromCoreName(String coreName);

    public abstract String getJIDPlusResourceFromCoreName(String coreName);

    public abstract String getCoreNameFromJID(String coreJID);

    public abstract void sendPacketCheckWellFormed(Packet packet) throws XMPPException;

    public abstract void sendHeartBeat();

}