package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.saic.uicds.core.em.adminconsole.client.model.IncidentGWT;
import com.saic.uicds.core.em.adminconsole.client.model.InterestGroupGWT;

public interface InterestGroupManagementServiceProxy extends RemoteService {

	public List<InterestGroupGWT> getListOfInterestGroups();

	public List<InterestGroupGWT> getListOfInterestGroupsWithInstances();

	public InterestGroupGWT getListOfIGInstances(String interestGroupType);

	public List<IncidentGWT> getAssociatedWorkProducts(
			String interestGroupType, String igInstanceName);

	public String closeArchiveAllInstancesOfIG(String igType);
}
