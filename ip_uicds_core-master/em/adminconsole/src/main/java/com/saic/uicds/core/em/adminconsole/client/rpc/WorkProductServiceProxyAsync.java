package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.saic.uicds.core.em.adminconsole.client.model.WorkProductGWT;

public interface WorkProductServiceProxyAsync {

    void archiveProduct(String productID, AsyncCallback<String> callback);

    void closeProduct(String productID, AsyncCallback<String> callback);

    void findByIncidentAndType(String incidentID, String productType,
            AsyncCallback<List<WorkProductGWT>> callback);

    void getProduct(String productID,boolean showDefaultWP,AsyncCallback<WorkProductGWT> callback);

    void getProduct(String productID, String xsltId,boolean showDefaultWP,AsyncCallback<WorkProductGWT> callback);

    void getWorkProductListChildren(WorkProductGWT workProduct,
            AsyncCallback<List<WorkProductGWT>> callback);

    void listAllWorkProducts(AsyncCallback<List<WorkProductGWT>> callback);

    void listByProductType(String type, AsyncCallback<List<WorkProductGWT>> callback);

    void getProductXsltIds(String wpID, AsyncCallback<List<String>> callback);

    void getXsltConfiguredDirectory(AsyncCallback<String> callback);
}
