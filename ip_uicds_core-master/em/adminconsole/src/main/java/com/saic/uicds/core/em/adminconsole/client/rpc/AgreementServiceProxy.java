package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.saic.uicds.core.em.adminconsole.client.model.AgreementGWT;

public interface AgreementServiceProxy extends RemoteService {

    public AgreementGWT createAgreement(AgreementGWT request);

    public AgreementGWT getAgreement(String coreID);

    public List<AgreementGWT> getAgreementList();

    public List<AgreementGWT> getAgreementListChildren(AgreementGWT agreementGWT);

    public String rescindAgreement(String coreID);

    public AgreementGWT updateAgreement(AgreementGWT request);
}