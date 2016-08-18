package com.saic.uicds.core.em.processes.agreements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.agreementService.AgreementListType;
import org.uicds.agreementService.AgreementType;
import org.uicds.agreementService.AgreementType.ShareRules;
import org.uicds.agreementService.AgreementType.ShareRules.ShareRule;
import org.uicds.agreementService.ConditionType;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.IncidentInfoType;
import org.uicds.incidentManagementService.IncidentListType;
import org.uicds.incidentManagementService.ShareIncidentRequestDocument;

import com.saic.uicds.core.em.messages.IncidentStateNotificationMessage;
import com.saic.uicds.core.em.service.IncidentManagementService;
import com.saic.uicds.core.em.util.IncidentUtil;
import com.saic.uicds.core.infrastructure.exceptions.InvalidInterestGroupIDException;
import com.saic.uicds.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnavailableException;
import com.saic.uicds.core.infrastructure.exceptions.UICDSException;
import com.saic.uicds.core.infrastructure.exceptions.XMPPComponentException;
import com.saic.uicds.core.infrastructure.messages.CoreStatusUpdateMessage;
import com.saic.uicds.core.infrastructure.messages.InterestGroupStateNotificationMessage;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.AgreementService;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;

/**
 * @author roger
 * 
 */
public class AutoShareIncidents {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private IncidentManagementService incidentManagementService;

    private AgreementService agreementService;

    private ConfigurationService configurationService;

    // private String coreName;

    static public final String INTEREST_GROUP_CODESPACE = "http://uicds.org/interestgroup#Incident";

    public void setConfigurationService(ConfigurationService configurationService) {

        this.configurationService = configurationService;
    }

    public void setAgreementService(AgreementService agreementService) {

        this.agreementService = agreementService;
    }

    public void setIncidentManagementService(IncidentManagementService incidentManagementService) {

        this.incidentManagementService = incidentManagementService;
    }

    /**
     * Core status update handler.
     * 
     * @param message the message
     * @ssdd
     */
    public void coreStatusUpdateHandler(CoreStatusUpdateMessage message) {

        String coreName = message.getCoreName();
        String coreStatus = message.getCoreStatus();

        if (coreName.equalsIgnoreCase(configurationService.getCoreName())) {
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Handling status chanage for " + coreName);
        }

        processCoreStatusChange(coreName, coreStatus);

    }

    private void processCoreStatusChange(String coreName, String coreStatus) {

        // Only process status from other cores CoreConnection resource
        if (coreName.endsWith("/CoreConnection")) {

            coreName = coreName.substring(0, coreName.lastIndexOf('/'));

            if (coreStatus.equals("unsubscribed")) {
                ;
            } else {
                if (coreStatus.equals("available")) {
                    if (log.isInfoEnabled()) {
                        log.info("checking shared incident state for " + coreName);
                    }

                    // Get a list of incidents
                    IncidentListType incidentList = incidentManagementService.getListOfIncidents();

                    if (incidentList != null && incidentList.sizeOfIncidentInfoArray() > 0) {
                        // For each owned incident check if it is already shared to this core
                        for (IncidentInfoType incidentInfo : incidentList.getIncidentInfoArray()) {
                            if (incidentInfo != null
                                && incidentInfo.getOwningCore().contains(
                                    configurationService.getCoreName())) {

                                log.debug("checking incident: " + incidentInfo.getId());
                                if (incidentInfo.getWorkProductIdentification() != null) {
                                    WorkProduct wp = incidentManagementService.getIncident(incidentInfo.getWorkProductIdentification().getIdentifier().getStringValue());
                                    UICDSIncidentType incident = IncidentUtil.getUICDSIncident(wp);

                                    // If it is not shared to this core test if it should be
                                    // according to the agreements and share it if it matches an
                                    // agreement
                                    if (incident.sizeOfSharedCoreNameArray() > 0) {

                                        // If not shared to any other core then evaluate if this
                                        // should be shared

                                        ArrayList<String> sharedList = new ArrayList<String>(
                                            Arrays.asList(incident.getSharedCoreNameArray()));

                                        // If the incident is not already shared to this core
                                        if (!sharedList.contains(coreName)) {
                                            // Then check if it should be
                                            List<String> coresToShareTo = getCoresToShareTo(incidentInfo.getWorkProductIdentification().getIdentifier().getStringValue());
                                            if (coresToShareTo.contains(coreName)) {
                                                shareIncident(incidentInfo.getId(), coreName);
                                            }
                                        }

                                        // If it is not shared to any cores then check if it
                                        // should
                                        // be shared to this core
                                    } else {

                                        List<String> coresToShareTo = getCoresToShareTo(incidentInfo.getWorkProductIdentification().getIdentifier().getStringValue());

                                        if (coresToShareTo.contains(coreName)) {
                                            shareIncident(incidentInfo.getId(), coreName);
                                        }
                                    }
                                } else {
                                    log.debug("null work product found for incident");
                                }
                            } else {
                                if (incidentInfo != null) {
                                    log.debug("null incident found in list");
                                } else {
                                    log.debug("incident not owned by this core");
                                }
                            }
                        }
                    } else {
                        log.debug("No incidents found to check for sharing");
                    }
                }
            }
        } // here
    }

    /**
     * Handles incident state notification messages.
     */
    public void handleIncidentState(IncidentStateNotificationMessage msg) {

        // log.info("Incident state change: " + msg.getIncidentInfo().getId() + " to " +
        // msg.getState());
        if (msg.getState() == InterestGroupStateNotificationMessage.State.NEW
            || msg.getState() == InterestGroupStateNotificationMessage.State.UPDATE) {

            handleNewIncident(msg);
        }
    }

