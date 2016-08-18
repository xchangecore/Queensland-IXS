package com.saic.uicds.core.infrastructure.dao.mssql;

import gov.ucore.ucore.x20.DigestDocument;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.uicds.core.dao.hb.GenericHibernateDAO;
import com.saic.uicds.core.infrastructure.dao.WorkProductDAO;
import com.saic.uicds.core.infrastructure.model.WorkProduct;

public class WorkProductMSSQLDB2DAO
    extends GenericHibernateDAO<WorkProduct, Integer>
    implements WorkProductDAO {

    private Logger log = LoggerFactory.getLogger(WorkProductMSSQLDB2DAO.class);

    /**
     * Make the entity persistent. If the entity is already persistent, then ensure that it is
     * saved.
     */
    @SuppressWarnings("static-access")
    @Override
    public WorkProduct makePersistent(WorkProduct entity) {

    	if (log.isInfoEnabled())
    		log.info("WorkProduct::makePersistent");
    	Session s = getSession();
    	
    	if (entity == null)
    		return null;
    	
    	if (log.isDebugEnabled())
    		log.debug("makePersistent - entity="+entity.toString());
        	
        Integer id = entity.getId();
        String productID = entity.getProductID();
        
    	WorkProduct wp = (productID !=null && !productID.equals(""))? findByProductID(productID) :null;
    	boolean foundProductByProductID = (wp != null);
        WorkProduct existingWP = (id != null) ? (WorkProduct) s.get(WorkProduct.class, id) : null;       
        boolean hasExistingWP = (existingWP != null);       
        //String existingWPProductID = (hasExistingWP)? existingWP.getProductID() : null;

        // no matter single core or multi core communication, as long as that id/workproduct
        // has the same productID as the incoming work product's, then update it.
        // for multi core transaction, id will not be good to use because they are most
        // likely
        // not the same, we should use ProductID.

        //Insert
        if  (!hasExistingWP && !foundProductByProductID)
        {
            // insert a new workproduct
        	if (log.isInfoEnabled())
        		log.info("insert a new workproduct. ProductID="+productID);
            s.save(entity);
 //           s.refresh(entity);
        }       
        //Update
        else if (foundProductByProductID)
        {
        	if (log.isInfoEnabled())
        		log.info("Update workproduct. Incoming id ="+id+
        				"; Existing id (found by productId)="+wp.getId()+
        				"; ProductID="+productID);
        	wp = (WorkProduct) s.get(WorkProduct.class, wp.getId(), LockMode.UPGRADE);
        	copyWorkProduct(entity, wp);
    		s.update(wp);
        }
        /*
        else if ((id == null) && foundProductByProductID)
        {
        	if (log.isInfoEnabled())
        		log.info("Id is null, but found the workproduct with same productId. productId="+productID+". Update the workproduct.");
        	wp = (WorkProduct) s.get(WorkProduct.class, wp.getId(), LockMode.UPGRADE);
        	copyWorkProduct(entity, wp);
    		s.update(wp);
 //   		entity.setId(wp.getId());  
//        	s.merge(entity);
        }
        else if ((id != null ) && foundProductByProductID){
        	if (log.isInfoEnabled())
        		log.info("Update workproduct. incoming Id="+id+", wp id (found by productId)="+wp.getId()+", productID="+productID);
    		wp = (WorkProduct) s.get(WorkProduct.class, wp.getId(), LockMode.UPGRADE);
        	copyWorkProduct(entity, wp);
        	s.update(wp);
//        	entity.setId(wp.getId());  
//        	s.merge(entity);
        }
        */
        else if (!foundProductByProductID && hasExistingWP) {
        	if (log.isInfoEnabled())
        		log.info("Update workproduct, id="+id+", productID="+productID+". ?Should not happen - no productID?");
        	existingWP = (WorkProduct) s.load(WorkProduct.class, id, LockMode.UPGRADE);
        	copyWorkProduct(entity, existingWP);
    		s.update(existingWP);        	
        }
        else 
        {
        	log.error("makePersistent. Unable to persist workproduct (id="+id+", productId="+productID+")");
        }
        return entity;
    }
    
    private void copyWorkProduct(WorkProduct src, WorkProduct dest){
    	log.info("copyWorkProduct");
		if ((src != null) && (dest != null)) {

			// Associated Interest Group IDs
			if (dest.getAssociatedInterestGroupIDs() != null)
				dest.getAssociatedInterestGroupIDs().clear();
			
	        if(src.getAssociatedInterestGroupIDs() !=null)
	        {
	        	if (dest.getAssociatedInterestGroupIDs() == null){
		        	 dest.setAssociatedInterestGroupIDs(new HashSet<String>());	        		
	        	}
	        	for (String igStr : src.getAssociatedInterestGroupIDs()) 
		        {
		        	dest.getAssociatedInterestGroupIDs().add(igStr);
		        }
	        }
	        
	        dest.setProductID(src.getProductID());
	        dest.setProductType(src.getProductType());
	        dest.setChecksum(src.getChecksum());
	        dest.setActive(src.getActive());
	        
	        dest.setMimeType(src.getMimeType());
	        dest.setProductVersion(src.getProductVersion());
	        dest.setSize(src.getSize());


	        dest.setCreatedBy(src.getCreatedBy());
	        dest.setCreatedDate(src.getCreatedDate());
	        dest.setUpdatedBy(src.getUpdatedBy());
	        dest.setUpdatedDate(src.getUpdatedDate());

	        // Create a new one for the new instances otherwise it is linked to the current entry
	        if (src.getDigest() != null) {
	            DigestDocument digest = DigestDocument.Factory.newInstance();
	            digest.setDigest(src.getDigest().getDigest());
	            dest.setDigest(digest);
	        }

	        dest.setProduct(src.getProduct().copy());
		}
		else if (src == null){
			log.error("copyWorkProduct: error - source workproduct cannot be null.");
		}
		else if (dest == null){
			log.info("copy into a new workproduct as destination.");
			dest = new WorkProduct (src);		
		}
    }
        	  
    /**
     * Make the entity transient. This will cause the entity to be removed from the database.
     * 
     * @param entity
     * @throws SQLException
     */

    @Override
    public void makeTransient(WorkProduct entity) {

    	if (log.isInfoEnabled())
    		log.info("makeTransient");
        // remove them after done the dao test
        archiveProduct(entity);
    }

    private void archiveProduct(WorkProduct entity) {

    	if (log.isInfoEnabled())
    		log.info("archiveProduct");
    	Session s = getSession();
        if (entity != null) {
            //String delSql = "delete from workproducts where ProductID ='"
            //    + entity.getProductID() + "'";
        	if (log.isInfoEnabled())
        		log.info("productID="+entity.getProductID()+", id="+entity.getId());
        	WorkProduct wp= findByProductID(entity.getProductID());
        	
        	if (wp != null){
            	if (log.isInfoEnabled())
            		log.info("Delete work product");
        		s.delete(wp);
        	}
        }
    }
    
    @Override
    public List<WorkProduct> findAllClosedVersionOfProduct(String productID) {

        //String searchSQL = "select * from workproducts where ProductID='" + productID + "' and active = false";
    	if (log.isInfoEnabled())
    		log.info("findAllClosedVersionOfProduct. productID="+productID);
    	

    	Session s = getSession();
        Query q = s.createQuery(
        		"from WorkProduct as wp where wp.productID = :productID and wp.active = :active "+
        		" order by wp.productVersion desc)");
        q.setParameter("productID", productID);
        q.setParameter("active", false);
        List<WorkProduct> list = q.list();

    	if (log.isInfoEnabled())
    		log.info(((list!=null)? list.size() : 0)+" records found.");
        return list;
    }

    @Override
    public List<WorkProduct> findAllVersionOfProduct(String productID) {
 	
        //String searchSQL = "select * from workproducts where ProductID='" + productID + "'";
    	if (log.isInfoEnabled())
    		log.info("findAllVersionOfProduct. productID="+productID);

    	Session s = getSession();
        Query q = s.createQuery(
        		"from WorkProduct as wp where wp.productID = :productID order by wp.productVersion desc)");
        q.setParameter("productID", productID);
        
        List<WorkProduct> list = q.list();

    	if (log.isInfoEnabled())
    		log.info(((list!=null)? list.size() : 0)+" records found.");
        
        return list;
    }

/*    
    public WorkProduct findLatestVersionOfProduct(String productID) {
 	
    	if (log.isInfoEnabled())
    		log.info("findLatestVersionOfProduct. productID="+productID);

    	Criteria c = getSession().createCriteria(getPersistentClass());
        c.add(Restrictions.eq("productID", productID));
        c.addOrder(Order.desc("productVersion"));
        
        List<WorkProduct> list = c.list();

        WorkProduct wp = (list != null)? list.get(0): null;

    	if (log.isInfoEnabled())
    		log.info(((list!=null)? list.size() : 0)+" records found.");
        
        return wp;
    }
*/
    @Override
    public List<WorkProduct> findByInterestGroup(String interestGroupID) {

        //String searchSQL = "select * from workproducts where AssociatedGroups='"
        //     + interestGroupID + "'";
    	if (log.isInfoEnabled())
    		log.info("findByInterestGroup. interestGroupID="+interestGroupID);
        Session s = getSession();
        Query q = s.createQuery(
        		"from WorkProduct as wp where :interestGroupID in elements(wp.associatedInterestGroupIDs)");
        q.setParameter("interestGroupID", interestGroupID);
        
        List<WorkProduct> list = q.list();
        
    	if (log.isInfoEnabled())
    		log.info(((list!=null)? list.size() : 0)+" records found.");
       
        return list;

    }

    @Override
    public List<WorkProduct> findByInterestGroupAndType(String interestGroupID, String type) {

        //String searchSQL = "select * from workproducts where AssociatedGroups='"
        //    + interestGroupID + "'" + " and WPType='" + type + "'";
    	if (log.isInfoEnabled())
    		log.info("findByInterestGroupAndType. interestGroupID="+interestGroupID+", productType="+type);

        Session s = getSession();
        Query q = s.createQuery(
        		"from WorkProduct as wp "+
        		"where wp.productType = :productType "+
        		"and :interestGroupID in elements(wp.associatedInterestGroupIDs)");
        q.setParameter("interestGroupID", interestGroupID);
        q.setParameter("productType", type);
        
        List<WorkProduct> list = q.list();
 
    	if (log.isInfoEnabled())
    		log.info(((list!=null)? list.size() : 0)+" records found.");
        
        return list;
    }

    @Override
    public WorkProduct findByProductID(String productID) {

        //String searchSQL = "select * from workproducts where ProductID='" + productID + "'";
    	if (log.isInfoEnabled())
    		log.info("findByProductID. productID="+productID);

    	Session s = getSession();
        Query q = s.createQuery(
        		"from WorkProduct as wp where wp.productID = :productID order by wp.productVersion desc)");
        q.setParameter("productID", productID);
        List<WorkProduct> list = q.list();
        
    	if (log.isInfoEnabled())
    		log.info( ((list!=null)? list.size(): 0) +" record found");

    	WorkProduct wp = null;
        if ((list != null && list.size() != 0) )
        {
        	wp = list.get(0);
        	if (log.isDebugEnabled())
        		log.debug("findByProductID. wp="+wp);
        }

        return wp;
    }

    @Override
    public WorkProduct findByProductIDAndVersion(String productID, Integer productVersion) {

        //String searchSQL = "select * from workproducts where ProductID='" + productID + "'"
        //    + " and ProductVersion =" + productVersion;
    	if (log.isInfoEnabled())
    		log.info("findByProductIDAndVersion. productID="+productID+", productVersion="+productVersion);

    	Session s = getSession();
        Query q = s.createQuery(
        		"from WorkProduct as wp where wp.productID = :productID and wp.productVersion = :productVersion");
        q.setParameter("productID", productID);
        q.setParameter("productVersion", productVersion);
        List<WorkProduct> list = q.list();
        
    	if (log.isInfoEnabled())
    		log.info( ((list!=null)? list.size(): 0) +" record found");

    	WorkProduct wp = null;
        if ((list != null && list.size() != 0) )
        {
        	wp = list.get(0);
        	if (log.isDebugEnabled())
        		log.debug("findByProductIDAndVersion. wp="+wp);
        }

        return wp;
    }

    @Override
    public List<WorkProduct> findByProductType(String productType) {

        //String searchSQL = "select * from workproducts where productType='" + productType + "'";
    	if (log.isInfoEnabled())
    		log.info("findByProductType. productType="+productType);

    	Session s = getSession();
        Query q = s.createQuery(
        		"from WorkProduct as wp where wp.productType = :productType");
        q.setParameter("productType", productType);
        List<WorkProduct> list = q.list();

    	if (log.isInfoEnabled())
    		log.info(((list!=null)? list.size() : 0)+" records found.");

        return list;
    }

    @Override
    public WorkProduct findByWorkProductIdentification(IdentificationType pkgId) {

    	if (log.isInfoEnabled())
    		log.info("findByWorkProductIdentification");
    	WorkProduct wp = null;
    	
        if (pkgId != null) {
            String productID = "";
            String cs = "";
            String state = "";
            String type = "";
            int productVersion = 0;

            if (pkgId.getIdentifier() != null) {
                productID = pkgId.getIdentifier().getStringValue();
            }

            if (pkgId.getChecksum() != null) {
                cs = pkgId.getChecksum().getStringValue();
            }

            if (pkgId.getType() != null) {
                type = pkgId.getType().getStringValue();
            }

            if (pkgId.getState() != null) {
                state = pkgId.getState().toString();
            }

            if (pkgId.getVersion() != null) {
                productVersion = Integer.parseInt(pkgId.getVersion().getStringValue());
            }

            //String searchSQL = "select * from workproducts where ProductVersion ="
            //    + productVersion + " and State ='" + state + "'" + " and WPType='" + type + "'"
            //    + " and ProductID='" + productID + "'" + " and WPChecksum='" + cs + "'";

        	if (log.isInfoEnabled())
        		log.info("productID="+productID+", productVersion="+productVersion+", productType="+type+
        				", checksum="+cs);
        	
        	Session s = getSession();
            Query q = s.createQuery(
            		"from WorkProduct as wp where "+
            		" wp.productID = :productID and wp.productVersion = :productVersion"+
            		" and wp.productType= :productType and wp.checksum= :checksum"
            				);
            q.setParameter("productID", productID);
            q.setParameter("productVersion", productVersion);
            q.setParameter("productType", type);
            q.setParameter("checksum", cs);
            List<WorkProduct> list = q.list();

            if (log.isInfoEnabled())
        		log.info( ((list!=null)? list.size(): 0) +" record found");
            
            if ((list != null && list.size() != 0) )
            {
            	wp = list.get(0);
            	if (log.isDebugEnabled())
            		log.debug("findByWorkProductIdentification. wp="+wp);
            }
        }
        return wp;
    }

	public Document findDocsBySearchCriteria(String searchSQL)
			throws ParserConfigurationException, SQLException {

    	if (log.isInfoEnabled())
    		log.info("findDocsBySearchCriteria. searchSQL="+searchSQL);
		Document doc = doFindDom(searchSQL);
		return doc;
	}

	@SuppressWarnings("unused")
	private Document doFindDom(String searchSQL)
			throws ParserConfigurationException, SQLException {
		log.error("doFindDom - To be implemented");
/*
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		Element results = doc.createElement(tablerecords);
		doc.appendChild(results);

		ResultSet rs = getSession().createSQLQuery(searchSQL).list();
		ResultSetMetaData rsmd = rs.getMetaData();

		int colCount = rsmd.getColumnCount();

		while (rs.next()) {

			Node wpNode = WorkProductHelper.toWorkProductSummary(
					createWorkProduct(rs)).getDomNode();
			Node importedWPNode = doc.importNode(wpNode, true);
			results.appendChild(importedWPNode);

		}

		if (doc != null) {
			return doc;
		} else {
			return null;
		}
		*/
		return null;
	}

    public WorkProduct findWPBySerachCriteria(String searchSQL) {

    	if (log.isInfoEnabled())
    		log.info("findWPBySearchCriteria. searchSQL="+searchSQL);
        WorkProduct wp = doWPWork(searchSQL);
        
        return wp;
    }

    private WorkProduct doWPWork(String SQL) {

    		log.error("doWPWork - to be implemented");
    	/*
        try {
            if (stmt != null) {
                ResultSet rs = stmt.executeQuery(SQL);
                if (rs.getRow() == 0)
                	log.debug("No record is found.");
                
                while (rs.next()) {
                    WorkProduct obj = createWorkProduct(rs);
                    // only one object
                    return obj;
                }

                rs.close();
                // stmt.close();
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
        	log.error("doWPWork - error:"+e.getMessage());
            e.printStackTrace();
        }

        log.debug("doWPWork - end");
        */
        return null;
    }


}
