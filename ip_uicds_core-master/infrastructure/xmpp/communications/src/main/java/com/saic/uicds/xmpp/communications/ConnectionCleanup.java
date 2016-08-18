package com.saic.uicds.xmpp.communications;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.packet.StreamError;

public class ConnectionCleanup
    implements ConnectionListener {

    private Logger logger = Logger.getLogger(this.getClass());

    private String name = "";

    CoreConnectionImpl coreConnection;

    public ConnectionCleanup(String name, CoreConnectionImpl coreConnection) {

        this.name = name;
        this.coreConnection = coreConnection;

    }

    public void connectionClosed() {

        logger.info("ConnectionCleanup::connectionClosed => " + name);
        coreConnection.setConnected(false);
    }

    public void connectionClosedOnError(Exception e) {

        logger.error("ConnectionCleanup::connectionClosedOnError => " + name);
        if (e instanceof org.jivesoftware.smack.XMPPException) {
            org.jivesoftware.smack.XMPPException xmppEx = (org.jivesoftware.smack.XMPPException) e;
            StreamError error = xmppEx.getStreamError();

            // Make sure the error is not null
            if (error != null) {
                String reason = error.getCode();
                logger.error("ConnectionCleanup::connectionClosedOnError reason: " + reason);

                if ("conflict".equals(reason)) {
                    // on conflict smack will not automatically try to reconnect
                    coreConnection.setConnected(false);
                    logger.error("ConnectionCleanup::connectionClosedOnError - another XMPP client logged in with the core's full JID so we cannot try to reconnect.");
                    return;
                }
            }
        }
    }

    public void reconnectingIn(int arg0) {

        logger.info("ConnectionCleanup::reconnectingIn => " + name);

    }

    public void reconnectionFailed(Exception arg0) {

        logger.error("ConnectionCleanup::reconnectionFailed => " + name);

    }

    public void reconnectionSuccessful() {

        logger.info("ConnectionCleanup::reconnectionSuccessful => " + name);
        coreConnection.setConnected(true);
        coreConnection.sendHeartBeat();
        logger.info("ConnectionCleanup::reconnectionSuccessful => " + name);
        coreConnection.setConnected(true);
    }

}