    /**
     * Handles the new incident state messages. Evaluates agreements to determine if an incident
     * should be shared based on the incident type
     * 
     * @param msg IncidentStateNotificationMessage
     * @param msg
     */
    private void handleNewIncident(IncidentStateNotificationMessage msg) {

        String coreName = configurationService.getCoreName();

        // Only share if this core is the owner of this incident
        // TODO: Change back to original conditional check when ticket #230 is fixed !!!!
        // if (coreName.equalsIgnoreCase(msg.getIncidentInfo().getOwningCore())) {
        if (coreName.toLowerCase().equals(msg.getIncidentInfo().getOwningCore().toLowerCase())) {

            List<String> coresToShareTo = getCoresToShareTo(msg.getIncidentInfo().getWorkProductIdentification().getIdentifier().getStringValue());

            for (String core : coresToShareTo) {
                shareIncident(msg.getIncidentInfo().getId(), core);
            }
        }
    }

    private void shareIncident(String incidentID, String core) {

        ShareIncidentRequestDocument shareIncidentRequest = ShareIncidentRequestDocument.Factory.newInstance();
        shareIncidentRequest.addNewShareIncidentRequest();
        shareIncidentRequest.getShareIncidentRequest().setCoreName(core);
        shareIncidentRequest.getShareIncidentRequest().setIncidentID(incidentID);

        try {
            log.info("Sharing incident " + incidentID + " to " + core);
            incidentManagementService.shareIncidentAgreementChecked(shareIncidentRequest.getShareIncidentRequest());
            // incidentManagementService.shareIncident(shareIncidentRequest.getShareIncidentRequest());
        } catch (InvalidInterestGroupIDException e) {
            log.error("Error sharing incident: InvalidInterestGroupIDException");
        } catch (LocalCoreNotOnlineException e) {
            log.error("Error sharing incident: LocalCoreNotOnlineException");
        } catch (RemoteCoreUnavailableException e) {
            log.error("Error sharing incident: RemoteCoreUnavailableException");
        } catch (XMPPComponentException e) {
            log.error("Error sharing incident: XMPPComponentException");
        } catch (NoShareAgreementException e) {
            log.error("Error sharing incident: NoShareAgreementException");
        } catch (NoShareRuleInAgreementException e) {
            log.error("Error sharing incident: NoShareRuleInAgreementException");
        } catch (UICDSException e) {
            log.error("Error sharing incident: UICDSException");
        }
    }

    /**
     * get all the agreements on this core, parse, and add to list
     */
    private List<String> getCoresToShareTo(String incidentWPID) {

        ArrayList<String> cores = new ArrayList<String>();

        // Get the list of agreements from this core
        AgreementListType alist = agreementService.getAgreementList();
        // EntityListType alist = agreementService.getAgreementsList(coreName, true);

        // If there are any agreements
        if (alist != null && alist.sizeOfAgreementArray() > 0) {

            // Get the incident document and then the type
            IncidentDocument incidentDoc = null;
            try {
                WorkProduct wp = incidentManagementService.getIncident(incidentWPID);

                if (wp != null) {
                    incidentDoc = (IncidentDocument) wp.getProduct();
                } else {
                    log.error("incident work product is null " + incidentWPID);
                    return cores;
                }

                String incidentType = null;
                if (incidentDoc.getIncident().sizeOfActivityCategoryTextArray() > 0) {
                    incidentType = incidentDoc.getIncident().getActivityCategoryTextArray(0).getStringValue();
                }

                // Get each agreement and parse it
                for (AgreementType agreement : alist.getAgreementArray()) {

                    if (agreement != null) {

                        String remoteCore = agreement.getPrincipals().getRemoteCore().getStringValue();

                        // log.info("checking core " + remoteCore);
                        // Check if agreement is enabled
                        if (agreement.getShareRules() == null) {
                            cores.add(remoteCore);
                        } else {
                            // Only share if the rules are enabled
                            if (agreement.getShareRules().getEnabled()) {
                                // If there are no explicit share rules then always share to that
                                // core
                                if (agreement.getShareRules().sizeOfShareRuleArray() == 0) {
                                    cores.add(remoteCore);
                                }
                                // if there are share ruls then see if this incident matches a share
                                // rule
                                else if (matchesShareRule(agreement.getShareRules(), incidentType)) {
                                    cores.add(remoteCore);
                                }
                            }

                        }
                    } else {
                        log.error("null agreement found in agreement list");
                    }
                }

            } catch (Exception e) {
                log.error("Error handling " + incidentWPID + ":" + e.getMessage());
            }
        }

        return cores;
    }

    private boolean matchesShareRule(ShareRules shareRules, String incidentType) {

        boolean matches = false;

        // If all the rules are disabled then always share
        boolean evaluateRules = false;
        for (ShareRule rule : shareRules.getShareRuleArray()) {
            if (rule.getEnabled()) {
                evaluateRules = true;
                break;
            }
        }

        if (evaluateRules) {
            for (ShareRule rule : shareRules.getShareRuleArray()) {
                // only process rule if it is enabled and has a condition
                if (rule.getEnabled() && rule.getCondition() != null) {
                    ConditionType condition = rule.getCondition();
                    if (condition.getInterestGroup() != null
                        && condition.getInterestGroup().getCodespace() != null
                        && condition.getInterestGroup().getStringValue() != null) {
                        if (condition.getInterestGroup().getCodespace().equals(
                            AutoShareIncidents.INTEREST_GROUP_CODESPACE)
                            && condition.getInterestGroup().getStringValue().equalsIgnoreCase(
                                incidentType)) {
                            matches = true;
                        }

                    }
                }
            }
        } else {
            matches = true;
        }
        return matches;
    }

}
