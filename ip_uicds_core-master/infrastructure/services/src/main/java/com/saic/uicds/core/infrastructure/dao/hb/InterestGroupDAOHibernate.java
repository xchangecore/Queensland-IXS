package com.saic.uicds.core.infrastructure.dao.hb;

import java.util.List;

import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.saic.uicds.core.dao.hb.GenericHibernateDAO;
import com.saic.uicds.core.infrastructure.dao.InterestGroupDAO;
import com.saic.uicds.core.infrastructure.model.InterestGroup;
import com.saic.uicds.core.infrastructure.model.WorkProduct;

public class InterestGroupDAOHibernate
    extends GenericHibernateDAO<InterestGroup, Integer>
    implements InterestGroupDAO {

    @Transient
    private Logger log = LoggerFactory.getLogger(InterestGroupDAOHibernate.class);

    @Override
    public InterestGroup findByInterestGroup(String interestGroupID) {
    	
    	if (log.isDebugEnabled())
    		log.debug("findByInterestGroup. InterestGroupID="+interestGroupID);

        Criterion criterion = Restrictions.eq("interestGroupID", interestGroupID);
        List<InterestGroup> interestGroups = findByCriteria(criterion);

        if (log.isDebugEnabled())
        	log.debug("found "+ ((interestGroups!= null)? interestGroups.size() : 0));
        	
        return (interestGroups != null && interestGroups.size() != 0)
            ? interestGroups.get(0)
            : null;
    }

    @Override
    public List<InterestGroup> findByOwningCore(String owningCore) {

    	if (log.isDebugEnabled())
    		log.debug("findByOwningCore - "+owningCore);
    	
        Criterion criterion = Restrictions.eq("owningCore", owningCore);
        List<InterestGroup> interestGroups = findByCriteria(criterion);

        if (log.isDebugEnabled())
        	log.debug("found "+((interestGroups!=null)? interestGroups.size():0));
        	
        return interestGroups;
    }

    @Override
    public void delete(String interestGroupID, boolean isDelete) {

    	if (log.isDebugEnabled())
    		log.debug("delete - "+interestGroupID);
    	
        InterestGroup interestGroup = findByInterestGroup(interestGroupID);
        if (interestGroup == null)
            return;

        if (isDelete == true) {
            makeTransient(interestGroup);
        } else {
            interestGroup.setActive(false);
            makePersistent(interestGroup);
        }
    }
    
    @Override
    public InterestGroup makePersistent(InterestGroup entity) {

    	if (log.isDebugEnabled())
    		log.debug("makePersistent. IG="+ ((entity!=null)? entity.getInterestGroupID(): "null"));
    	Session s = getSession();
    	
    	s.saveOrUpdate(entity);
    	
    	if (log.isDebugEnabled())
    		log.debug("makePersistent - complete");
    	
    	return entity;
    }

}
