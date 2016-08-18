package com.saic.uicds.xmpp.communications;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.XMPPError;

import com.saic.uicds.xmpp.extensions.util.ArbitraryIQ;

public class CommandWithReplyImpl
    implements CommandWithReply {

    private XmppConnection connection;
    private String packetID;
    private String errorMessage;
    private XMPPError xmppError;
    private XMPPError.Type errorType;
    private String errorCondition;
    private int errorCode;
    private boolean unsupported = false;
    private boolean payloadTooBig = false;
    private boolean invalidPayload = false;
    private boolean itemRequired = false;
    private boolean payloadRequired = false;
    private boolean itemForbidden = false;
    private boolean invalidJID = false;
    private PacketCollector collector;
    private String subscriptionID;
    IQ result;
    private Pattern subidPattern;

    public CommandWithReplyImpl(XmppConnection con, Packet packet) {

        connection = con;
        packetID = packet.getPacketID();

        // Setup a collector to receive the servers response
        collector = connection.createPacketCollector(new PacketIDFilter(packetID));

        // Get the subscription id if applicable
        subscriptionID = null;
        subidPattern = Pattern.compile("subid=[\"']([\\w-]+?)[\"']");

        // Set the results from the command to null
        result = null;

        if (connection.isConnected()) {
            try {
                connection.sendPacket(packet);
            } catch (IllegalStateException e) {
                // set result to an error
                result = new ArbitraryIQ();
                result.setType(IQ.Type.ERROR);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#waitForSuccessOrFailure()
     */
    public boolean waitForSuccessOrFailure() {

        boolean success = true;

        if (connection.isConnected() && result == null) {
            int seconds = connection.getWaitTimeInSeconds() * 1000;
            // System.out.println("+++++ wait " + connection.getWaitTimeInSeconds() +
            // " seconds +++++");
            // System.out.println("Waiting for "+packetID);

            result = (IQ) collector.nextResult(seconds);

            // Stop queuing results
            collector.cancel();

            if (result == null) {
                errorMessage = "No response from the server.";
                success = false;
            } else if (result.getType() == IQ.Type.ERROR) {
                xmppError = result.getError();
                if (result.getError() != null) {
                    errorMessage = result.getError().getMessage();
                    errorCondition = result.getError().getCondition();
                    errorType = result.getError().getType();
                    errorCode = result.getError().getCode();
                } else {
                    errorMessage = null;
                    errorCondition = "";
                    errorCode = 0;
                    errorType = XMPPError.Type.CANCEL;
                }
                if (errorMessage == null) {
                    errorMessage = "NULL error message";
                }
                if (result.getError() != null) {
                    PacketExtension pe = null;
                    pe = result.getError().getExtension("unsupported",
                        "http://jabber.org/protocol/pubsub#errors");
                    if (pe != null) {
                        unsupported = true;
                        errorMessage += " - (action is unsupported by the server)";
                    }
                    pe = result.getError().getExtension("payload-too-big",
                        "http://jabber.org/protocol/pubsub#errors");
                    if (pe != null) {
                        payloadTooBig = true;
                        errorMessage += " - (payload is too big)";
                    }
                    pe = result.getError().getExtension("invalid-payload",
                        "http://jabber.org/protocol/pubsub#errors");
                    if (pe != null) {
                        invalidPayload = true;
                        errorMessage += " - (invalid payload)";
                    }
                    pe = result.getError().getExtension("item-required",
                        "http://jabber.org/protocol/pubsub#errors");
                    if (pe != null) {
                        itemRequired = true;
                        errorMessage += " - (item element is required for this node)";
                    }
                    pe = result.getError().getExtension("payload-required",
                        "http://jabber.org/protocol/pubsub#errors");
                    if (pe != null) {
                        payloadRequired = true;
                        errorMessage += " - (payload is required for this node)";
                    }
                    pe = result.getError().getExtension("item-forbidden",
                        "http://jabber.org/protocol/pubsub#errors");
                    if (pe != null) {
                        itemForbidden = true;
                        errorMessage += " - (cannot publish item to transient node)";
                    }
                    pe = result.getError().getExtension("invalid-jid",
                        "http://jabber.org/protocol/pubsub#errors");
                    if (pe != null) {
                        invalidJID = true;
                        errorMessage += " - (the bare JID portions of the JIDs do not match)";
                    }
                }

                success = false;
            } else {
                // Pull out the subscription id if applicable
                Matcher m = subidPattern.matcher(result.toXML());
                if (m.find()) {
                    subscriptionID = m.group(1);
                    // System.out.println("GOT subid " + subscriptionID);
                } else {
                    // System.err.println("No subid in " + result.toXML());
                }
                // Collection<String> exts = result.getPropertyNames();
                // System.out.println(exts.size()+" EXTENSIONS in "+result.getType());
                // for (Object o : exts) {
                // String ex = (String)o;
                // if (ex != null) {
                // System.out.println("FOUND "+ex);
                // }
                // }
            }
        } else {
            success = false;
            // Stop queuing results
            collector.cancel();
        }

        return success;
    }

    // public Packet waitForResponse() {
    // }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#getErrorMessage()
     */
    public String getErrorMessage() {

        return errorMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#getErrorCode()
     */
    public int getErrorCode() {

        return errorCode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#getErrorCondition()
     */
    public String getErrorCondition() {

        return errorCondition;
    }

    /* (non-Javadoc)
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#getXMPPError()
     */
    public XMPPError getXMPPError() {

        return xmppError;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#getErrorType()
     */
    public XMPPError.Type getErrorType() {

        return errorType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#getResult()
     */
    public IQ getResult() {

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#getSubscriptionID()
     */
    public String getSubscriptionID() {

        return subscriptionID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#isInvalidPayload()
     */
    public boolean isInvalidPayload() {

        return invalidPayload;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#isItemForbidden()
     */
    public boolean isItemForbidden() {

        return itemForbidden;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#isItemRequired()
     */
    public boolean isItemRequired() {

        return itemRequired;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#isPayloadRequired()
     */
    public boolean isPayloadRequired() {

        return payloadRequired;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#isPayloadTooBig()
     */
    public boolean isPayloadTooBig() {

        return payloadTooBig;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#isUnsupported()
     */
    public boolean isUnsupported() {

        return unsupported;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saic.uicds.xmpp.communications.CommandWithReply#isInvalidJID()
     */
    public boolean isInvalidJID() {

        return invalidJID;
    }
}
