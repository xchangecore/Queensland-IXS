package com.saic.uicds.core.em.service.impl;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.broadcastService.BroadcastMessageRequestDocument;
import org.uicds.directoryServiceData.WorkProductTypeListType;

import x0.oasisNamesTcEmergencyEDXLDE1.ContentObjectType;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument;
import x0.oasisNamesTcEmergencyEDXLDE1.ValueSchemeType;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;

import com.saic.uicds.core.em.endpoint.BroadcastServiceEndpoint;
import com.saic.uicds.core.em.exceptions.SendMessageErrorException;
import com.saic.uicds.core.em.service.BroadcastService;
import com.saic.uicds.core.em.util.BroadcastUtil;
import com.saic.uicds.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.saic.uicds.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnavailableException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnknownException;
import com.saic.uicds.core.infrastructure.messages.Core2CoreMessage;
import com.saic.uicds.core.infrastructure.service.CommunicationsService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.NotificationService;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.UicdsStringUtil;

/**
 * The broadcast service implementation distributes the message by the following process:
 * <ul>
 * <li>iterates through all the explicitAddress elements
 * <li>finds the cores that need to receive this message
 * <li>sends this message to the broadcast service on those cores through the communications service
 * </ul>
 * 
 * This service also receives messages sent from broadcast services hosted on other cores and:
 * <ul>
 * <li>iterates through all the explictAddress elements
 * <li>delivers this message to Notification endpoint of the explictAddresses that are are on this
 * core
 * </ul>
 * 
 * @version UICDS - alpha
 * @author roberta
 * @author Andre Bonner
 * @ssdd
 */
