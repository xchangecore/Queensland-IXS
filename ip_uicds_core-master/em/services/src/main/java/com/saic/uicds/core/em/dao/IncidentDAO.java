package com.saic.uicds.core.em.dao;

import java.util.List;

import com.saic.uicds.core.dao.GenericDAO;
import com.saic.uicds.core.em.model.Incident;

/**
 * IncidentDAO
 * 
 * @author created: package: com.saic.dctd.uicds.core.dao
 */
public interface IncidentDAO extends GenericDAO<Incident, Integer> {

    public void delete(String incidentId, boolean isDelete);

    public List<Incident> findAll();

    public List<Incident> findAllClosedIncident();

    public Incident findByIncidentID(String incidentId);

    public boolean isActive(String incidentId);
}
