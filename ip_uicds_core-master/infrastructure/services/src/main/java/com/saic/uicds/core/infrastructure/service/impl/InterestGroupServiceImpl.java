package com.saic.uicds.core.infrastructure.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;

import org.uicds.interestGroupService.InterestGroupListInfoType;
import org.uicds.interestGroupService.InterestGroupListType;

import org.uicds.interestGroupService.ShareInterestGroupRequestDocument.ShareInterestGroupRequest;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import com.saic.precis.x2009.x06.base.CodespaceValueType;
import com.saic.precis.x2009.x06.base.IdentificationListType;
import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.structures.InterestGroupSubtypeType;
import com.saic.precis.x2009.x06.structures.InterestGroupType;

import com.saic.uicds.core.infrastructure.dao.InterestGroupDAO;
import com.saic.uicds.core.infrastructure.exceptions.InvalidInterestGroupIDException;
import com.saic.uicds.core.infrastructure.model.CodeSpaceValueType;
import com.saic.uicds.core.infrastructure.model.InterestGroup;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.InterestGroupManagementComponent;
import com.saic.uicds.core.infrastructure.service.InterestGroupService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;


public class InterestGroupServiceImpl implements InterestGroupService {
	
    private static final InterestGroupSubtypeType InterestGroupSubtype = null;

	Logger log = LoggerFactory.getLogger(InterestGroupServiceImpl.class);
    
    private InterestGroupManagementComponent interestGroupManagementComponent;
    
    private ConfigurationService configurationService;
    
    private DirectoryService directoryService;
    
    public WorkProductService getWorkProductService() {
		return workProductService;
	}

	public void setWorkProductService(WorkProductService workProductService) {
		this.workProductService = workProductService;
	}

	public void setInterestGroupManagementComponent(
			InterestGroupManagementComponent interestGroupManagementComponent) {
		this.interestGroupManagementComponent = interestGroupManagementComponent;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}

	public void setDirectoryService(DirectoryService directoryService) {
		this.directoryService = directoryService;
	}

	public void setInterestGroupDAO(InterestGroupDAO interestGroupDAO) {
		this.interestGroupDAO = interestGroupDAO;
	}

	private InterestGroupDAO interestGroupDAO;
    
    private WorkProductService workProductService;
    
	@Override
	public String createInterestGroup(InterestGroupType interestGroup, IdentificationListType wpList) {
		// get the owning core first
        String owningCore = getConfigurationService().getCoreName();
        // create interest group 
        InterestGroupInfo igInfo = new InterestGroupInfo();
        igInfo.setInterestGroupID(null);
        igInfo.setInterestGroupType(interestGroup.getInterestGroupType());
        igInfo.setName(interestGroup.getName());
        igInfo.setDescription(interestGroup.getDescription());
        igInfo.setOwningCore(owningCore);
        igInfo.setInterestGroupSubType(interestGroup.getInterestGroupSubTypeArray()[0].getLabel().toString()+"#"+interestGroup.getInterestGroupSubTypeArray()[0].getCodespace().toString());
        String interestGroupID = interestGroupManagementComponent.createInterestGroup(igInfo);
        //if the specialized work product is not null, associate it with the interest group
        if (interestGroup.getSpecializedWorkProduct() != null) {
        	log.debug("associating specialized wp to interest group");
        	try {
        	String swp = workProductService.associateWorkProductToInterestGroup(interestGroup.getSpecializedWorkProduct().toString(), interestGroupID);
        	    }catch (Exception e){
        		e.printStackTrace();
        	  }
        	}
        
        //if the work product list is not null, then associate the work products with the interest group
        if (wpList != null && wpList.sizeOfIdentificationArray()>0) {
            for (IdentificationType identification: wpList.getIdentificationArray()){
        	   String result = workProductService.associateWorkProductToInterestGroup (wpList.getIdentificationArray().toString(), interestGroupID);
        	  
        	}
        
        }
		return interestGroupID;
	}

