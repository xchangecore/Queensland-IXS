package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;

public class AgreementGWT implements Serializable {

    private static final long serialVersionUID = -7909684672536211321L;

    private static String CODESPACE_INTEREST_GROUP_INCIDENT = "http://uicds.org/interestgroup#Incident";
    private static String INTEREST_GROUP_INCIDENT_NAME = "Incident";

    public static String OPERATION_ADD_RULE = "addRule";
    public static String OPERATION_DELETE_RULE = "deleteRule";
    public static String OPERATION_TOGGLE_AGREEMENT_STATUS = "toggleAgreementStatus";

    // operation is a flag variable to distinguish the type of update to perform
    private String operation = OPERATION_TOGGLE_AGREEMENT_STATUS;
    // agreementXml is xml string of the original agreement.
    // Update operations use this as the base agreement that is then modified and dispatched.
    private String agreementXml = null;
    private Boolean shareRulesEnabled;
    private String localCore;
    private String remoteCore;
    Boolean ruleEnabled = true;
    String ruleId = null;
    String ruleCodespace = CODESPACE_INTEREST_GROUP_INCIDENT;
    String ruleLabel = INTEREST_GROUP_INCIDENT_NAME;
    // principal variables
    String ruleIncidentType = null;
    private boolean root = false;
    private boolean leaf = true;
    private String statusMessage = null;
    private String agreementTreeTitle;

    public AgreementGWT() {

        super();
    }

    public String getAgreementTreeTitle() {

        return agreementTreeTitle;
    }

    public String getAgreementXml() {

        return agreementXml;
    }

    public String getLocalCore() {

        return localCore;
    }

    // rule variables

    public String getOperation() {

        return operation;
    }

    public String getRemoteCore() {

        return remoteCore;
    }

    public String getRuleCodespace() {

        return ruleCodespace;
    }

    public Boolean getRuleEnabled() {

        return ruleEnabled;
    }

    public String getRuleId() {

        return ruleId;
    }

    public String getRuleIncidentType() {

        return ruleIncidentType;
    }

    public String getRuleLabel() {

        return ruleLabel;
    }

    public Boolean getShareRulesEnabled() {

        return shareRulesEnabled;
    }

    public String getStatusMessage() {

        return statusMessage;
    }

    public boolean isLeaf() {

        return leaf;
    }

    public boolean isRoot() {

        return root;
    }

    public void setAgreementTreeTitle(String agreementTreeTitle) {

        this.agreementTreeTitle = agreementTreeTitle;
    }

    public void setAgreementXml(String agreementXml) {

        this.agreementXml = agreementXml;
    }

    public void setLeaf(boolean leaf) {

        this.leaf = leaf;
    }

    public void setLocalCore(String localCore) {

        this.localCore = localCore;
    }

    public void setOperation(String operation) {

        this.operation = operation;
    }

    // tree variables to determine the current level in the tree

    public void setRemoteCore(String remoteCore) {

        this.remoteCore = remoteCore;
    }

    public void setRoot(boolean root) {

        this.root = root;
    }

    public void setRuleCodespace(String ruleCodespace) {

        this.ruleCodespace = ruleCodespace;
    }

    public void setRuleEnabled(Boolean ruleEnabled) {

        this.ruleEnabled = ruleEnabled;
    }

    public void setRuleId(String ruleId) {

        this.ruleId = ruleId;
    }

    public void setRuleIncidentType(String ruleIncidentType) {

        this.ruleIncidentType = ruleIncidentType;
    }

    // message for reporting status back from server

    public void setRuleLabel(String ruleLabel) {

        this.ruleLabel = ruleLabel;
    }

    public void setShareRulesEnabled(Boolean shareRulesEnabled) {

        this.shareRulesEnabled = shareRulesEnabled;
    }

    public void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
    }
}
