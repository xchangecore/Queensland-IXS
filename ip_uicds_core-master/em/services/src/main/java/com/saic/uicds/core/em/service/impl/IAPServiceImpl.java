package com.saic.uicds.core.em.service.impl;

import gov.ucore.ucore.x20.DigestType;
import gov.ucore.ucore.x20.EntityType;
import gov.ucore.ucore.x20.SimplePropertyType;
import gov.ucore.ucore.x20.ThingType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.iapService.IAPComponentType;
import org.uicds.iapService.IncidentActionPlanDocument;
import org.uicds.iapService.IncidentActionPlanType;
import org.uicds.icsForm.ICSFormDocument;
import org.uicds.icsFormCommon.ICSFormDocumentType;
import org.uicds.uicdsCommon.DocumentType;
import org.uicds.uicdsCommon.IAPDocumentDocument1;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.uicds.core.em.service.IAPService;
import com.saic.uicds.core.em.util.EMDigestHelper;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.uicds.core.infrastructure.util.DigestHelper;
import com.saic.uicds.core.infrastructure.util.InfrastructureNamespaces;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.UUIDUtil;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

/**
 * The IAPService implementation.
 * 
 * @author wuerfelr
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class IAPServiceImpl
    implements IAPService, ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(IAPServiceImpl.class);

    private WorkProductService workProductService;

    private DirectoryService directoryService;

    private static final boolean APPROVED = true;
    private static final boolean DRAFT = false;

    public void systemInitializedHandler(String messgae) {

        WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(IAP_WORKPRODUCT_TYPE);
        typeList.addProductType(ICSFORM_WORKPRODUCT_TYPE);
        directoryService.registerUICDSService(NS_IAPService, IAP_SERVICE_NAME, typeList, typeList);
    }

    /**
     * Creates and publishes a workProduct of IAP service type.
     * 
     * @param plan the plan
     * 
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createIAP(IncidentActionPlanType plan) {

        WorkProduct wp = createIAPWorkProduct(plan, DRAFT);

        ProductPublicationStatus status = workProductService.publishProduct(wp);
        if (status.getStatus().equals(ProductPublicationStatus.FailureStatus)) {
            log.error("createIAP: Error publishing new IAP");
        }

        return status;
    }

    private WorkProduct createIAPWorkProduct(IncidentActionPlanType plan, boolean approved) {

        String planWPID = UUIDUtil.getID(IAPService.IAP_WORKPRODUCT_TYPE);
        plan.setId(planWPID);
        // Create WorkProduct
        IncidentActionPlanDocument iap = IncidentActionPlanDocument.Factory.newInstance();
        iap.setIncidentActionPlan(plan);
        WorkProduct wp = new WorkProduct();
        wp.setProductType(IAPService.IAP_WORKPRODUCT_TYPE);
        wp.setProduct(iap);
        wp.setProductID(planWPID);
        wp.setDigest(new EMDigestHelper(iap.getIncidentActionPlan(), planWPID, approved).getDigest());

        // Add interest group associated if requested.
        if (plan.getIncidentID() != null) {
            wp.getAssociatedInterestGroupIDs().add(plan.getIncidentID());
        }
        return wp;
    }

    /**
     * Creates and publishes a workProduct of IAP service form type
     * 
     * @param form the form
     * 
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createICSForm(ICSFormDocumentType form) {

        String formWPID = UUIDUtil.getID(ICSFORM_WORKPRODUCT_TYPE);
        form.setId(formWPID);
        ICSFormDocument icsForm = ICSFormDocument.Factory.newInstance();
        icsForm.addNewICSForm().set(form);
        WorkProduct wp = new WorkProduct();
        wp.setProductType(IAPService.ICSFORM_WORKPRODUCT_TYPE);
        wp.setProduct(form);
        wp.setProductID(formWPID);

        // Add interest group associated if requested.
        if (form.getIncidentID() != null) {
            wp.getAssociatedInterestGroupIDs().add(form.getIncidentID());
        }

        ProductPublicationStatus status = workProductService.publishProduct(wp);
        if (status.getStatus().equals(ProductPublicationStatus.FailureStatus)) {
            log.error("Error publishing ICS Form");
        }

        return status;
    }

    /**
     * Gets the IAP using the supplied workProduct Id string
     * 
     * @param workProductID the work product id
     * 
     * @return the iAP
     * @ssdd
     */
    @Override
    public IncidentActionPlanDocument getIAP(String workProductID) {

        WorkProduct wp = workProductService.getProduct(workProductID);
        try {
            return (IncidentActionPlanDocument) wp.getProduct();
        } catch (Exception e) {
            log.error("Error parsing IAP work product: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the ICS form using the supplied workProduct Id string
     * 
     * @param workProductID the work product id
     * 
     * @return the iCS form
     * @ssdd
     */
    @Override
    public ICSFormDocument getICSForm(String workProductID) {

        WorkProduct wp = workProductService.getProduct(workProductID);
        ICSFormDocument form = null;
        try {
            form = (ICSFormDocument) wp.getProduct();
        } catch (Exception e) {
            log.error("Error parsing IAP work product: " + e.getMessage());
        }
        return form;
    }

    /**
     * Publishes an updated IAP workProduct if the product already exists or publishes a new IAP
     * workProduct if it does not.
     * 
     * @param plan the plan
     * @param workProductIdentification the work product identification
     * 
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateIAP(IncidentActionPlanType plan,
        IdentificationType workProductIdentification, boolean activate) {

        WorkProduct wp = workProductService.getProduct(workProductIdentification);

        return updateIAPWorkProduct(plan, workProductIdentification, activate, wp);
    }

    private ProductPublicationStatus updateIAPWorkProduct(IncidentActionPlanType plan,
        IdentificationType workProductIdentification, boolean activate, WorkProduct wp) {

        // if updating to activate the IAP then use the current IAP payload
        if (plan == null) {
            plan = getIAPFromWorkProduct(wp).getIncidentActionPlan();
        }
        if (wp != null) {
            IncidentActionPlanDocument iap = IncidentActionPlanDocument.Factory.newInstance();
            iap.addNewIncidentActionPlan().set(plan);
            WorkProduct newWP = new WorkProduct(wp);
            newWP.setProduct(iap);
            newWP.setDigest(new EMDigestHelper(iap.getIncidentActionPlan(),
                workProductIdentification.getIdentifier().getStringValue(), activate).getDigest());
            ProductPublicationStatus status = workProductService.publishProduct(newWP);
            return status;
        } else {
            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Specified Incident Action Plan does not exist");
            return status;
        }
    }

    /**
     * Publishes an updated ICS form workProduct if the product already exists or publishes a new
     * ICS form workProduct if it does not.
     * 
     * @param form the form
     * @param workProductIdentification the work product identification
     * 
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateICSForm(ICSFormDocumentType form,
        IdentificationType workProductIdentification) {

        WorkProduct wp = workProductService.getProduct(workProductIdentification);
        if (wp != null) {
            ICSFormDocument ics = ICSFormDocument.Factory.newInstance();
            ics.addNewICSForm().set(form);
            WorkProduct newWP = new WorkProduct(wp);
            newWP.setProduct(ics);
            ProductPublicationStatus status = workProductService.publishProduct(wp);
            return status;
        } else {
            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Specified ICS Form does not exist");
            return status;
        }
    }

    /**
     * Update an IAP workProduct given the workProduct identification.
     * 
     * @param document the document
     * @param workProductIdentification the work product identification
     * 
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateDocument(DocumentType document,
        IdentificationType workProductIdentification) {

        WorkProduct wp = workProductService.getProduct(document.getId());
        if (wp != null) {
            IAPDocumentDocument1 iapDoc = IAPDocumentDocument1.Factory.newInstance();
            iapDoc.addNewIAPDocument().set(document);
            WorkProduct newWP = new WorkProduct(wp);
            newWP.setProduct(iapDoc);
            newWP = WorkProductHelper.setWorkProductIdentification(newWP, workProductIdentification);
            ProductPublicationStatus status = workProductService.publishProduct(wp);
            return status;
        } else {
            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Specified Document does not exist");
            return status;
        }
    }

    /**
     * This takes an IAP from the list of IAP's for this incident and turns it into the 'active'
     * IAP.
     * 
     * @param workProductID the work product id
     */
    @Override
    public ProductPublicationStatus setApprovedIAP(IdentificationType workProductID,
        String incidentID) {

        ProductPublicationStatus status = new ProductPublicationStatus();
        status.setStatus(ProductPublicationStatus.FailureStatus);

        // get the requested IAP work product
        WorkProduct requestedIAP = workProductService.getProduct(workProductID);
        if (requestedIAP == null) {
            status.setReasonForFailure("Work Product does not exist");
            return status;
        }

        // Get the IAP that is requested to be approved
        IncidentActionPlanDocument iap = getIAPFromWorkProduct(requestedIAP);

        // see if this incident already has an active IAP
        WorkProduct approvedIAP = findApprovedIAP(incidentID);

        // Create a new IAP work product that is marked as approved
        if (approvedIAP == null) {
            if (incidentID != null && !incidentID.isEmpty()) {
                if (iap.getIncidentActionPlan().getIncidentID() == null
                    || iap.getIncidentActionPlan().getIncidentID().isEmpty()) {
                    iap.getIncidentActionPlan().setIncidentID(incidentID);
                }
            }

            WorkProduct wp = createIAPWorkProduct(iap.getIncidentActionPlan(), APPROVED);

            status = workProductService.publishProduct(wp);
            if (status.getStatus().equals(ProductPublicationStatus.FailureStatus)) {
                log.error("setApprovedIAP: Error publishing the new approved IAP");
            }
        }
        // Update the current approved work product
        else {
            IdentificationType workProductIdentification = WorkProductHelper.getWorkProductIdentification(approvedIAP);
            status = updateIAPWorkProduct(iap.getIncidentActionPlan(), workProductIdentification,
                true, approvedIAP);
        }

        return status;
    }

    private WorkProduct findApprovedIAP(String incidentID) {

        List<WorkProduct> workProducts = workProductService.findByInterestGroupAndType(incidentID,
            IAP_WORKPRODUCT_TYPE);

        List<WorkProduct> approvedIAPs = new ArrayList<WorkProduct>();

        for (WorkProduct product : workProducts) {
            if (product.getAssociatedInterestGroupIDs().size() > 0) {
                Iterator<String> it = product.getAssociatedInterestGroupIDs().iterator();
                if (!it.next().equals(incidentID)) {
                    continue;
                }
            }

            if (product.getDigest() != null && product.getDigest().getDigest() != null) {
                DigestType digest = product.getDigest().getDigest();
                if (digest.sizeOfThingAbstractArray() > 0) {
                    for (ThingType thing : digest.getThingAbstractArray()) {
                        if (thing instanceof EntityType) {
                            SimplePropertyType status = DigestHelper.getSimplePropertyFromThing(
                                thing, InfrastructureNamespaces.UICDS_EVENT_STATUS_CODESPACE, null,
                                "Status", null);
                            if (status.getCode().equals("Approved")) {
                                approvedIAPs.add(product);
                            }
                        }
                    }
                }
            }
        }

        if (approvedIAPs.size() > 1) {
            log.warn("Found more than one Approved IAP work product for incident: " + incidentID);
            return approvedIAPs.get(0);
        } else if (approvedIAPs.size() == 1) {
            return approvedIAPs.get(0);
        }
        return null;
    }

    @Override
    public WorkProduct getApprovedIAP(String incidentID) {

        return findApprovedIAP(incidentID);

    }

    /**
     * Gets the list of ICS form workProducts for the given incidentId
     * 
     * @param incidentID the incident id
     * 
     * @return the iCS form list
     * @ssdd
     */
    @Override
    public WorkProduct[] getICSFormList(String incidentID) {

        // GetByType and IncidentID
        List<WorkProduct> wpList = workProductService.findByInterestGroupAndType(incidentID,
            IAPService.ICSFORM_WORKPRODUCT_TYPE);
        if (wpList == null || wpList.size() <= 0)
            return null;
        WorkProduct[] products = wpList.toArray(new WorkProduct[wpList.size()]);

        return products;
    }

    /**
     * Attach ics forms to iap. If any supplied component Ids are already attached to the IAP (as
     * identifed by Id string, they are removed and added back with the new componentId
     * identification types and the IAP workProduct is re-published
     * 
     * @param componentIds the component ids
     * @param workProductID the work product id
     * 
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus attachWorkProductToIAP(IdentificationType[] componentIds,
        String workProductID) {

        // Check if IAP exists
        WorkProduct workProduct = workProductService.getProduct(workProductID);
        // IncidentActionPlanDocument planDoc = getIAP(workProductID);

        ProductPublicationStatus status = null;

        // Check if IAP exists
        if (workProduct != null) {

            // Get the IAP
            IncidentActionPlanDocument iapDoc = getIAPFromWorkProduct(workProduct);

            // List<IdentificationType> componentsToAdd =
            // Arrays.asList(componentIds);

            ArrayList<IdentificationType> componentsToAdd = new ArrayList<IdentificationType>(
                Arrays.asList(componentIds));

            if (componentsToAdd.size() == 0) {
                return status;
            }

            // If alreay attached then remove it from the list of components to add
            if (iapDoc.getIncidentActionPlan().getComponents() != null
                && iapDoc.getIncidentActionPlan().getComponents().sizeOfComponentArray() > 0) {
                for (IAPComponentType comp : iapDoc.getIncidentActionPlan().getComponents().getComponentArray()) {
                    IdentificationType id = comp.getComponentIdentifier().getWorkProductIdentification();
                    for (IdentificationType cid : componentIds) {
                        if (id.getIdentifier().getStringValue().equalsIgnoreCase(
                            cid.getIdentifier().getStringValue())) {
                            componentsToAdd.remove(cid);
                        }
                    }
                }
            }

            // Add any that are not already in the IAP
            for (IdentificationType comp : componentsToAdd) {
                iapDoc.getIncidentActionPlan().addNewComponents().addNewComponent().addNewComponentIdentifier().setWorkProductIdentification(
                    comp);
            }

            // Update the work product if there were any new components added
            if (componentsToAdd.size() > 0) {
                status = workProductService.publishProduct(newWorkProductVersion(workProduct,
                    iapDoc));
            }
            // If it was already in the work product return a success without publishing a new
            // version
            else {
                status = new ProductPublicationStatus();
                status.setStatus(ProductPublicationStatus.SuccessStatus);
                status.setProduct(workProduct);
            }
        }

        return status;
    }

    private WorkProduct newWorkProductVersion(WorkProduct oldWorkProduct,
        IncidentActionPlanDocument iapDoc) {

        WorkProduct newWorkProduct = new WorkProduct(oldWorkProduct);
        newWorkProduct.setProduct(iapDoc);
        return newWorkProduct;
    }

    private IncidentActionPlanDocument getIAPFromWorkProduct(WorkProduct workProduct) {

        IncidentActionPlanDocument plan = null;
        try {
            plan = (IncidentActionPlanDocument) workProduct.getProduct();
        } catch (Exception e) {
            log.error("Error parsing IAP work product: " + e.getMessage());
        }
        return plan;
    }

    /** {@inheritDoc} */
    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    /**
     * 
     * @param workProductService
     */
    public void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }
}