	@Override
	public ProductPublicationStatus archiveInterestGroup(String interestGroupId) {
	       ProductPublicationStatus status = validateInterestGroup(interestGroupId);
	        if (status != null) {
	            return status;
	        }
	        
	        status = new ProductPublicationStatus();
	        
	        log.debug("Archiving interest group, finding the work products, mark them to be deleted");
	        WorkProduct[] products = getWorkProductService().getAssociatedWorkProductList(interestGroupId);
	        for (WorkProduct product : products) {
	            if (product.isActive() == true) {
	                return new ProductPublicationStatus(interestGroupId + " contains "
	                    + product.getProductID() + " which needs to be closed first");
	            }
	            log.debug("delete " + product.getProductID() + ", Ver.: " + product.getProductVersion());
	            getWorkProductService().deleteWorkProduct(product.getProductID());
	        }
	        
	        // delete the interest group now.
	        try {
	            log.debug("ask InterestGroupManagementComponent to delete InterestGroup: " + interestGroupId);
	            getInterestGroupManagementComponent().deleteInterestGroup(interestGroupId);
	        } catch (InvalidInterestGroupIDException e) {
	            e.printStackTrace();
	            return new ProductPublicationStatus("delete interest group: " + interestGroupId + ": "
	                + e.getMessage());
	        }


	        status.setStatus(ProductPublicationStatus.SuccessStatus);
		return status;
	}

	@Override
	public ProductPublicationStatus closeInterestGroup(String interestGroupId) {
        ProductPublicationStatus status = validateInterestGroup(interestGroupId);
        if (status != null) {
            return status;
        }

        status = new ProductPublicationStatus();
        // find the work products, mark them to be deleted
        WorkProduct[] products = getWorkProductService().getAssociatedWorkProductList(interestGroupId);
        for (WorkProduct product : products) {
            if (product != null && product.isActive()) {
                log.debug("mark " + product.getProductID() + ", Ver.: "
                    + product.getProductVersion() + " as Deleted/InActive");
                getWorkProductService().closeProduct(
                    WorkProductHelper.getWorkProductIdentification(product));
            }
        }      
        
        // find the interest group model and mark it to be deleted
        log.debug("mark " + interestGroupId + " as Deleted/InActive");
        interestGroupDAO.delete(interestGroupId, false);  
        
        status.setStatus(ProductPublicationStatus.SuccessStatus);		
		return status;
	}

	@Override
	public InterestGroupListType getInterestGroupList() {
		InterestGroupListType response = InterestGroupListType.Factory.newInstance();
		
        ArrayList<InterestGroupListInfoType> igList = new ArrayList<InterestGroupListInfoType>();

        List<InterestGroupInfo> igInfo = interestGroupManagementComponent.getInterestGroupList();
        if (igInfo != null && igInfo.size() > 0) {
             for (InterestGroupInfo ig : igInfo) {
                InterestGroupListInfoType interestGroup = toInterestGroupType(ig);
                if (interestGroup!= null) {
                    igList.add(interestGroup);
                }
             }
        }

        if (igList.size() > 0) {
        	InterestGroupListInfoType[] intgrp = new InterestGroupListInfoType[igList.size()];
        	intgrp = igList.toArray(intgrp);
        	response.setInterestGroupListArray(intgrp);
        }
  
              
        return response;
    }
		

	@Override
	public boolean shareInterestGroup(ShareInterestGroupRequest shareRequest) {
		boolean status = true;
        try {
		interestGroupManagementComponent.shareInterestGroup(shareRequest.getInterestGroupID(), shareRequest.getCoreName(),shareRequest.getDetailedInfo(), false);
        }catch (Exception e) {
            log.error("Unable to share interest group");
            e.printStackTrace();
            status=false;
        };
        
		return status;
	}

	@Override
	public boolean unShareInterestGroup() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateInterestGroup(InterestGroupType request,
			String interestGroupId) {
		boolean status = true;
		// get the owning core first
        String owningCore = getConfigurationService().getCoreName();
        // create interest group 
        InterestGroupInfo igInfo = new InterestGroupInfo();
        igInfo.setInterestGroupID(interestGroupId);
        igInfo.setInterestGroupType(request.getInterestGroupType());
        igInfo.setName(request.getName());
        igInfo.setDescription(request.getDescription());
        igInfo.setOwningCore(owningCore);
        igInfo.setInterestGroupSubType(request.getInterestGroupSubTypeArray()[0].toString()+"#"+request.getInterestGroupSubTypeArray()[0].getLabel().toString()+"#"
                +request.getInterestGroupSubTypeArray()[0].getCodespace().toString());
        try {
			interestGroupManagementComponent.updateInterestGroup(igInfo);
		} catch (InvalidInterestGroupIDException e) {			
			e.printStackTrace();
			status = false;
		}
		return status;
	}

