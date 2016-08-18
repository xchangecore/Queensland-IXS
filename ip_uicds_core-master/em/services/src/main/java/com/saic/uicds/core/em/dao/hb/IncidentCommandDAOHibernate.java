/**
 * 
 */
package com.saic.uicds.core.em.dao.hb;

import com.saic.uicds.core.dao.hb.GenericHibernateDAO;
import com.saic.uicds.core.em.dao.IncidentCommandDAO;
import com.saic.uicds.core.em.model.IncidentCommandStructure;

public class IncidentCommandDAOHibernate extends
        GenericHibernateDAO<IncidentCommandStructure, String> implements IncidentCommandDAO {

    // @Override
    // public IncidentCommandStructure findByIncidentId(String id) {
    //
    // List<IncidentCommandStructure> structures = null;
    // structures = findAll();
    // for (IncidentCommandStructure structure : structures)
    // log.debug("ID: " + structure.getId());
    // Criterion criterion = Expression.eq("id", id);
    // try {
    // structures = findByCriteria(criterion);
    // } catch (Exception e) {
    // // TODO: handle exception
    // e.printStackTrace();
    // }
    // return structures.size() == 0 ? null : structures.get(0);
    // }
}