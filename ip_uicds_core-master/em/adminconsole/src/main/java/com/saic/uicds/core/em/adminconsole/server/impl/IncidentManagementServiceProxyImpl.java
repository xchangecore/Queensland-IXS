package com.saic.uicds.core.em.adminconsole.server.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.uicds.incidentManagementService.IncidentInfoType;
import org.uicds.incidentManagementService.IncidentListType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.saic.uicds.core.em.adminconsole.client.model.IncidentGWT;
import com.saic.uicds.core.em.adminconsole.client.rpc.IncidentManagementServiceProxy;
import com.saic.uicds.core.em.service.IncidentManagementService;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;

public class IncidentManagementServiceProxyImpl extends RemoteServiceServlet implements
    IncidentManagementServiceProxy {

    /**
     * Serializable
     */
    private static final long serialVersionUID = 920044164411975496L;

    // load the incident management service

    private IncidentManagementService service = null;

    private WorkProductService workProductService = null;

    @Override
	public String closeAndArchiveIncident(String incidentID) {
        String returnMessage = "Close and Archive Incident: " + incidentID + " ";

        ProductPublicationStatus closeStatus = getIncidentManagementService().closeIncident(incidentID);
        if (closeStatus.getStatus().equals(ProductPublicationStatus.SuccessStatus)){
        	returnMessage += "\nClose: "+closeStatus.getStatus();
        	ProductPublicationStatus archiveStatus = getIncidentManagementService().archiveIncident(incidentID);
        	if(archiveStatus.getStatus().equals(ProductPublicationStatus.SuccessStatus))
                returnMessage += "\nArchive: "+archiveStatus.getStatus();
            else
                returnMessage += "\nArchive Operation Failure: " + closeStatus.getReasonForFailure();
        }else{
        	returnMessage += "\nClose Operation Failure: " + closeStatus.getReasonForFailure();
        }
        return returnMessage;
	}
    
    @Override
    public String archiveIncident(String incidentID) {

        String returnMessage = "Archive Incident: " + incidentID;

        ProductPublicationStatus status = getIncidentManagementService().archiveIncident(incidentID);

        if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus))
            returnMessage += status.getStatus();
        else
            returnMessage += " Failed: " + status.getReasonForFailure();

        return returnMessage;
    }

    // load the work product service

    @Override
    public String closeIncident(String incidentID) {

        String returnMessage = "Close Incident: " + incidentID + " ";

        ProductPublicationStatus status = getIncidentManagementService().closeIncident(incidentID);
        if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus))
            returnMessage += status.getStatus();
        else
            returnMessage += " Failure: " + status.getReasonForFailure();

        return returnMessage;
    }
    
    public boolean isIncidentActive(String incidentId) {
        WorkProduct wp=getWorkProductService().getProduct(incidentId);
        boolean active=wp.getActive();
        return active;
    }


    public IncidentGWT getIncidentInfo(String incidentID) {

        IncidentInfoType data = getIncidentManagementService().getIncidentInfo(incidentID);
        
        IncidentGWT value = new IncidentGWT();
        value.setWorkProductID(data.getWorkProductIdentification().getIdentifier().getStringValue());
        value.setTitle(data.getName());
        value.setIncidentID(data.getId());
        value.setSummary(data.getDate());
        return value;
    }

    private IncidentManagementService getIncidentManagementService() {

        if (this.service == null) {
            boolean loaded = loadService();
        }
        return this.service;
    }

    public List<IncidentGWT> getListOfIncidents() {

        IncidentListType incidentTypes = getIncidentManagementService().getListOfIncidents();
        IncidentInfoType[] incidents = incidentTypes.getIncidentInfoArray();
        List<IncidentGWT> listGWT = new ArrayList<IncidentGWT>(incidents.length);

        for (IncidentInfoType value : incidents) {
            IncidentGWT incident;
            incident = new IncidentGWT(value.getName());
            incident.setIncidentID(value.getId());
            incident.setIsIncident(true);
            incident.setWorkProductID(value.getWorkProductIdentification().getIdentifier().getStringValue());
            incident.setDigest(true);
            listGWT.add(incident);
        }
        return listGWT;
    }

    public List<IncidentGWT> getListOfWorkProductIncidents(String incidentId) {

        ArrayList<IncidentGWT> workProductIncidentGWTList = new ArrayList<IncidentGWT>();

        WorkProduct[] workProductList = getWorkProductService().getAssociatedWorkProductList(
            incidentId);
        for (WorkProduct workProduct : workProductList) {
            IncidentGWT incidentGWT = new IncidentGWT(workProduct.getProductID());
            incidentGWT.setWorkProductID(workProduct.getProductID());
            incidentGWT.setLeaf(true);
            if(workProduct.getDigest()!=null){
                incidentGWT.setDigest(true);
            }else{
                incidentGWT.setDigest(false);
            }
            workProductIncidentGWTList.add(incidentGWT);
        }
        return workProductIncidentGWTList;
    }

    private IncidentGWT[] getWorkProductGWTArray() {

        ArrayList<IncidentGWT> workProductGWTArrayList = new ArrayList<IncidentGWT>();
        IncidentGWT incidentGWT1 = new IncidentGWT("dude");
        incidentGWT1.setLeaf(true);
        workProductGWTArrayList.add(incidentGWT1);
        IncidentGWT incidentGWT2 = new IncidentGWT("dude1");
        incidentGWT2.setLeaf(true);
        workProductGWTArrayList.add(incidentGWT2);

        int i = 0;
        IncidentGWT[] workProductGWTArray = new IncidentGWT[workProductGWTArrayList.size()];
        for (IncidentGWT incidentGWT : workProductGWTArrayList) {
            workProductGWTArray[i++] = incidentGWT;
        }
        return workProductGWTArray;
    }

    private WorkProductService getWorkProductService() {

        if (this.workProductService == null) {
            boolean loaded = loadWorkProductService();
        }
        return this.workProductService;
    }

    private boolean loadService() {

        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        this.service = (IncidentManagementService) springContext.getBean("incidentManagementService");
        if (service == null) {
            throw new RuntimeException("Unable to load IncidentManagementService!");
        } else {
            return true;
        }
    }

    private boolean loadWorkProductService() {

        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        this.workProductService = (WorkProductService) springContext.getBean("workProductService");
        if (workProductService == null) {
            throw new RuntimeException("Unable to load WorkProductService!");
        } else {
            return true;
        }
    }

}