	@Override
	public WorkProductList getListOfWorkProduct(String interestGroupId) {
		WorkProduct[]products = workProductService.getAssociatedWorkProductList(interestGroupId);
        WorkProductList workProductList= null;
        if (products != null && products.length > 0) {
            workProductList = WorkProductList.Factory.newInstance();
            for (WorkProduct product:products){
            	workProductList.addNewWorkProduct().set(WorkProductHelper.toWorkProductSummary(product));
            }
        } 
        return workProductList;
   
		
	}
	public void systemInitializedHandler(String messgae) {
        String urn = getConfigurationService().getServiceNameURN(INTEREST_GROUP_SERVICE_NAME);
        WorkProductTypeListType publishedProducts = WorkProductTypeListType.Factory.newInstance();
        WorkProductTypeListType subscribedProducts = WorkProductTypeListType.Factory.newInstance();
        directoryService.registerUICDSService(urn, INTEREST_GROUP_SERVICE_NAME, publishedProducts,
        subscribedProducts);
	}
	
	 public InterestGroupManagementComponent getInterestGroupManagementComponent() {

	        return interestGroupManagementComponent;
	    }

	   /** {@inheritDoc} */
	    public ConfigurationService getConfigurationService() {

	        return this.configurationService;
	    }

	    /** {@inheritDoc} */
	    public DirectoryService getDirectoryService() {

	        return this.directoryService;
	    }
   
 
	    public InterestGroupDAO getInterestGroupDAO(){
	    	return interestGroupDAO;
	    }
    


    private ProductPublicationStatus validateInterestGroup(String interestGroupID) {
        log.debug("Validating Interest Group...");
        if (interestGroupID == null) {

            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Empty Interest Group ID");
            return status;
        }
        if (getInterestGroupDAO().findByInterestGroup(interestGroupID) == null) {
            return new ProductPublicationStatus("Interest Group: " + interestGroupID + " does not exist");
        }
        // if the core is not the owning core then return failure
        if (interestGroupManagementComponent.interestGroupOwnedByCore(interestGroupID) == false) {

            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(getDirectoryService().getCoreName()
                + " doesn't own interest group: " + interestGroupID);
            return status;
        }
        return null;
    }
    
    private CodespaceValueType parseInterestGroupSubType(String igSubtype) {
    	
    	CodespaceValueType result = CodespaceValueType.Factory.newInstance();  	
   	
    	String[] tokens = igSubtype.split("#");
    	result.setLabel(tokens[0]);
    	result.setCodespace(tokens[1]);
   	
		return result;
    	
    }
    
    private InterestGroupListInfoType toInterestGroupType(InterestGroupInfo ig) {

    	InterestGroupListInfoType interestGroupInfo = null;
        if (ig != null) {
        	CodespaceValueType igSubtype = CodespaceValueType.Factory.newInstance();
        	    	
        	InterestGroupType interestGroup = InterestGroupType.Factory.newInstance();
        	interestGroupInfo = InterestGroupListInfoType.Factory.newInstance();
            
        	igSubtype = parseInterestGroupSubType(ig.getInterestGroupSubType());
            interestGroup.setInterestGroupType(ig.getInterestGroupType());
            
            interestGroup.setInterestGroupSubTypeArray(0, igSubtype);
 			
            interestGroup.setName(ig.getName());
            interestGroup.setDescription(ig.getDescription());
            
            interestGroupInfo.setId(ig.getInterestGroupID());
            
        }
        return interestGroupInfo;
    }



	@Override
	public WorkProduct getInterestGroup(String specializedWPId) {
		
		return specializedWPId != null ? getWorkProductService().getProduct(specializedWPId) : null;
	}
}