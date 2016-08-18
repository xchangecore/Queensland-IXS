package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.saic.uicds.core.em.adminconsole.client.model.IncidentGWT;

public interface IncidentManagementServiceProxyAsync {

	void archiveIncident(String incidentID, AsyncCallback<String> callback);

	void closeIncident(String incidentID, AsyncCallback<String> callback);

	/**
	 * 
	 * @param incidentID
	 * @return
	 */
	void getIncidentInfo(String incidentID, AsyncCallback<IncidentGWT> callback);

	/**
	 * Returns a list all incidents for this core
	 * 
	 * @return
	 */
	void getListOfIncidents(AsyncCallback<List<IncidentGWT>> callback);

	void getListOfWorkProductIncidents(String incidentId,
			AsyncCallback<List<IncidentGWT>> asyncCallback);

	void isIncidentActive(String itemName, AsyncCallback<Boolean> asyncCallback);

	void closeAndArchiveIncident(String incidentID,
			AsyncCallback<String> asyncCallback);

}
