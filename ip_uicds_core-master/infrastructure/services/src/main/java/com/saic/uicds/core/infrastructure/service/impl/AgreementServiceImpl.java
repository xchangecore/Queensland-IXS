package com.saic.uicds.core.infrastructure.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.uicds.agreementService.AgreementListType;
import org.uicds.agreementService.AgreementType;
import org.uicds.directoryServiceData.WorkProductTypeListType;

import com.saic.precis.x2009.x06.base.CodespaceValueType;
import com.saic.uicds.core.infrastructure.dao.AgreementDAO;
import com.saic.uicds.core.infrastructure.exceptions.AgreementWithCoreExists;
import com.saic.uicds.core.infrastructure.exceptions.MissingConditionInShareRuleException;
import com.saic.uicds.core.infrastructure.exceptions.MissingShareRulesElementException;
import com.saic.uicds.core.infrastructure.messages.AgreementRosterMessage;
import com.saic.uicds.core.infrastructure.model.Agreement;
import com.saic.uicds.core.infrastructure.model.CodeSpaceValueType;
import com.saic.uicds.core.infrastructure.model.ShareRule;
import com.saic.uicds.core.infrastructure.service.AgreementService;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.util.AgreementUtil;

/**
 * The AgreementsService implementation.
 * 
 * @author William Summers
 * @since 1.0
 * @see com.saic.uicds.core.infrastructure.model.Agreement Agreement Data Model
 * @see com.saic.uicds.core.infrastructure.model.CodeSpaceValueType CodeSpaceValueType Data Model
 * @see com.saic.uicds.core.infrastructure.model.ShareRule ShareRule Data Model
 * @ssdd
 */
