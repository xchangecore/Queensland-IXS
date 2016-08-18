package com.saic.uicds.xmpp.communications;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;

import com.saic.uicds.core.infrastructure.messages.AgreementRosterMessage;
import com.saic.uicds.core.infrastructure.messages.Core2CoreMessage;
import com.saic.uicds.core.infrastructure.messages.CoreRosterMessage;
import com.saic.uicds.core.infrastructure.messages.DeleteInterestGroupMessage;
import com.saic.uicds.core.infrastructure.messages.InterestGroupStateNotificationMessage;
import com.saic.uicds.core.infrastructure.messages.JoinedPublishProductRequestMessage;
import com.saic.uicds.core.infrastructure.messages.NewInterestGroupCreatedMessage;
import com.saic.uicds.core.infrastructure.messages.ProductPublicationMessage;
import com.saic.uicds.core.infrastructure.messages.ProductPublicationStatusMessage;
import com.saic.uicds.core.infrastructure.messages.ShareInterestGroupMessage;
import com.saic.uicds.xmpp.communications.util.XmppUtils;
import com.saic.uicds.xmpp.extensions.core2coremessage.Core2CoreMessageIQFactory;
import com.saic.uicds.xmpp.extensions.notification.NotificationExtensionFactory;

public class CommunicationsServiceXmppImpl {

    Logger log = LoggerFactory.getLogger(CommunicationsServiceXmppImpl.class);

    private String defaultPropertiesFile;

    public String getDefaultPropertiesFile() {

        return defaultPropertiesFile;
    }

    public void setDefaultPropertiesFile(String defaultPropertiesFile) {

        this.defaultPropertiesFile = defaultPropertiesFile;
    }

    // private MessageChannel getProductRequestChannel;
    //
    // public void setGetProductRequestChannel(MessageChannel channel) {
    // getProductRequestChannel = channel;
    // }

    private MessageChannel coreRosterChannel;

    private InterestGroupManager interestGroupManager;

    private Core2CoreMessageProcessor core2CoreMessageProcessor;

    public MessageChannel getCoreRosterChannel() {

        return coreRosterChannel;
    }

    public void setCoreRosterChannel(MessageChannel coreRosterChannel) {

        this.coreRosterChannel = coreRosterChannel;
    }

    public InterestGroupManager getInterestGroupManager() {

        return interestGroupManager;
    }

    public void setInterestGroupManager(InterestGroupManager interestGroupManager) {

        this.interestGroupManager = interestGroupManager;
    }

    public Core2CoreMessageProcessor getCore2CoreMessageProcessor() {

        return core2CoreMessageProcessor;
    }

    public void setCore2CoreMessageProcessor(Core2CoreMessageProcessor core2CoreMessageProcessor) {

        this.core2CoreMessageProcessor = core2CoreMessageProcessor;
    }

    @PostConstruct
    public void initialize() {

        log.info("CommunicationsServiceXmppImpl::init - started");
        // assert (getProductRequestChannel != null);
        assert (interestGroupManager != null);
        assert (core2CoreMessageProcessor != null);
        log.info("CommunicationsServiceXmppImpl::init - completed");
    }

    /**
     * Receives notification that a new interest group has been created.
     * 
     * @param NewInterestGroupCreatedMessage
     */
    public void newInterestGroupCreatedHandler(NewInterestGroupCreatedMessage message) {

        log.debug("===========> *** newInterestGroupCreatedHandler -  interestGroupID="
            + message.getInterestGroupID() + " interestGroupType=" + message.getInterestGroupType());

        InterestGroup interestGroup = new InterestGroup();
        interestGroup.interestGroupID = message.getInterestGroupID();
        interestGroup.interestGroupType = message.getInterestGroupType();
        interestGroup.interestGroupOwner = message.getOwningCore();
        interestGroup.workProductTypes = message.getJoinedWPTYpes();
        interestGroup.interestGroupPubsubService = XmppUtils.getPubsubServiceFromJID("pubsub",
            message.getOwningCore());

        if (!interestGroupManager.interestGroupExists(interestGroup.interestGroupID)) {
            if (message.restored) {
                log.debug("newInterestGroupCreatedHandler:  ===>sharingStatus="
                    + message.sharingStatus);
                if (message.sharingStatus.equals(InterestGroupStateNotificationMessage.SharingStatus.Joined.toString())) {
                    interestGroupManager.restoreJoinedInterestGroup(interestGroup,
                        message.getOwnerProperties());
                } else if (message.sharingStatus.equals(InterestGroupStateNotificationMessage.SharingStatus.Shared.toString())) {
                    interestGroupManager.restoreSharedInterestGroup(interestGroup,
                        message.getSharedCoreList());
                } else {
                    interestGroupManager.restoreOwnedInterestGroup(interestGroup);
                }
            } else {
                interestGroupManager.createInterestGroup(interestGroup);
            }
        }
    }

