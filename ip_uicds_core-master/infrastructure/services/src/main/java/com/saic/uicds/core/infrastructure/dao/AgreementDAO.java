/**
 * 
 */
package com.saic.uicds.core.infrastructure.dao;

import java.util.List;

import com.saic.uicds.core.dao.GenericDAO;
import com.saic.uicds.core.infrastructure.model.Agreement;

/**
 * @author summersw
 * 
 */
public interface AgreementDAO extends GenericDAO<Agreement, String> {

    Agreement findByCoreID(String coreID);
    
    public List<Agreement> getAgreementsWithEnabledRules();
    
    //Agreement findByTimeInterval(??);
    //Agreement findByPolygon(??);

}
