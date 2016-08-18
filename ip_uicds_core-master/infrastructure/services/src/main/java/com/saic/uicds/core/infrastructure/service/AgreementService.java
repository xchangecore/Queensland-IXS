package com.saic.uicds.core.infrastructure.service;

import org.springframework.transaction.annotation.Transactional;
import org.uicds.agreementService.AgreementListType;
import org.uicds.agreementService.AgreementType;

import com.saic.uicds.core.infrastructure.dao.AgreementDAO;
import com.saic.uicds.core.infrastructure.exceptions.AgreementWithCoreExists;
import com.saic.uicds.core.infrastructure.exceptions.MissingConditionInShareRuleException;
import com.saic.uicds.core.infrastructure.exceptions.MissingShareRulesElementException;

/**
 * Manages Agreements.
 * 
 * @author William Summers
 * @since 1.0
 * @see com.saic.uicds.core.infrastructure.model.Agreement Agreement Data Model
 * @ssdd
 * 
 */
@Transactional
public interface AgreementService {
    public static final String AGREEMENT_SERVICE_NAME = "AgreementService";
    public static final String AgreementTypeUrn = "urn:uicds:agreement:type";
    public static final String AgreementIdUrn = "urn:uicds:agreement:id";

    /**
     * Allows the client to create a new Agreement
     * 
     * @param agreement instance of agreement object
     * @return agreement - instance that was created in core
     * @throws MissingConditionInShareRuleException, AgreementWithCoreExists 
     * @see AgreementType
     * @ssdd
     */
    public AgreementType createAgreement(AgreementType agreement) 
    	throws MissingShareRulesElementException, MissingConditionInShareRuleException, AgreementWithCoreExists;

    /**
     * Allows the client to modify an existing Agreement
     * 
     * @param agreementID
     * @param agreement instance of agreement object
     * @return agreement - instance that was created in core
     * @see AgreementType
     * @ssdd
     */
    public AgreementType updateAgreement(String coreID, AgreementType agreement);

    /**
     * Allows the client to delete an existing agreement
     * 
     * @param agreementID
     * @return string - agreementID of deleted agreement
     * @ssdd
     */
    public String rescindAgreement(String coreID);

    /**
     * Allows the client to retrieve an agreement associated with a specific agreementID
     * 
     * @param coreID
     * @return agreement - instance that was requested corresponding to the ID specified
     * @see AgreementType
     * @ssdd
     */
    public AgreementType getAgreement(String coreID);

    /**
     * Allows the client to retrieve a list of existing Agreements
     * 
     * @param coreID primary key
     * @param matchPartials if true, match all IDs of which the supplied entityID is a substring
     * @return EntityList - list of entity's that have agreements with this entity
     * @see EntityListType
     * @ssdd
     */

    public AgreementListType getAgreementList();
    
    /**
     * Gets the ConfigurationService dependency
     * 
     * @param None
     * @return ConfigurationService
     * @see ConfigurationService
     * @ssdd
     */
    public ConfigurationService getConfigurationService();

    /**
     * Sets the ConfigurationService dependency
     * 
     * @param service ConfigurationService
     * @return void
     * @see ConfigurationService
     * @ssdd
     */
    public void setConfigurationService(ConfigurationService service);

    /**
     * Gets the DirectoryService dependency
     * 
     * @param None
     * @return DirectoryService
     * @see DirectoryService
     * @ssdd
     */
    public DirectoryService getDirectoryService();

    /**
     * Sets the DirectoryService dependency
     * 
     * @param service DirectoryService
     * @return void
     * @see DirectoryService
     * @ssdd
     */
    public void setDirectoryService(DirectoryService service);

    /**
     * SystemIntialized Message Handler
     * 
     * @param message SystemInitialized message
     * @return void
     * @see applicationContext
     * @ssdd
     */
    public void systemInitializedHandler(String messgae);
}