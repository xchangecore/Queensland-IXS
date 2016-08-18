package com.saic.uicds.core.em.service;

import org.springframework.transaction.annotation.Transactional;
import org.uicds.broadcastService.BroadcastMessageRequestDocument;

import com.saic.uicds.core.em.exceptions.SendMessageErrorException;
import com.saic.uicds.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.saic.uicds.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.saic.uicds.core.infrastructure.messages.Core2CoreMessage;
import com.saic.uicds.core.infrastructure.service.CommunicationsService;

/**
 * The Broadcast Service provides operations to send messages to joined cores.
 * 
 * @version UICDS - alpha
 * @author roberta
 * @ssdd
 * 
 */
@Transactional
public interface BroadcastService {
    public static final String BROADCAST_SERVICE_NAME = "BroadcastService";

    /**
     * Send a BroadCast Message
     * 
     * @param message the message
     * 
     * @throws IllegalArgumentException the illegal argument exception
     * @throws EmptyCoreNameListException the empty core name list exception
     * @throws SendMessageErrorException the send message error exception
     * @throws LocalCoreNotOnlineException the local core not online exception
     * @throws NoShareAgreementException the no share agreement exception
     * @throws NoShareRuleInAgreementException the no share rule in agreement exception
     * @ssdd
     */
    public void broadcastMessage(BroadcastMessageRequestDocument message)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException, NoShareAgreementException, NoShareRuleInAgreementException;

    /**
     * Gets the communications service.
     * 
     * @return the communications service
     * @ssdd
     */
    public CommunicationsService getCommunicationsService();

    /**
     * Sets the communications service.
     * 
     * @param service the new communications service
     * @ssdd
     */
    public void setCommunicationsService(CommunicationsService service);

    /**
     * SystemIntialized Message Handler
     * 
     * @param message SystemInitialized message
     * @return void
     * @see applicationContext
     * @ssdd
     */
    public void systemInitializedHandler(String messgae);

    /**
     * Handles notification of a received broadcast message
     * 
     * @param message message
     * @see Core2CoreMessage
     * @return errors true if error condition occurred - false otherwise
     * @see applicationContext
     * @ssdd
     */
    public void broadcastMessageNotificationHandler(Core2CoreMessage message);

}
