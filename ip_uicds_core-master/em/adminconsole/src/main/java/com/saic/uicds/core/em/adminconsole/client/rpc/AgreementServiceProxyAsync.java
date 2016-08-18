package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.saic.uicds.core.em.adminconsole.client.model.AgreementGWT;

public interface AgreementServiceProxyAsync {

    void createAgreement(AgreementGWT request, AsyncCallback<AgreementGWT> callback);

    void getAgreement(String coreID, AsyncCallback<AgreementGWT> callback);

    void getAgreementList(AsyncCallback<List<AgreementGWT>> callback);

    void getAgreementListChildren(AgreementGWT agreementGWT,
        AsyncCallback<List<AgreementGWT>> callback);

    void rescindAgreement(String coreID, AsyncCallback<String> callback);

    void updateAgreement(AgreementGWT request, AsyncCallback<AgreementGWT> callback);
}