public class AgreementServiceImpl
    implements AgreementService {

    Logger log = LoggerFactory.getLogger(AgreementServiceImpl.class);

    private ConfigurationService configService;
    private DirectoryService directoryService;
    private AgreementDAO dao;

    private MessageChannel agreementRosterChannel;

    public static final String SHARE_RULE_ID_PREFIX = "UICDS-";

    public void setAgreementRosterChannel(MessageChannel agreementRosterChannel) {

        this.agreementRosterChannel = agreementRosterChannel;
    }

    /** {@inheritDoc} */
    public void systemInitializedHandler(String messgae) {

        String urn = getConfigurationService().getServiceNameURN(AGREEMENT_SERVICE_NAME);
        WorkProductTypeListType publishedProducts = WorkProductTypeListType.Factory.newInstance();
        WorkProductTypeListType subscribedProducts = WorkProductTypeListType.Factory.newInstance();
        directoryService.registerUICDSService(urn, AGREEMENT_SERVICE_NAME, publishedProducts,
            subscribedProducts);

        sendInitialAgreementRoster();
    }

    /**
     * Send initial agreement roster.
     * 
     * @ssdd
     */
    public void sendInitialAgreementRoster() {

        List<Agreement> agreements = getDao().findAll();

        for (Agreement agreement : agreements) {
            Map<String, AgreementRosterMessage.State> cores = new HashMap<String, AgreementRosterMessage.State>();

            log.debug("sendInitialAgreementRoster: send status for "
                + agreement.getRemoteCore().getValue());

            // First time we see this (even if it's from the database), so send a "CREATE" state
            cores.put(agreement.getRemoteCore().getValue(), AgreementRosterMessage.State.CREATE);

            // send out an intial roster for each agreement
            AgreementRosterMessage message = new AgreementRosterMessage(
                agreement.getRemoteCore().getValue(), cores);

            Message<AgreementRosterMessage> notification = new GenericMessage<AgreementRosterMessage>(
                message);

            agreementRosterChannel.send(notification);
        }

    }

    /**
     * Send agreement roster update.
     * 
     * @param agreement the agreement
     * @param state the state
     * @ssdd
     */
    public void sendAgreementRosterUpdate(Agreement agreement, AgreementRosterMessage.State state) {

        Map<String, AgreementRosterMessage.State> cores = new HashMap<String, AgreementRosterMessage.State>();

        log.debug("sendAgreementRosterUpdate: send status for "
            + agreement.getRemoteCore().getValue());

        // First time we see this (even if it is from the database), so send a "CREATE" state
        cores.put(agreement.getRemoteCore().getValue(), state);

        AgreementRosterMessage message = new AgreementRosterMessage(
            agreement.getRemoteCore().getValue(), cores);

        Message<AgreementRosterMessage> notification = new GenericMessage<AgreementRosterMessage>(
            message);

        agreementRosterChannel.send(notification);
    }

    /**
     * Creates a core to core agreement and enables share rules if specified. Processes share rules
     * for specific work product types. Note that the remote core must also create an agreement with
     * the local core to be reciprocal.
     * 
     * @param agreementType the agreement type
     * 
     * @return the agreement type
     * 
     * @throws MissingConditionInShareRuleException the missing condition in share rule exception
     * @throws AgreementWithCoreExists the agreement with core already exists
     * @ssdd
     */
    @Override
    public AgreementType createAgreement(AgreementType agreementType)
        throws MissingShareRulesElementException, MissingConditionInShareRuleException,
        AgreementWithCoreExists {

        if (agreementType.getPrincipals().getRemoteCore() == null
            || agreementType.getPrincipals().getRemoteCore().isNil()) {
            throw new IllegalArgumentException("Remote core is null in agreement request");
        }
        if (agreementType.getPrincipals().getLocalCore() == null
            || agreementType.getPrincipals().getLocalCore().isNil()) {
            throw new IllegalArgumentException("Local core is null in agreement request");
        }

        Agreement model = getDao().findByCoreID(
            agreementType.getPrincipals().getRemoteCore().getStringValue());
        if (model != null) {
            throw new AgreementWithCoreExists();
        }

        if (agreementType.getShareRules() == null) {
            throw new MissingShareRulesElementException();
        }

        model = new Agreement();

        // Set the Consumer
        CodeSpaceValueType remoteCore = new CodeSpaceValueType();
        if (agreementType.getPrincipals().getRemoteCore().getLabel() != null) {
            remoteCore.setLabel(agreementType.getPrincipals().getRemoteCore().getLabel());
        }
        remoteCore.setValue(agreementType.getPrincipals().getRemoteCore().getStringValue());
        model.setRemoteCore(remoteCore);

        // Set the Provider
        CodeSpaceValueType localCore = new CodeSpaceValueType();
        if (agreementType.getPrincipals().getLocalCore().getLabel() != null) {
            localCore.setLabel(agreementType.getPrincipals().getLocalCore().getLabel());
        }
        localCore.setValue(agreementType.getPrincipals().getLocalCore().getStringValue());
        model.setLocalCore(localCore);

        // set enable field for ruleSet
        model.setEnabled(agreementType.getShareRules().getEnabled());

        if (agreementType.getShareRules() != null
            && agreementType.getShareRules().sizeOfShareRuleArray() > 0) {

            // Set the Share Rules
            HashSet<ShareRule> shareRules = new HashSet<ShareRule>();

            int ruleID = 0;
            for (AgreementType.ShareRules.ShareRule shareRule : agreementType.getShareRules().getShareRuleArray()) {
                ShareRule rule = new ShareRule();
                if (shareRule.getId() == null) {
                    rule.setRuleID(SHARE_RULE_ID_PREFIX + ruleID++);
                } else {
                    rule.setRuleID(shareRule.getId());
                }
                rule.setEnabled(shareRule.getEnabled());

                if (shareRule.getCondition() == null) {
                    throw new MissingConditionInShareRuleException();
                } else {
                    CodeSpaceValueType interestGroup = new CodeSpaceValueType();
                    if (shareRule.getCondition().getInterestGroup().getCodespace() != null) {
                        interestGroup.setCodeSpace(shareRule.getCondition().getInterestGroup().getCodespace());
                    }
                    if (shareRule.getCondition().getInterestGroup().getLabel() != null) {
                        interestGroup.setLabel(shareRule.getCondition().getInterestGroup().getLabel());

                    }
                    if (shareRule.getCondition().getInterestGroup().getStringValue() != null) {
                        interestGroup.setValue(shareRule.getCondition().getInterestGroup().getStringValue());
                    }

                    rule.setInterestGroup(interestGroup);

                    // if (shareRule.getCondition().getTimeInterval() != null) {
                    // rule.setTimeInterval(shareRule.getCondition().getTimeInterval());
                    // }
                    //
                    // if (shareRule.getCondition().getPolygon() != null) {
                    // rule.setPolygon(shareRule.getCondition().getPolygon());
                    // }
                }

                if (shareRule.getWorkProducts() != null) {
                    // CodeSpaceValueType[] workProducts = new
                    // CodeSpaceValueType[shareRule.getWorkProducts().sizeOfTypeArray()];
                    HashSet<CodeSpaceValueType> workProducts = new HashSet<CodeSpaceValueType>();
                    int j = 0;
                    for (CodespaceValueType type : shareRule.getWorkProducts().getTypeArray()) {
                        CodeSpaceValueType workProduct = new CodeSpaceValueType();

                        if (type.getCodespace() != null) {
                            workProduct.setCodeSpace(type.getCodespace());
                        }
                        if (type.getLabel() != null) {
                            workProduct.setLabel(type.getLabel());
                        }
                        if (type.getStringValue() != null) {
                            workProduct.setValue(type.getStringValue());
                        }

                        workProducts.add(workProduct);
                        j++;
                    }

                    rule.setWorkProducts(workProducts);
                }
                // shareRulesArray[i] = rule;
                // i++;
                shareRules.add(rule);

            }

            // add to model
            model.setShareRules(shareRules);

        }

        // persist the agreement
        AgreementType response = null;
        try {
            model = getDao().makePersistent(model);
            if (model != null) {
                log.debug("Persisted Agreement: " + model.getRemoteCore().getValue());
                response = AgreementUtil.copyProperties(model);

                // send a new agreement roster data to CommsXMPP
                sendAgreementRosterUpdate(model, AgreementRosterMessage.State.CREATE);
            } else {
                log.error("error persisting agreement object");
                response = AgreementType.Factory.newInstance();
            }
        } catch (Exception e) {
            log.error("exception occurred persisting agreement object");
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Update agreement to enable/disable share rules and to replace the list of share rules
     * 
     * @param remoteCoreID the remote core id
     * @param agreementType the agreement type
     * 
     * @return the agreement type
     * @ssdd
     */
    @Override
    public AgreementType updateAgreement(String remoteCoreID, AgreementType agreementType) {

        Agreement model = getDao().findByCoreID(remoteCoreID);

        if (model == null) {
            return AgreementType.Factory.newInstance();
        }

        log.debug("Updating agreement " + model.getRemoteCore().getValue());

        // set enable field for ruleSet
        model.setEnabled(agreementType.getShareRules().getEnabled());

        if (agreementType.getShareRules() != null
            && agreementType.getShareRules().sizeOfShareRuleArray() > 0) {

            // Set the Share Rules
            // ShareRule[] shareRulesArray = new ShareRule[length];
            HashSet<ShareRule> shareRules = new HashSet<ShareRule>();

            for (AgreementType.ShareRules.ShareRule shareRule : agreementType.getShareRules().getShareRuleArray()) {
                ShareRule rule = new ShareRule();
                rule.setRuleID(shareRule.getId());
                rule.setEnabled(shareRule.getEnabled());

                CodeSpaceValueType interestGroup = new CodeSpaceValueType();
                if (shareRule.getCondition().getInterestGroup().getCodespace() != null) {
                    interestGroup.setCodeSpace(shareRule.getCondition().getInterestGroup().getCodespace());
                }
                if (shareRule.getCondition().getInterestGroup().getLabel() != null) {
                    interestGroup.setLabel(shareRule.getCondition().getInterestGroup().getLabel());

                }
                if (shareRule.getCondition().getInterestGroup().getStringValue() != null) {
                    interestGroup.setValue(shareRule.getCondition().getInterestGroup().getStringValue());
                }

                rule.setInterestGroup(interestGroup);

                // if (shareRule.getCondition().getTimeInterval() != null) {
                // rule.setTimeInterval(shareRule.getCondition().getTimeInterval());
                // }
                //
                // if (shareRule.getCondition().getPolygon() != null) {
                // rule.setPolygon(shareRule.getCondition().getPolygon());
                // }

                if (shareRule.getWorkProducts() != null) {
                    // CodeSpaceValueType[] workProducts = new
                    // CodeSpaceValueType[shareRule.getWorkProducts().sizeOfTypeArray()];
                    HashSet<CodeSpaceValueType> workProducts = new HashSet<CodeSpaceValueType>();
                    int j = 0;
                    for (CodespaceValueType type : shareRule.getWorkProducts().getTypeArray()) {
                        CodeSpaceValueType workProduct = new CodeSpaceValueType();

                        if (type.getCodespace() != null) {
                            workProduct.setCodeSpace(type.getCodespace());
                        }
                        if (type.getLabel() != null) {
                            workProduct.setLabel(type.getLabel());
                        }
                        if (type.getStringValue() != null) {
                            workProduct.setValue(type.getStringValue());
                        }

                        workProducts.add(workProduct);
                        j++;
                    }

                    rule.setWorkProducts(workProducts);
                }

                // shareRulesArray[i] = rule;
                // i++;
                shareRules.add(rule);
            }

            // add to model
            // if only it were this simple
            // model.setShareRules(shareRulesArray);
            model.setShareRules(shareRules);

        } else {
            model.setShareRules(new HashSet<ShareRule>());
        }

        // persist the agreement
        Agreement model2 = null;
        AgreementType response = null;
        try {
            model2 = getDao().makePersistent(model);
            if (model2 != null) {
                log.debug("Updated Agreement: " + model2.getRemoteCore().getValue());
                response = AgreementUtil.copyProperties(model2);

                // send a new agreement roster data to CommsXMPP
                sendAgreementRosterUpdate(model2, AgreementRosterMessage.State.CREATE);
            } else {
                log.error("error persisting agreement object");
                response = AgreementType.Factory.newInstance();
            }
        } catch (Exception e) {
            log.debug("exception occurred persisting agreement object");
            e.printStackTrace();
        }

        return response;

    }

    /**
     * Rescind agreement.
     * 
     * @param coreID the core id
     * 
     * @return the string
     * @ssdd
     */
    @Override
    public String rescindAgreement(String coreID) {

        Agreement model = getDao().findByCoreID(coreID);

        if (model.getRemoteCore().getValue().equals(coreID)) {
            log.debug("Deleting agreement: " + coreID);
            getDao().makeTransient(model);

            // send a rescinded agreement roster data to CommsXMPP
            sendAgreementRosterUpdate(model, AgreementRosterMessage.State.RESCIND);

            return coreID;
        } else {
            log.debug("Could not find agreement for: " + coreID);
            return null;
        }

    }

    /**
     * Gets the agreement.
     * 
     * @param coreID the core id of the remote core
     * 
     * @return the agreement
     * @ssdd
     */
    @Override
    public AgreementType getAgreement(String coreID) {

        log.debug("Fetching agreement: " + coreID + " as model2");
        Agreement model2 = getDao().findByCoreID(coreID);
        AgreementType response = null;

        if (model2 != null) {
            response = AgreementUtil.copyProperties(model2);
        }

        return response;
    }

    /**
     * Gets the list of agreements for all joined cores.
     * 
     * @return the agreement list
     * @ssdd
     */
    @Override
    public AgreementListType getAgreementList() {

        AgreementListType response = AgreementListType.Factory.newInstance();
        List<Agreement> agreements = getDao().findAll();

        if (agreements != null && agreements.size() > 0) {
            AgreementType[] agreementTypes = new AgreementType[agreements.size()];
            int i = 0;
            for (Agreement agreement : agreements) {
                agreementTypes[i] = AgreementUtil.copyProperties(agreement);
                i++;
            }
            response.setAgreementArray(agreementTypes);
            return response;
        } else {
            return response;
        }

    }

    /** {@inheritDoc} */
    @Override
    public ConfigurationService getConfigurationService() {

        return this.configService;
    }

    /** {@inheritDoc} */
    @Override
    public void setConfigurationService(ConfigurationService service) {

        this.configService = service;
    }

    /** {@inheritDoc} */
    public AgreementDAO getDao() {

        return this.dao;
    }

    /** {@inheritDoc} */
    public void setDao(AgreementDAO dao) {

        this.dao = dao;
    }

    /** {@inheritDoc} */
    @Override
    public DirectoryService getDirectoryService() {

        return this.directoryService;
    }

    /** {@inheritDoc} */
    @Override
    public void setDirectoryService(DirectoryService service) {

        this.directoryService = service;
    }
}
