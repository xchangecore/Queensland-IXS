package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.saic.uicds.core.em.adminconsole.client.model.IncidentGWT;

public interface IncidentManagementServiceProxy extends RemoteService {

    public String archiveIncident(String incidentID);

    public String closeIncident(String incidentID);

    public String closeAndArchiveIncident(String incidentID);

    /**
     * 
     * @param incidentID
     * @return
     */
    public IncidentGWT getIncidentInfo(String incidentID);

    /**
     * Returns a list all incidents for this core
     * 
     * @return
     */
    public List<IncidentGWT> getListOfIncidents();

    public List<IncidentGWT> getListOfWorkProductIncidents(String incidentId);
    
    public boolean isIncidentActive(String itemName);
}