public class BroadcastServiceImpl implements BroadcastService, ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(BroadcastServiceEndpoint.class);

    CommunicationsService communicationsService;

    private DirectoryService directoryService;
    
    static final String NEW_LINE = System.getProperty("line.separator");

    public void setDirectoryService(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    private NotificationService notificationService;

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Parses a broadcast request document for and edxl distribution message and sends the edxl
     * document message to all referenced cores
     * 
     * @param message the broadcast request document
     * 
     * @exception BroadcastFault
     * @ssdd
     */
    @Override
    public void broadcastMessage(BroadcastMessageRequestDocument message)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException, NoShareAgreementException, NoShareRuleInAgreementException {
        // TODO Auto-generated method stub
        // log.debug("sendMessage: "+product.xmlText());
        log.debug("sendMessage: " + message.xmlText());
        // Must have a distribution element
        EDXLDistribution edxl = message.getBroadcastMessageRequest().getEDXLDistribution();
        if (edxl == null) {
            throw new IllegalArgumentException("Empty EDXLDistribution element");
        } else {
            // Find all the destination cores or JIDs from the explicit address fields
            HashSet<String> cores = BroadcastUtil.getCoreList(edxl);
            HashSet<String> jids = BroadcastUtil.getJidList(edxl);

            // Send the message to each core that has a user in an explictAddress element
            if (cores.size() == 0 && jids.size() == 0) {
                throw new EmptyCoreNameListException();
            } else {
                SendMessageErrorException errorException = new SendMessageErrorException();

                EDXLDistributionDocument edxlDoc = EDXLDistributionDocument.Factory.newInstance();
                edxlDoc.setEDXLDistribution(edxl);
                
                // Send the message to the cores as a Broadcase Service message
                errorException = sendMessageToCore(cores, errorException, edxlDoc.xmlText());
                
                // Send the message to any external XMPP addresses
                errorException = sendXMPPMessage(jids, errorException, edxlDoc);

                if (errorException.getErrors().size() > 0) {
                    throw errorException;
                }
            }
            
        }

    }

	private SendMessageErrorException sendMessageToCore(HashSet<String> cores,
			SendMessageErrorException errorException, String msgStr)
			throws NoShareAgreementException, NoShareRuleInAgreementException,
			LocalCoreNotOnlineException {
		for (String core : cores) {
		    try {
		        // log.debug("sendMessage:  Sending " + msgStr + " to: " + core);
//		    	System.out.println("sending to " + core);
		        communicationsService.sendMessage(msgStr,
		            CommunicationsService.CORE2CORE_MESSAGE_TYPE.BROADCAST_MESSAGE, core);
		        log.debug("called communicationsService.sendMessage");
		    } catch (RemoteCoreUnknownException e1) {
		        errorException.getErrors().put(core,
		            SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.CORE_UNKNOWN);
		    } catch (RemoteCoreUnavailableException e2) {
		        errorException.getErrors().put(core,
		            SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.CORE_UNAVAILABLE);
		    } catch (LocalCoreNotOnlineException e) {
		        throw e;
		    }
		}
		return errorException;
	}

	private SendMessageErrorException sendXMPPMessage(HashSet<String> jids,
			SendMessageErrorException errorException, EDXLDistributionDocument edxlDoc)
			throws NoShareAgreementException, NoShareRuleInAgreementException,
			LocalCoreNotOnlineException {
		for (String jid : jids) {
		        // log.debug("sendMessage:  Sending " + msgStr + " to: " + core);
		        communicationsService.sendXMPPMessage(getMessageBody(edxlDoc) , null, 
		        		edxlDoc.xmlText(), jid);
		}
		return errorException;
	}

    private String getMessageBody(EDXLDistributionDocument edxlDoc) {
    	StringBuffer body = new StringBuffer();
    	if (edxlDoc.getEDXLDistribution() != null) {
    		body.append("EDXL-DE message received from ");
    		if (edxlDoc.getEDXLDistribution().getSenderID() != null) {
    			body.append(edxlDoc.getEDXLDistribution().getSenderID());
    		}
    		else {
    			body.append("UICDS");
    		}
    		body.append(NEW_LINE);
    		if (edxlDoc.getEDXLDistribution().getDateTimeSent() != null) {
    			body.append("Sent at ");
    			body.append(edxlDoc.getEDXLDistribution().getDateTimeSent().toString());
        		body.append(NEW_LINE);
    		}
    		if (edxlDoc.getEDXLDistribution().sizeOfContentObjectArray() > 0) {
    			body.append("Content element descriptions: ");
    			body.append(NEW_LINE);
    			for (ContentObjectType content : edxlDoc.getEDXLDistribution().getContentObjectArray()) {
    				if (content.getContentDescription() != null) {
    					body.append("Content Description: ");
    					body.append(content.getContentDescription());
    					body.append(NEW_LINE);
    				}
    			}
    		}
    	}
    	return body.toString();
	}

	/**
     * Broadcast message notification handler dispatches received messages to the listeners
     * specified in the explicit address array
     * 
     * @param message the message
     * @ssdd
     */
    public void broadcastMessageNotificationHandler(Core2CoreMessage message) {
        log.debug("broadcastMessageNotificationHandler: received message=[" + message.getMessage()
            + "] from " + message.getFromCore());

        XmlObject xmlObj;
        try {

            EDXLDistributionDocument edxlDoc = EDXLDistributionDocument.Factory.parse(message.getMessage());

            if (edxlDoc.getEDXLDistribution().sizeOfExplicitAddressArray() > 0) {
                // Find core name for each explicit address.
                for (ValueSchemeType type : edxlDoc.getEDXLDistribution().getExplicitAddressArray()) {
                    if (type.getExplicitAddressScheme().equals(
                        CommunicationsService.UICDSExplicitAddressScheme)) {
                        for (String address : type.getExplicitAddressValueArray()) {
                            xmlObj = XmlObject.Factory.parse(edxlDoc.toString());
                            // log.debug("broadcastMessageNotificationHandler: sending notification ["
                            // + xmlObj.toString() + "]  to " + address);
                            sendMessageNotification(xmlObj, address);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            log.error("broadcastMessageNotificationHandler: Error parsing message - not a valid XML string");
            throw new IllegalArgumentException("Message is not a valid XML string");
        }
    }

    private void sendMessageNotification(XmlObject xmlObj, String address) {
        ArrayList<NotificationMessageHolderType> messages = new ArrayList<NotificationMessageHolderType>();

        NotificationMessageHolderType t = NotificationMessageHolderType.Factory.newInstance();
        NotificationMessageHolderType.Message m = t.addNewMessage();

        try {
            m.set(xmlObj);
            messages.add(t);

            NotificationMessageHolderType[] notification = new NotificationMessageHolderType[messages.size()];

            notification = messages.toArray(notification);
            log.debug("===> sending Core2Core message: array size=" + notification.length);
            notificationService.notify(UicdsStringUtil.getSubmitterResourceInstanceName(address), notification);
        } catch (Throwable e) {
            log.error("productPublicationStatusNotificationHandler: error creating and sending  Core2Core message  notification to "
                + address);
            e.printStackTrace();
        }
    }

    @Override
    public CommunicationsService getCommunicationsService() {
        return this.communicationsService;
    }

    @Override
    public void setCommunicationsService(CommunicationsService service) {
        this.communicationsService = service;
    }

    /** {@inheritDoc} */
    public void systemInitializedHandler(String message) {
        WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        directoryService.registerUICDSService(NS_BroadcastService, BROADCAST_SERVICE_NAME,
            typeList, typeList);
    }

}
