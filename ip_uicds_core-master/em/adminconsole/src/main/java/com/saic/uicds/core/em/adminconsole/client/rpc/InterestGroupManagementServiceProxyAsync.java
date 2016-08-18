package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.saic.uicds.core.em.adminconsole.client.model.IncidentGWT;
import com.saic.uicds.core.em.adminconsole.client.model.InterestGroupGWT;

public interface InterestGroupManagementServiceProxyAsync {

	void getListOfInterestGroups(AsyncCallback<List<InterestGroupGWT>> callback);

	void getListOfInterestGroupsWithInstances(
			AsyncCallback<List<InterestGroupGWT>> callback);

	void getListOfIGInstances(String interestGroupType,
			AsyncCallback<InterestGroupGWT> callback);
	
	void getAssociatedWorkProducts(String interestGroupType, String igInstanceName,
			AsyncCallback<List<IncidentGWT>> callback);
	
	void closeArchiveAllInstancesOfIG(String igType, AsyncCallback<String> callback);
}