    /**
     * Receives notification that a new interest group has been deleted.
     * 
     * @param NewInterestGroupCreatedMessage
     */
    public void deleteInterestGroupHandler(DeleteInterestGroupMessage message) {

        log.debug("deleteInterestGroupHandler - interestGroupID=" + message.getInterestGroupID());
        interestGroupManager.deleteInterestGroup(message.getInterestGroupID());

    }

    /**
     * Receives message GetProductResponseMessage on the getProductResponseChannel Handler Spring
     * Integration message channel from the Communications Service.
     * 
     * @param message Work product response message (GetProductResponseMessage)
     * @throws ApplicationException
     */
    public void productPublicationHandler(ProductPublicationMessage message) {

        ProductPublicationMessage.PublicationType pubType = message.getPubType();
        String interestGroupID = message.getInterestGroupID();
        String wpID = message.getProductID();
        String wpType = message.getProductType();
        String wp = message.getProduct();

        log.debug("productPublicationHandler: interestGroupID=" + interestGroupID + " wpID=" + wpID
            + " wpType=" + wpType + " pubType=" + pubType);

        if (pubType.equals(ProductPublicationMessage.PublicationType.Publish)) {
            interestGroupManager.publishWorkProduct(interestGroupID, wpID, wpType, wp);
        } else if (pubType.equals(ProductPublicationMessage.PublicationType.Delete)) {
            interestGroupManager.deleteWorkProduct(wpID, wpType, interestGroupID);
        }
    }

    public void shareInterestGroupHandler(ShareInterestGroupMessage message)
        throws IllegalArgumentException, IllegalStateException {

        String interestGroupID = message.getInterestGroupID();
        String remoteCore = message.getRemoteCore();
        String interestGroupInfo = message.getInterestGroupInfo();
        List<String> workProductTypesToShare = message.getWorkProductTypesToShare();

        // Don't let exceptions get thrown back to the message channel
        // try {
        interestGroupManager.shareInterestGroup(interestGroupID, remoteCore, interestGroupInfo,
            workProductTypesToShare);
        // } catch (IllegalStateException e) {
        // log.error(e.getMessage());
        // }
    }

    // public String getWorkProduct(String interestGroupID, String wpID) {
    // return interestGroupManager.getWorkProduct(interestGroupID, wpID);
    // }

    public void systemInitializedHandler(String message) {

        log.info("=====> CommunicationsServiceXmppImpl:systemInitializedHandler - sending core roster");
        sendCoreRoster();
        log.debug("=====> CommunicationsServiceXmppImpl:systemInitializedHandler - core roster sent");
    }

    public void sendCoreRoster() {

        log.debug("=====> CommunicationsServiceXmppImpl:sendCoreRoster");

        Map<String, String> rosterStatusMap = new HashMap<String, String>();
        rosterStatusMap = interestGroupManager.getCoreConnection().getRosterStatus();

        // add ourself
        rosterStatusMap.put(interestGroupManager.getCoreConnection().getJID(), "available");

        CoreRosterMessage msg = new CoreRosterMessage(rosterStatusMap);
        Message<CoreRosterMessage> response = new GenericMessage<CoreRosterMessage>(msg);
        coreRosterChannel.send(response);
    }

