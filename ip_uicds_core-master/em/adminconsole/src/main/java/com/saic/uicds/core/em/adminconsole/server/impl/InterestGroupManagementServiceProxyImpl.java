package com.saic.uicds.core.em.adminconsole.server.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.uicds.incidentManagementService.IncidentInfoType;
import org.uicds.incidentManagementService.IncidentListType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.saic.uicds.core.em.adminconsole.client.model.IGInstanceGWT;
import com.saic.uicds.core.em.adminconsole.client.model.IncidentGWT;
import com.saic.uicds.core.em.adminconsole.client.model.InterestGroupGWT;
import com.saic.uicds.core.em.adminconsole.client.rpc.InterestGroupManagementServiceProxy;
import com.saic.uicds.core.em.service.IncidentManagementService;
import com.saic.uicds.core.infrastructure.exceptions.InvalidInterestGroupIDException;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.InterestGroupManagementComponent;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.InterestGroupInfo;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

public class InterestGroupManagementServiceProxyImpl extends
		RemoteServiceServlet implements InterestGroupManagementServiceProxy {

	private static final long serialVersionUID = -6482575875975540574L;

	// load the interestGroupManagementComponent

	private InterestGroupManagementComponent service = null;

	private WorkProductService workProductService = null;

	private IncidentManagementService incidentManagementService = null;

	public IncidentManagementService getIncidentManagementService() {
		if (this.incidentManagementService == null) {
			loadIncidentService();
		}
		return this.incidentManagementService;
	}

	public InterestGroupManagementComponent getService() {
		if (this.service == null) {
			loadService();
		}
		return this.service;
	}

	public WorkProductService getWorkProductService() {

		if (this.workProductService == null) {
			loadWorkProductService();
		}
		return this.workProductService;
	}

	private boolean loadService() {

		WebApplicationContext springContext = WebApplicationContextUtils
				.getWebApplicationContext(this.getServletContext());
		this.service = (InterestGroupManagementComponent) springContext
				.getBean("interestGroupManagementComponent");
		if (service == null) {
			throw new RuntimeException(
					"Unable to load InterestGroupManagementComponent!");
		} else {
			return true;
		}
	}

	private boolean loadWorkProductService() {

		WebApplicationContext springContext = WebApplicationContextUtils
				.getWebApplicationContext(this.getServletContext());
		this.workProductService = (WorkProductService) springContext
				.getBean("workProductService");
		if (workProductService == null) {
			throw new RuntimeException("Unable to load WorkProductService!");
		} else {
			return true;
		}
	}

	private boolean loadIncidentService() {

		WebApplicationContext springContext = WebApplicationContextUtils
				.getWebApplicationContext(this.getServletContext());
		this.incidentManagementService = (IncidentManagementService) springContext
				.getBean("incidentManagementService");
		if (incidentManagementService == null) {
			throw new RuntimeException("Unable to load IncidentService!");
		} else {
			return true;
		}
	}

	@Override
	public List<InterestGroupGWT> getListOfInterestGroups() {
		List<InterestGroupInfo> interestGroups = getService()
				.getInterestGroupList();
		List<InterestGroupGWT> listGWT = new ArrayList<InterestGroupGWT>(
				interestGroups.size());
		for (InterestGroupInfo ig : interestGroups) {
			InterestGroupGWT igGWT;
			igGWT = new InterestGroupGWT(ig.getName());
			igGWT.setName(ig.getName());
			igGWT.setInterestGroupID(ig.getInterestGroupID());
			igGWT.setSummary(ig.getDescription());
			igGWT.setInterestGroupType(ig.getInterestGroupType());
			igGWT.setInterestGroupSubType(ig.getInterestGroupSubType());
			listGWT.add(igGWT);
		}
		return listGWT;
	}

	@Override
	public List<InterestGroupGWT> getListOfInterestGroupsWithInstances() {
		List<InterestGroupInfo> interestGroups = getService()
				.getInterestGroupList();
		Map<String, InterestGroupGWT> mapGWT = new HashMap<String, InterestGroupGWT>();
		/*
		 * 1. Interest Group Id: IG-e3d84e3d-9608-40e4-9344-e043c770ea76, Name:
		 * Earthquake, Type: Incident, OwningCore: uicds@santhosha-pc 2.
		 * Interest Group Id: IG-9187ed0a-a319-46a4-9498-d2b42b3a7bce, Name:
		 * chemical event has occurred, Type: Incident, OwningCore:
		 * uicds@santhosha-pc]
		 */
		for (InterestGroupInfo ig : interestGroups) {
			InterestGroupGWT igGWT = mapGWT.get(ig.getInterestGroupType());
			IGInstanceGWT instanceGWT = new IGInstanceGWT();
			instanceGWT.setInstanceName(ig.getName());
			instanceGWT.setInterestGroupID(ig.getInterestGroupID());
			instanceGWT.setInterestGroupSubType(ig.getInterestGroupSubType());
			instanceGWT.setSummary(ig.getDescription());
			String workProductID = getWorkProductID(ig);
			instanceGWT.setWorkProductID(workProductID);
			if (igGWT == null) {
				igGWT = new InterestGroupGWT(ig.getInterestGroupType());
				igGWT.setInterestGroupType(ig.getInterestGroupType());
				mapGWT.put(ig.getInterestGroupType(), igGWT);
			}
			igGWT.getIgInstances().add(instanceGWT);
		}
		List<InterestGroupGWT> igList = new ArrayList<InterestGroupGWT>();
		for (InterestGroupGWT ig : mapGWT.values()) {
			igList.add(ig);
		}
		return igList;
	}

	private String getWorkProductID(InterestGroupInfo ig) {
		String igType = ig.getInterestGroupType();
		String igId = ig.getInterestGroupID();
		WorkProduct[] workProductList = getWorkProductService()
				.getAssociatedWorkProductList(igId);
		for (WorkProduct workProduct : workProductList) {
			if (workProduct.getProductID().contains(igType))
				return workProduct.getProductID();
		}
		return null;
	}

	@Override
	public InterestGroupGWT getListOfIGInstances(String interestGroupType) {
		List<InterestGroupGWT> igInstances = getListOfInterestGroupsWithInstances();
		for (InterestGroupGWT ig : igInstances) {
			if (ig.getInterestGroupType().equals(interestGroupType))
				return ig;
		}
		return null;
	}

	@Override
	public List<IncidentGWT> getAssociatedWorkProducts(
			String interestGroupType, String igInstanceName) {
		ArrayList<IncidentGWT> workProductIncidentGWTList = new ArrayList<IncidentGWT>();

		WorkProduct[] workProductList = getWorkProductService()
				.getAssociatedWorkProductList(igInstanceName);
		for (WorkProduct workProduct : workProductList) {
			IncidentGWT incidentGWT = new IncidentGWT(
					workProduct.getProductID());
			incidentGWT.setWorkProductID(workProduct.getProductID());
			incidentGWT.setLeaf(true);
			if (workProduct.getDigest() != null) {
				incidentGWT.setDigest(true);
			} else {
				incidentGWT.setDigest(false);
			}
			workProductIncidentGWTList.add(incidentGWT);
		}
		return workProductIncidentGWTList;
	}

	@Override
	public String closeArchiveAllInstancesOfIG(String igType) {
		String message = "Close and Archive All:\n";
		if (igType.equalsIgnoreCase("Incident")) {
			message += closeArchiveAllInstancesOfIncident();
		} else {
			InterestGroupGWT interestGroupGWT = getListOfIGInstances(igType);
			List<IGInstanceGWT> igInstances = interestGroupGWT.getIgInstances();
			for (IGInstanceGWT igInstance : igInstances) {
				String igId = igInstance.getInterestGroupID();
				String igMessage = closeArchiveInstancesOfIG(igId);
				message += igMessage + "\n";
			}
		}
		return message;
	}

	private String closeArchiveAllInstancesOfIncident() {
		String message="";
		IncidentListType listOfIncidents = getIncidentManagementService().getListOfIncidents();
		IncidentInfoType[] incidents = listOfIncidents.getIncidentInfoArray();
		for(IncidentInfoType incident:incidents){
			String incidentID=incident.getId();
			String closeArchiveIncident=closeAndArchiveIncident(incidentID);
			message+=closeArchiveIncident+"\n";
		}
		return message;
	}

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
	
	private String closeArchiveInstancesOfIG(String igId) {
		String name = getService().getInterestGroup(igId).getName();
		String igMessage = " => Close and Archive " + name + ": ";
		WorkProduct[] wps = getWorkProductService()
				.getAssociatedWorkProductList(igId);
		boolean allWpsClosed = true;
		for (WorkProduct wp : wps) {
			ProductPublicationStatus wpCloseStatus = getWorkProductService()
					.closeProduct(
							WorkProductHelper.getWorkProductIdentification(wp));
			if (wpCloseStatus.getStatus().equals(
					ProductPublicationStatus.SuccessStatus)) {
				ProductPublicationStatus wpArchiveStatus = getWorkProductService()
						.archiveProduct(
								WorkProductHelper
										.getWorkProductIdentification(wp));
				if (wpArchiveStatus.getStatus().equals(
						ProductPublicationStatus.SuccessStatus)) {
				} else {
					igMessage += "\n  - Archive Operation Failure: "
							+ wpArchiveStatus.getReasonForFailure() + "\n";
					allWpsClosed = false;
				}
			} else {
				igMessage += "\n - Close Operation Failure: "
						+ wpCloseStatus.getReasonForFailure() + "\n";
				allWpsClosed = false;
			}
		}
		if (allWpsClosed) {
			try {
				getService().deleteInterestGroup(igId);
				igMessage += "Success";
			} catch (InvalidInterestGroupIDException e) {
				e.printStackTrace();
				igMessage += "Failure - " + e.getMessage() + "\n";
			}
		}
		return igMessage;
	}
}
