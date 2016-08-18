package com.saic.uicds.core.infrastructure.dao;

import java.util.Date;
import java.util.List;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.uicds.core.dao.GenericDAO;
import com.saic.uicds.core.infrastructure.model.WorkProduct;

public interface WorkProductDAO extends GenericDAO<WorkProduct, Integer> {

    public List<WorkProduct> findAllClosedVersionOfProduct(String productID);

    //public List<WorkProduct> findAllOrderedBy(String orderBy);

    public List<WorkProduct> findAllVersionOfProduct(String productID);

    //public List<WorkProduct> findByCreatedBy(String createdBy);

    //public List<WorkProduct> findByCreatedDate(Date beginDate, Date endDate);

    public List<WorkProduct> findByInterestGroup(String interestGroupID);

    public List<WorkProduct> findByInterestGroupAndType(String interestGroupID, String type);

    public WorkProduct findByProductID(String productID);

    //public WorkProduct findByProductIDAndChecksum(String productID, String checksum);

    //public WorkProduct findByProductIDAndType(String productID, String productType);

    public WorkProduct findByProductIDAndVersion(String productID, Integer productVersion);

    public List<WorkProduct> findByProductType(String productType);

    //public List<WorkProduct> findBySize(Integer lowerBound, Integer upperBound);

    //public List<WorkProduct> findByTypeAndSize(String productType, Integer lowerBound, Integer upperBound);

    //public List<WorkProduct> findByUpdatedBy(String updatedBy);

    //public List<WorkProduct> findByUpdatedDate(Date beginDate, Date endDate);

    public WorkProduct findByWorkProductIdentification(IdentificationType pkgId);

    //public void markDeleted(String productID);
   
	  
}