    public void agreementRosterHandler(AgreementRosterMessage message) {

        log.info("=====> CommunicationsServiceXmppImpl:agreementRosterHandler - received roster with numberOfCores="
            + message.getCores().size());
        Map<String, AgreementRosterMessage.State> cores = message.getCores();

        Set<String> keys = cores.keySet();

        for (String key : keys) {
            log.info("*************** core:" + key + " state:" + cores.get(key));
            Pattern pattern = Pattern.compile("(.*)@(.*)");
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                log.info("******************* JID=" + key + " userName=" + matcher.group(1)
                    + " hostName=" + matcher.group(2));
                try {
                    switch (cores.get(key)) {
                    case CREATE:
                        interestGroupManager.getCoreConnection().addRosterEntry(key, key);
                        // interestGroupManager.getCoreConnection().addRosterEntry(key,
                        // matcher.group(2));
                        break;
                    case AMEND:
                        log.debug("AMEND roster handled");
                        break;
                    case RESCIND:
                        interestGroupManager.getCoreConnection().deleteRosterEntry(key);
                    }

                } catch (Exception e) {
                    log.info("Error: Caught exception while attempting to add roster entry for "
                        + matcher.group(2));
                    e.printStackTrace();
                }
            } else {
                log.error("******* Receiving invalid JID in agreementRosterHandler message - JID="
                    + key + "   expected format[userName@hostName]");
            }

        }
    }

    public void core2CoreMessageHandler(Core2CoreMessage message) {

        log.debug("=====> core2CoreMessageHandler - received message intended for core "
            + message.getToCore());

        if (message.getMessageType() == null) {
            log.error("received null message type to send: not sending");
            return;
        }

        if (message.getMessageType().equals("XMPP_MESSAGE")) {

            org.jivesoftware.smack.packet.Message msg = NotificationExtensionFactory.createNotificationMessage(
                message.getToCore(), message.getBody(), message.getXhtml(), message.getMessage());
            // msg.setTo(message.getToCore());
            // msg.addBody(null, message.getMessage());
            try {
                interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(msg);
            } catch (XMPPException e) {
                log.error("Error sending XMPP message: " + e.getMessage());
            }
            return;
        }

        String remoteCoreJID = interestGroupManager.getCoreConnection().getJIDFromCoreName(
            message.getToCore());
        String remoteCoreJIDWithResource = interestGroupManager.getCoreConnection().getJIDPlusResourceFromCoreName(
            message.getToCore());

        IQ msg = Core2CoreMessageIQFactory.createCore2CoreMessage(message.getMessage(),
            message.getMessageType(), remoteCoreJID, remoteCoreJIDWithResource);

        log.debug(msg.toXML());

        try {
            interestGroupManager.getCoreConnection().sendPacketCheckWellFormed(msg);
        } catch (XMPPException e) {
            log.error("Error sending core 2 core message: " + e.getMessage());
        }
    }

    public void joinedPublishProductRequestHandler(JoinedPublishProductRequestMessage message) {

        String interestGroupID = message.getInterestGroupId();
        String owningCore = message.getOwningCore();
        String productId = message.getProductId();
        String productType = message.getProductType();
        String act = message.getAct();
        String product = message.getWorkProduct();
        String userID = message.getUserID();

        log.debug("joinedPublishProductRequestHandler: act=" + act + "interestGroupID="
            + interestGroupID + " owningCore=" + owningCore + " productId=" + productId
            + " productType=" + productType + " userID=" + userID);

        // send XEP message to the owning core's CommunicationXmppImpl
        interestGroupManager.requestJoinedPublishProduct(interestGroupID, owningCore, productId,
            productType, act, userID, product);

    }

    // This method is handled in the owning core
    // This method handles the status of a pending publication request. The status is transmitted
    // back to the requesting core via the XMPP connection
    public void productPublicationStatusHandler(ProductPublicationStatusMessage message) {

        String userID = message.getUserID();
        String requestingCore = message.getRequestingCore();
        String status = message.getStatus();

        log.debug("productPublicationStatusHandler: userID=" + userID + " requestingCore="
            + requestingCore + " status=[" + status + "]");

        interestGroupManager.sendProductPublicationStatus(requestingCore, userID, status);
    }
}