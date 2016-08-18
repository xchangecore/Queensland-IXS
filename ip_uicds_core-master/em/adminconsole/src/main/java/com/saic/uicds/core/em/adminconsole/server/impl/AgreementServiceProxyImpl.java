package com.saic.uicds.core.em.adminconsole.server.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.uicds.agreementService.AgreementListType;
import org.uicds.agreementService.AgreementType;
import org.uicds.agreementService.AgreementType.Principals;
import org.uicds.agreementService.AgreementType.ShareRules;
import org.uicds.agreementService.AgreementType.ShareRules.ShareRule;
import org.uicds.agreementService.ConditionType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.saic.precis.x2009.x06.base.CodespaceValueType;
import com.saic.uicds.core.em.adminconsole.client.model.AgreementGWT;
import com.saic.uicds.core.em.adminconsole.client.rpc.AgreementServiceProxy;
import com.saic.uicds.core.infrastructure.exceptions.AgreementWithCoreExists;
import com.saic.uicds.core.infrastructure.exceptions.MissingConditionInShareRuleException;
import com.saic.uicds.core.infrastructure.exceptions.MissingShareRulesElementException;
import com.saic.uicds.core.infrastructure.service.AgreementService;

public class AgreementServiceProxyImpl extends RemoteServiceServlet implements
    AgreementServiceProxy {

    private static final long serialVersionUID = 3631735818416848975L;

    private static String JID_PREFIX_LOCAL = "uicds";

    private AgreementService service = null;

    public AgreementType agreementAddRule(AgreementGWT agreementGWT) {

        AgreementType agreement = getOriginalAgreement(agreementGWT);
        ShareRules shareRules = agreement.getShareRules();

        // do not add the share rule if it exists already
        for (ShareRule originalShareRule : agreement.getShareRules().getShareRuleArray()) {
            if (originalShareRule.getId().equals(agreementGWT.getRuleId())
                || (originalShareRule.getCondition().getInterestGroup().getStringValue().equals(agreementGWT.getRuleIncidentType()))) {
                agreementGWT.setStatusMessage("Rule Id " + agreementGWT.getRuleId()
                    + " or Incident Type " + agreementGWT.getRuleIncidentType() + " Already Exists");
                System.out.println(agreementGWT.getStatusMessage());
                return agreement;
            }
        }

        if (agreementGWT.getRuleId() != null) {
            ShareRule shareRule = shareRules.addNewShareRule();
            shareRule.setEnabled(agreementGWT.getRuleEnabled());
            shareRule.setEnabled(true);
            shareRule.setId(agreementGWT.getRuleId());
            ConditionType condition = shareRule.addNewCondition();
            CodespaceValueType interestGroup = condition.addNewInterestGroup();
            interestGroup.setCodespace(agreementGWT.getRuleCodespace());
            interestGroup.setLabel(agreementGWT.getRuleLabel());
            interestGroup.setStringValue(agreementGWT.getRuleIncidentType());
        }

        return agreement;
    }

    public AgreementType agreementDeleteRule(AgreementGWT agreementGWT) {

        AgreementType originalAgreement = getOriginalAgreement(agreementGWT);
        AgreementType agreement = AgreementType.Factory.newInstance();
        Principals principals = agreement.addNewPrincipals();
        principals.setLocalCore(originalAgreement.getPrincipals().getLocalCore());
        principals.setRemoteCore(originalAgreement.getPrincipals().getRemoteCore());
        ShareRules shareRules = agreement.addNewShareRules();
        shareRules.setEnabled(originalAgreement.getShareRules().getEnabled());

        Boolean ruleFound = false;
        for (ShareRule originalShareRule : originalAgreement.getShareRules().getShareRuleArray()) {
            if (originalShareRule.getId().equals(agreementGWT.getRuleId())) {
                ruleFound = true;
            } else {
                ShareRule newShareRule = shareRules.addNewShareRule();
                newShareRule.setId(originalShareRule.getId());
                newShareRule.setCondition(originalShareRule.getCondition());
            }
        }

        if (!ruleFound) {
            agreementGWT.setStatusMessage("Rule Id " + agreementGWT.getRuleId() + " Not Found");
            System.out.println(agreementGWT.getStatusMessage());
            return originalAgreement;
        }

        return agreement;
    }

    private AgreementService agreementService() {

        if (this.service == null) {
            boolean loaded = loadService();
        }
        return this.service;
    }

    private AgreementGWT agreementToAgreementGWT(AgreementType agreement) {

        AgreementGWT tempGWT = new AgreementGWT();
        tempGWT.setRoot(false);
        Principals principals = agreement.getPrincipals();
        String agreementTreeTitle = principals.getRemoteCore().getStringValue();
        if (agreement.getShareRules().getEnabled()) {
            tempGWT.setAgreementTreeTitle(agreementTreeTitle);
        } else {
            tempGWT.setAgreementTreeTitle("[" + agreementTreeTitle + "]");
        }
        tempGWT.setLocalCore(principals.getLocalCore().getStringValue());
        tempGWT.setRemoteCore(principals.getRemoteCore().getStringValue());
        tempGWT.setShareRulesEnabled(agreement.getShareRules().getEnabled());
        tempGWT.setAgreementXml(Util.getPrettyXmlFromString(agreement.toString()));

        return tempGWT;
    }

    public AgreementGWT createAgreement(AgreementGWT agreementGWT) {

        System.out.println("createAgreement " + agreementGWT.getRemoteCore());

        // populate the agreement to be submitted from the agreementGWT
        AgreementType agreement = AgreementType.Factory.newInstance();
        Principals principals = agreement.addNewPrincipals();
        principals.addNewLocalCore().setStringValue(
            JID_PREFIX_LOCAL + "@" + getFullyQualifiedHostName());
        principals.addNewRemoteCore().setStringValue(agreementGWT.getRemoteCore());

        ShareRules rules = agreement.addNewShareRules();
        rules.setEnabled(false);
        // System.out.println("agreement:\n" + agreement.toString());

        // submit the agreement for creation
        try {
            agreementService().createAgreement(agreement);
        } catch (MissingConditionInShareRuleException e) {
            System.err.println(e.getMessage());
        } catch (AgreementWithCoreExists e) {
            System.err.println(e.getMessage());
        } catch (MissingShareRulesElementException e) {
            System.err.println(e.getMessage());
        }
        return agreementGWT;
    }

    public AgreementGWT getAgreement(String coreID) {

        AgreementType agreement = agreementService().getAgreement(coreID);
        AgreementGWT agreementGWT = agreementToAgreementGWT(agreement);
        System.out.println("getAgreement " + agreementGWT.getRemoteCore());
        return agreementGWT;
    }

    public List<AgreementGWT> getAgreementList() {

        AgreementListType agreementList = agreementService().getAgreementList();
        AgreementType[] agreementArray = agreementList.getAgreementArray();
        List<AgreementGWT> agreementListGWT = new ArrayList<AgreementGWT>(agreementArray.length);
        for (AgreementType agreement : agreementArray) {
            agreementListGWT.add(agreementToAgreementGWT(agreement));
        }
        return agreementListGWT;
    }

    public List<AgreementGWT> getAgreementListChildren(AgreementGWT agreementGWT) {

        List<AgreementGWT> agreementListGWT = null;
        if (agreementGWT.isRoot()) {
            agreementGWT.setRoot(false);
            agreementListGWT = new ArrayList<AgreementGWT>(1);
            agreementListGWT.add(agreementGWT);
        } else {
            agreementListGWT = getAgreementList();
        }
        return agreementListGWT;
    }

    private String getFullyQualifiedHostName() {

        String fullyQualifiedHostName = null;
        try {
            fullyQualifiedHostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return fullyQualifiedHostName;
    }

    private AgreementType getOriginalAgreement(AgreementGWT agreementGWT) {

        AgreementType agreement = null;
        try {
            agreement = AgreementType.Factory.parse(agreementGWT.getAgreementXml());
        } catch (XmlException e) {
            e.printStackTrace();
        }
        return agreement;
    }

    private boolean loadService() {

        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        this.service = (AgreementService) springContext.getBean("agreementService");
        if (service == null) {
            throw new RuntimeException("Unable to load AgreementService!");
        } else {
            return true;
        }
    }

    public String rescindAgreement(String coreID) {

        String rescindedCoreID = agreementService().rescindAgreement(coreID);
        System.out.println("rescindAgreement:" + rescindedCoreID);
        return rescindedCoreID;
    }

    public AgreementGWT updateAgreement(AgreementGWT agreementGWT) {

        System.out.println("updateAgreement " + agreementGWT.getOperation() + " "
            + agreementGWT.getRemoteCore());
        AgreementType agreement = null;
        String operation = agreementGWT.getOperation();
        if (operation.equals(AgreementGWT.OPERATION_ADD_RULE)) {
            agreement = agreementAddRule(agreementGWT);
            if (agreementGWT.getStatusMessage() != null) {
                System.out.println("statusMessage=" + agreementGWT.getStatusMessage());
            } else {
                System.out.println("agreement=" + agreement.toString());
                AgreementType agreementResult = agreementService().updateAgreement(
                    agreementGWT.getRemoteCore(), agreement);
                agreementGWT = agreementToAgreementGWT(agreementResult);
            }
        } else if (operation.equals(AgreementGWT.OPERATION_DELETE_RULE)) {
            agreement = agreementDeleteRule(agreementGWT);
            if (agreementGWT.getStatusMessage() != null) {
                System.out.println("statusMessage=" + agreementGWT.getStatusMessage());
            } else {
                System.out.println("agreement=" + agreement.toString());
                AgreementType agreementResult = agreementService().updateAgreement(
                    agreementGWT.getRemoteCore(), agreement);
                agreementGWT = agreementToAgreementGWT(agreementResult);
            }
        } else {
            agreement = getOriginalAgreement(agreementGWT);
            if (agreement.getShareRules().getEnabled() == true) {
                agreement.getShareRules().setEnabled(false);
            } else if (agreement.getShareRules().getEnabled() == false) {
                agreement.getShareRules().setEnabled(true);
            }
            System.out.println("agreement=" + agreement.toString());
            AgreementType agreementResult = agreementService().updateAgreement(
                agreementGWT.getRemoteCore(), agreement);
            agreementGWT = agreementToAgreementGWT(agreementResult);
        }
        return agreementGWT;
    }
}
