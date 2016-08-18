/**
 * 
 */
package com.saic.uicds.core.infrastructure.dao.hb;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;

import com.saic.uicds.core.dao.hb.GenericHibernateDAO;
import com.saic.uicds.core.infrastructure.dao.NotificationDAO;
import com.saic.uicds.core.infrastructure.model.Agreement;
import com.saic.uicds.core.infrastructure.model.Notification;
import com.saic.uicds.core.infrastructure.model.NotificationSubscription;
import com.saic.uicds.core.infrastructure.model.ShareRule;

public class NotificationDAOHibernate extends GenericHibernateDAO<Notification, String> implements
    NotificationDAO {

    @Override
    public List<Notification> findAll() {

        List<Notification> all = super.findAll();
        Set<Notification> set = new HashSet<Notification>(all);
        List<Notification> ret = new ArrayList<Notification>(set);
        return ret;
    }

    public Notification findByEntityId(String entityID) {

        Notification notfication = null;
        Criterion criterion = Restrictions.eq("entityID", entityID);
        List<Notification> notifications = findByCriteria(criterion);

        return (notifications != null && notifications.size() != 0) ? notifications.get(0)
                : null;

    }
    
    public int findMsgCountByEntityId(String entityID) {

    	//fli added 11/29/2011
        Notification notfication = findByEntityId(entityID);
        return notfication.getMsgCount();

    }

    public List<Notification> findBySubscriptionId(Integer SubID)
    {  
    	
	/* hibernate associate property query not work well this way, but leave here as a future reference.
	List<Notification> nList = getSession().createCriteria(Notification.class)  //getPersistentClass()) 			
	.createCriteria("subscriptions")  
		        .add(Restrictions.eq("subscriptionID", new Integer(SubID)))  
		    //    .setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
		       .list();
	*/
    	
    	//we use hibernate root sql call and do this way.

     	String sql_query="SELECT ENTITY_ID FROM notification,notification_subscription " +
    	    			"WHERE notification.NOTIFICATION_ID = notification_subscription.NOTIFICATION_ID " +
    	    			"and notification_subscription.SUBSCRIPTION_ID=" + SubID;    
    	
    	Query query = getSession().createSQLQuery(sql_query);     
		@SuppressWarnings("unchecked")
		List<String> nList = query.list();
    	
		//see we got or not
		//System.out.println("Size= " + nList.size());     	
    	
		List<Notification> list =new ArrayList<Notification>();   	
    	Iterator<String> it=nList.iterator();
        while(it.hasNext())
        {
       	 String obj = it.next();
    
       	 //  see we got the right one or not
       	 //  System.out.println("Obj id= " + obj); 
	       	 Notification not=findByEntityId(obj);
	       	 if(not !=null)
	       	 {
	       		 list.add(not);
	       	 }
       
       }
        	    	
    	return list;
    
    }

}