package com.saic.uicds.xmpp.communications;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;

import com.saic.uicds.core.infrastructure.messages.Core2CoreMessage;
import com.saic.uicds.xmpp.extensions.util.ArbitraryIQ;

public class Core2CoreMessageIQListener implements PacketListener {
    private Logger log = Logger.getLogger(this.getClass());

    private Pattern toCoreJIDPattern = Pattern.compile("toCoreJID=[\"'](.+?)[\"']");
    private Pattern msgTypePattern = Pattern.compile("msgType=[\"'](.+?)[\"']");
    private Pattern sendMessagePattern = Pattern.compile("<sendMessage (.+?)>(.+?)</sendMessage>",
        Pattern.DOTALL | Pattern.MULTILINE);

    private Core2CoreMessageProcessor messageProcessor;

    public Core2CoreMessageIQListener(Core2CoreMessageProcessor instance) {
        log.debug("====#> Core2CoreMessageIQListener - constructor");
        messageProcessor = instance;
    }

    @Override
    public void processPacket(Packet packet) {
        log.debug("====#> Core2CoreMessageIQListener:processPacket "); // + packet.toXML());
        if (packet instanceof ArbitraryIQ) {

            ArbitraryIQ iq = (ArbitraryIQ) packet;
            // log.debug("Got a core2coreMessage IQ: "+packet.toXML());

            String from = iq.getFrom();
            String packetId = iq.getPacketID();

            // Get initial interest group information
            String xml = iq.getChildElementXML();

            // Handle ERROR
            XMPPError error = iq.getError();
            if (error != null) {
                String coreJID = messageProcessor.getCoreConnection().getJID();
                String coreName = messageProcessor.getCoreConnection().getCoreNameFromJID(coreJID);

                log.error("Core2CoreMessageIQListener:processPacket: received an UNEXPECTED  error  message "
                	+ "from " + coreName + "xml=["
                    + xml + "]");

            } else {
                // Handle join
                Matcher m = sendMessagePattern.matcher(xml);
                if (m.find()) {
                    if (iq.getType() == IQ.Type.SET) {
                        doSendMessage(from, packetId, xml);
                    }
                    return;
                }
            }
        }
    }

    private void doSendMessage(String from, String packetId, String xml) {

//        log.debug("doSendMessage: received message=[" + xml + "] from " + from);

        String fromCore = messageProcessor.getCoreConnection().getCoreNameFromJID(from);

        String toCoreJID = "";
        Matcher m = toCoreJIDPattern.matcher(xml);
        if (m.find()) {
            toCoreJID = m.group(1);
        } else {
            log.error("Core2CoreMessageIQListener:processPacket: Can't find toCore in  message");
            return;
        }

        String msgType = "";
        m = msgTypePattern.matcher(xml);
        if (m.find()) {
            msgType = m.group(1);
        } else {
            log.error("Core2CoreMessageIQListener:processPacket: Can't find msgType in  message");
            return;
        }

        String message = null;
        m = sendMessagePattern.matcher(xml);
        if (m.find() && (m.groupCount() == 2)) {
            message = m.group(2);
            log.debug("====> message=[" + message + "]");
        } else {
            log.error("doSendMessage: unable to extract message from the received sendMessage.");
            return;
        }

        Core2CoreMessage msg = new Core2CoreMessage();
        msg.setFromCore(fromCore);
        msg.setToCore(messageProcessor.getCoreConnection().getCoreNameFromJID(toCoreJID));
        msg.setMessageType(msgType);
        msg.setMessage(message);
        Message<Core2CoreMessage> notification = (Message<Core2CoreMessage>) new GenericMessage<Core2CoreMessage>(
            msg);
        messageProcessor.getCore2CoreMessageNotificationChannel().send(notification);

    }

}
