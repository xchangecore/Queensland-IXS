package com.saic.uicds.core.em.processes.ipaws;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import x1.oasisNamesTcEmergencyCap1.AlertDocument;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.NamespaceMapItemType;
import com.saic.precis.x2009.x06.base.NamespaceMapType;
import com.saic.uicds.core.em.service.AlertService;
import com.saic.uicds.core.infrastructure.exceptions.InvalidXpathException;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;

/**
 * class IPAWSAlertWorkProduct accesses uicds services for IPAWS
 */
public class IPAWSAlertWorkProduct {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    AlertService alertService;

    @Autowired
    WorkProductService productService;

    private Hashtable<String, WorkProduct> alertWpTable = new Hashtable<String, WorkProduct>();

    /**
     * method findAlertInUICDS checks whether the alert is already in UICDS
     * 
     * @param String alertId the alert id to check
     * @return boolean true if it already is in UICDS, false if not
     */
    public boolean findAlertInUICDS(String alertId) {

        return alertWpTable.containsKey(alertId);
    }

    /* *
     * method getAlertFromWP
     * return the alert from the alert wp in the alertWpTable
     * @param String alertId
     * @return Alert the alert in UICDS with id alertId
     */
    public Alert getAlertFromWP(String alertId) {

        WorkProduct product = this.alertWpTable.get(alertId);
        if (product != null) {
            AlertDocument alertDoc = (AlertDocument) product.getProduct();
            return alertDoc.getAlert();
        }

        return null;
    }

    /**
     * method getAlertWorkProduct retrieve all alert work products in UICDS
     * 
     * @return WorkProduct[] array of alert workproducts in UICDS
     */
    public Set<String> getAlertWorkProduct() {

        NamespaceMapType map = NamespaceMapType.Factory.newInstance();
        NamespaceMapItemType mapItem = map.addNewItem();

        try {
            // clear the table
            this.alertWpTable.clear();

            // get the alert wp
            WorkProduct[] products = this.alertService.getListOfAlerts("", map);
            if (products != null) {
                for (WorkProduct wp : products) {
                    String createdBy = wp.getCreatedBy();
                    int pos = createdBy.indexOf("@");
                    String user = createdBy.substring(0, pos);

                    // filter for createdBy userid!=null these are created by IPAWS
                    if (!user.equals("null")) {
                        AlertDocument alertDoc = (AlertDocument) wp.getProduct();

                        // now save in the map
                        this.alertWpTable.put(alertDoc.getAlert().getIdentifier(), wp);
                    }
                }
            }
        } catch (InvalidXpathException e) {
            e.printStackTrace();
        }

        return this.alertWpTable.keySet();
    }

    /**
     * method createAlert creates a alert work productin UICDS. It tries to find the alert work
     * product by alert identifier. If it found it, then it closes and archive the work product. It
     * then create a new alert work product.
     * 
     * @param String alertID the alert identifier
     * @pram Alert alert the Alert object to be created in UICDS
     */
    public void updateAlertWorkProduct(String alertID, Alert alert) {

        // find the alert wp
        WorkProduct wp = this.alertWpTable.get(alertID);
        try {
            if (wp != null) {
                // found it
                IdentificationType identifier = productService.getProductIdentification(wp.getProductID());

                // if the msgtype is update then close product and create new
                if (alert.getMsgType().equals(Alert.MsgType.CANCEL)) {
                    this.productService.closeProduct(identifier);
                    this.productService.archiveProduct(identifier);
                }

            } else {

                ProductPublicationStatus status = this.alertService.createAlert(null, alert);

                if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("Created alert work product ");
                    sb.append(status.getProduct().getProductID());
                    sb.append(" from IPAWS alert ");
                    sb.append(alert.getIdentifier());
                    log.info(sb.toString());
                } else {
                    StringBuffer sb = new StringBuffer();
                    sb.append("Error creating alert work product from IPAWS alert ");
                    sb.append(alert.getIdentifier());
                    sb.append(": ");
                    sb.append(status.getReasonForFailure());
                    log.error(sb.toString());
                }
            }
        } catch (Exception e) {
            // this catch all error to allow IPAWSCap to continue processing the
            // rest of the incoming cap alerts from IPAWS-OPEN
            log.error("Unable to update Alert " + alertID + e.getMessage());

            /*
            log.error("Alert = " + alert.toString());
            java.io.StringWriter writer = new java.io.StringWriter();
            java.io.PrintWriter print = new java.io.PrintWriter(writer);
            e.printStackTrace(print);
            log.error(writer.toString());
            */
            IPAWSCap.notProcessedList.add(alertID);
        }
    }

    public void closeAllAlertWPs() {

        Set<String> alertIdSet = this.alertWpTable.keySet();
        if (alertIdSet.size() == 0) {
            getAlertWorkProduct();
        }

        Iterator<String> it = alertIdSet.iterator();
        while (it.hasNext()) {
            String alertId = it.next();
            WorkProduct wp = this.alertWpTable.get(alertId);

            IdentificationType identifier = productService.getProductIdentification(wp.getProductID());

            this.productService.closeProduct(identifier);
            this.productService.archiveProduct(identifier);
        }
    }
}
