package com.saic.uicds.core.em.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;

import x1.oasisNamesTcEmergencyCap1.AlertDocument;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;

import com.saic.precis.x2009.x06.base.NamespaceMapItemType;
import com.saic.precis.x2009.x06.base.NamespaceMapType;
import com.saic.uicds.core.em.service.AlertService;
import com.saic.uicds.core.em.util.EMDigestHelper;
import com.saic.uicds.core.infrastructure.exceptions.InvalidXpathException;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

/**
 * The AlertService implementation.
 * 
 * @since 1.0
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class AlertServiceImpl
    implements AlertService, ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    private DirectoryService directoryService;

    private WorkProductService workProductService;

    /**
     * Cancel alert deletes the work product identified by the workproduct id string
     * 
     * @param workProductId the work product id
     * @return the product publication status
     * @ssdd
     */
    public ProductPublicationStatus cancelAlert(String workProductId) {

        log.info("work product id to cancel: " + workProductId);

        WorkProduct wp = getWorkProductService().getProduct(workProductId);

        ProductPublicationStatus status;

        if (wp == null) {
            status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(workProductId + " doesn't existed");
            return status;
        }

        // if it's still not closed, we need to close it first
        if (wp.isActive() == true) {
            status = getWorkProductService().closeProduct(
                WorkProductHelper.getWorkProductIdentification(wp));
            if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
                return status;
        }

        return getWorkProductService().archiveProduct(
            WorkProductHelper.getWorkProductIdentification(wp));
    }

    /**
     * Creates a workproduct of alert type. Adds the supplied incident id to the set of associated
     * interest groups
     * 
     * @param incidentId the incident id
     * @param alert the alert
     * @return the product publication status
     * @ssdd
     */
    public ProductPublicationStatus createAlert(String incidentId, Alert alert) {

        // log.info("Received New Alert Message:\n" + alert.toString());
        // Check for msgType of incoming Alert
        // If it is Cancel then look in the References data to find one or more
        // WorkProductIdentifers
        // In references - one or more Cap Alerts are identified with the attributes
        // sender,identifier,sent for each Cap Alert separated by spaces
        //
        AlertDocument alertDoc = AlertDocument.Factory.newInstance();
        alertDoc.setAlert(alert);
        String msgType = alertDoc.getAlert().getMsgType().toString();

        if (msgType.equalsIgnoreCase("Cancel")) {
            ProductPublicationStatus status = new ProductPublicationStatus();

            log.info("New Alert is of type Cancel, ");

            // log.info("References Data: " + alertDoc.getAlert().getReferences());

            // get info from references and parse for Alert ID's
            String references = alertDoc.getAlert().getReferences();
            String prefix = "";
            String[] alerts = references.substring(prefix.length()).split(" ");
            String[] alertIDs = new String[alerts.length];

            for (int i = 0; i < alerts.length; i++) {
                log.info("Alert Record: " + alerts[i]);
                String[] eachAlert = alerts[i].split(",");
                for (int j = 0; j < eachAlert.length; j++) {
                    if (j == 1) { // second item
                        log.info("Alert ID: " + eachAlert[j]);
                        alertIDs[i] = eachAlert[j];
                    }
                }

            }

            // use the alert ids to cancel each alert
            log.info("looping to cancel each alert id");
            for (String z : alertIDs) {
                String wpIdentifier = z;
                WorkProduct wp = getAlertByAlertId(wpIdentifier);
                if (wp != null) {
                    log.info("calling cancel alert for: ");
                    log.info("wp ID: " + wp.getProductID());
                    status = cancelAlert(wp.getProductID());
                    log.info("Status: " + status.getStatus());

                } else {
                    log.info("Alert: " + wpIdentifier + " not found on core to delete");
                }
            }
            return status;
        }

        WorkProduct wp = new WorkProduct();
        wp.setProductType(AlertService.Type);
        wp.setProduct(alertDoc);

        if (incidentId != null) {
            wp.getAssociatedInterestGroupIDs().add(incidentId);
        }

        // get digest byte array
        wp.setDigest(new EMDigestHelper(alert).getDigest());

        ProductPublicationStatus status = workProductService.publishProduct(wp);

        return status;

    }

    private WorkProduct findAlertWP(String alertID) {

        List<WorkProduct> productList = getWorkProductService().listByProductType(AlertService.Type);
        for (WorkProduct product : productList) {
            try {
                AlertDocument alertDocument = (AlertDocument) product.getProduct();

                if (alertDocument.getAlert().getIdentifier().equals(alertID)) {
                    return product;
                }
            } catch (Exception e) {
                log.error("Not Valid Alert Document:\n" + product.getProduct().xmlText());
            }

        }
        return null;
    }

    /**
     * Gets the alert using the workproduct id string
     * 
     * @param wpID the wp id
     * @return the alert
     * @ssdd
     */
    public WorkProduct getAlert(String wpID) {

        return getWorkProductService().getProduct(wpID);
    }

    /**
     * Gets the alert by alert id.
     * 
     * @param alertId the alert id
     * @return the alert by alert id
     * @ssdd
     */
    @Override
    public WorkProduct getAlertByAlertId(String alertId) {

        return findAlertWP(alertId);
    }

    public DirectoryService getDirectoryService() {

        return this.directoryService;
    }

    /**
     * Gets the list of alerts.
     * 
     * @param queryType the query type
     * @param namespaceMap the namespace map
     * @return the list of alerts
     * @ssdd
     */
    public WorkProduct[] getListOfAlerts(String queryType, NamespaceMapType namespaceMap)
        throws InvalidXpathException {

        Map<String, String> mapNamespaces = new HashMap<String, String>();
        if (namespaceMap != null) {
            for (NamespaceMapItemType ns : namespaceMap.getItemArray()) {
                mapNamespaces.put(ns.getPrefix(), ns.getURI());
            }

        }

        /*
         * Get a list of WP by Type from Work Product and use that list to match the alertID
         */
        List<WorkProduct> listOfProducts = getWorkProductService().getProductByTypeAndXQuery(
            AlertService.Type, queryType, mapNamespaces);
        if (listOfProducts != null && listOfProducts.size() > 0) {
            WorkProduct[] products = new WorkProduct[listOfProducts.size()];
            return listOfProducts.toArray(products);
        } else {
            return null;
        }
    }

    public WorkProductService getWorkProductService() {

        return this.workProductService;
    }

    public void setDirectoryService(DirectoryService service) {

        this.directoryService = service;
    }

    public void setWorkProductService(WorkProductService service) {

        this.workProductService = service;
    }

    /**
     * System initialized handler.
     * 
     * @param message the message
     */
    public void systemInitializedHandler(String message) {

        WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(AlertService.Type);
        directoryService.registerUICDSService(NS_AgreementService, ALERT_SERVICE_NAME, typeList,
            typeList);
    }
}
