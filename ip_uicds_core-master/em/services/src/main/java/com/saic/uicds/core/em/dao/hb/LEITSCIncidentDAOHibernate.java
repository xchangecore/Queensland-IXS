package com.saic.uicds.core.em.dao.hb;

import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.saic.uicds.core.dao.hb.GenericHibernateDAO;
import com.saic.uicds.core.em.dao.LEITSCIncidentDAO;
import com.saic.uicds.core.em.model.LEITSCIncident;

/**
 * LEITSCIncidentDAOHibernate
 * 
 * @author: Aruna Hau
 */
public class LEITSCIncidentDAOHibernate extends GenericHibernateDAO<LEITSCIncident, Integer>
    implements LEITSCIncidentDAO {

    @Override
    public LEITSCIncident findByIncident(String incidentID) {
        Criterion criterion = Restrictions.eq("incidentID", incidentID);
        List<LEITSCIncident> leitscIncidents = findByCriteria(criterion);

        return (leitscIncidents != null && leitscIncidents.size() != 0) ? leitscIncidents.get(0)
            : null;
    }

    @Override
    public LEITSCIncident findByLEITSCIncident(String leitscIncidentID) {
        Criterion criterion = Restrictions.eq("leitscIncidentID", leitscIncidentID);
        List<LEITSCIncident> leitscIncidents = findByCriteria(criterion);

        return (leitscIncidents != null && leitscIncidents.size() != 0) ? leitscIncidents.get(0)
            : null;
    }

}
