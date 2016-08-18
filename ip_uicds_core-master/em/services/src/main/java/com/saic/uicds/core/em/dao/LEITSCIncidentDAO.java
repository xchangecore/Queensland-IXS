package com.saic.uicds.core.em.dao;

import com.saic.uicds.core.dao.GenericDAO;
import com.saic.uicds.core.em.model.LEITSCIncident;

public interface LEITSCIncidentDAO extends GenericDAO<LEITSCIncident, Integer> {

    public LEITSCIncident findByIncident(String incidentID);

    public LEITSCIncident findByLEITSCIncident(String leitscIncidentID);
}
