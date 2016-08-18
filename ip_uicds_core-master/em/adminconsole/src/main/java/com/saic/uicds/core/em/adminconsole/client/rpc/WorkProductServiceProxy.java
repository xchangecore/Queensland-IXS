package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.saic.uicds.core.em.adminconsole.client.model.WorkProductGWT;

public interface WorkProductServiceProxy extends RemoteService {

    public String archiveProduct(String productID);

    public String closeProduct(String productID);

    public List<WorkProductGWT> findByIncidentAndType(String incidentID, String productType);

    public WorkProductGWT getProduct(String productID, boolean showDefaultWP);

    public WorkProductGWT getProduct(String productID, String xsltId, boolean showDefaultWP);

    public List<WorkProductGWT> getWorkProductListChildren(WorkProductGWT workProduct);

    public List<WorkProductGWT> listAllWorkProducts();

    public List<WorkProductGWT> listByProductType(String type);

    public List<String> getProductXsltIds(String wpID);
    
    public String getXsltConfiguredDirectory();
}
