/**
 * 
 */
package com.saic.uicds.core.infrastructure.dao.hb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.saic.uicds.core.dao.hb.GenericHibernateDAO;
import com.saic.uicds.core.infrastructure.dao.AgreementDAO;
import com.saic.uicds.core.infrastructure.model.Agreement;
import com.saic.uicds.core.infrastructure.model.ShareRule;

/**
 * @author summersw
 * 
 */
public class AgreementDAOHibernate extends GenericHibernateDAO<Agreement, String> implements
        AgreementDAO {

    Logger log = LoggerFactory.getLogger(AgreementDAOHibernate.class);

    @Override
    public List<Agreement> findAll() {
        List<Agreement> all = super.findAll();
        Set<Agreement> set = new HashSet<Agreement>(all);
        List<Agreement> ret = new ArrayList<Agreement>(set);
        return ret;
    }

    @Override
    public Agreement findByCoreID(String coreID) {

        // TODO: use some kind of find by criterion or example here instead of brute force search
        List<Agreement> list = findAll();
        for (Agreement a : list) {
            if (a.getRemoteCore().getValue().equalsIgnoreCase(coreID)) {
                return a;
            }
        }
        return null;

        // System.out.println("FIND by crit START");
        // CodeSpaceValueType remoteCore = new CodeSpaceValueType();
        // remoteCore.setValue(coreID);
        // Criterion criterion = Restrictions.eq("principals", remoteCore);
        // List<Agreement> agreements = findByCriteria(criterion);
        // System.out.println("FIND by crit END size: "+agreements.size());
        // if (agreements.size() == 1) {
        // return agreements.get(0);
        // }
        // else if (agreements.size() > 1) {
        // log.error("More than one agreement found for "+coreID);
        // System.out.println("MORE than one ageementDAOHib");
        // return null;
        // }
        // else {
        // System.out.println("none ageementDAOHib");
        // return null;
        // }

    }

    @SuppressWarnings("unchecked")
    public List<Agreement> getAgreementsWithEnabledRules() {
    	
    	//below not necessary  -- FLI 11/16/2011
    	/*  ???? why need those?
        List<Agreement> list = findAll();
        for (Agreement a : list) {
            System.out.println("Agreement " + a.getId() + " rules size: "
                    + a.getShareRules().size());
        }
	 
    	
        Criteria crit = getSession().createCriteria(getPersistentClass());
        Criteria rulesCrit = crit.createCriteria("shareRules");
        rulesCrit.add(Restrictions.eq("enabled", true));
        List<Agreement> l = crit.list();
        System.out.println("NEW size: " + l.size());
	*/
        ShareRule rule = new ShareRule();
        rule.setEnabled(true);
        Criterion criterion = Restrictions.eq("shareRules", rule);
        // Restrictions.
        // Criterion criterion = org.hibernate.criterion.Restrictions.isNotNull("shareRules");
        // criterion.
        List<Agreement> agreements = findByCriteria(criterion);
        System.out.println("FIND by crit END size: " + agreements.size());
        System.out.println("rules size: " + agreements.get(0).getShareRules().size());
        if (agreements.size() == 1) {
            return agreements;
        } else {
            System.out.println("none ageementDAOHib");
            return null;
        }
    }

}
