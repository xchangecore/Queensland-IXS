package com.saic.uicds.core.em.have.service;

import org.springframework.transaction.annotation.Transactional;
import org.uicds.haveService.EdxlDeResponseDocument;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;

import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.uicds.core.em.exceptions.SendMessageErrorException;
import com.saic.uicds.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.saic.uicds.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.saic.uicds.core.infrastructure.messages.Core2CoreMessage;

/**
 * 
 * 
 * @author Roger
 * @since 1.0
 * @ssdd
 */
@Transactional
public interface HAVEService {

    public static final String HAVE_SERVICE_NAME = "HAVEService";
    
    public static final String HAVE_PRODUCT_TYPE = "EDXL-HAVE";

    /**
     * Allows the client to submit an EDXL-HAVE document wrapped in EDXL-DE
     * 
     * @param request - EDXLDistribution containing HAVE Message
     * @return workProductId - String
     * @throws LocalCoreNotOnlineException
     * @throws SendMessageErrorException
     * @throws EmptyCoreNameListException
     * @throws IllegalArgumentException
     * @see EDXLDistribution
     * @ssdd
     */
    public EdxlDeResponseDocument edxldeRequest(EDXLDistribution request)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException;

    /**
     * Gets a list of HAVE work products for an incident if the id is not null or empty.
     * 
     * @param incidentID
     * @return WorkProductList
     * @ssdd
     */
    public WorkProductList getHAVEMessages(String incidentID);
    
    /**
     * Find a work product with the given distributionReference.
     * @param distributionReference
     * @return
     */
    public WorkProductDocument findHAVEMessage(String distributionReference);
    
    /**
     * Handle incoming HAVE messages to put on the appropriate notification queue.
     * 
     * @param message
     */
    public void haveMessageNotificationHandler(Core2CoreMessage message);

    /**
     * SystemIntialized Message Handler
     * 
     * @param message - SystemInitialized message
     * @return void
     * @see applicationContext
     * @ssdd
     */
    public void systemInitializedHandler(String messgae);

}
