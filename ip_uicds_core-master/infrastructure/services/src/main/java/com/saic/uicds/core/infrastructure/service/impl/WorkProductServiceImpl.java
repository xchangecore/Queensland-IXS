package com.saic.uicds.core.infrastructure.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.transaction.annotation.Transactional;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.precis.x2009.x06.structures.WorkProductIdentificationDocument;
import com.saic.precis.x2009.x06.structures.WorkProductPropertiesDocument;
import com.saic.uicds.core.infrastructure.dao.WorkProductDAO;
import com.saic.uicds.core.infrastructure.exceptions.InvalidXpathException;
import com.saic.uicds.core.infrastructure.messages.JoinedPublishProductRequestMessage;
import com.saic.uicds.core.infrastructure.messages.ProductChangeNotificationMessage;
import com.saic.uicds.core.infrastructure.messages.ProductToInterestGroupAssociationMessage;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.InterestGroupManagementComponent;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.util.DocumentUtil;
import com.saic.uicds.core.infrastructure.util.LogEntry;
import com.saic.uicds.core.infrastructure.util.ServletUtil;
import com.saic.uicds.core.infrastructure.util.UUIDUtil;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;
import com.saic.uicds.core.infrastructure.util.WorkProductUtil;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The WorkProductService implementation. WorkProducts are maintained in a hibernate model.
 *
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */


@Transactional
public class WorkProductServiceImpl
        implements WorkProductService {

    private final static Logger log = LoggerFactory.getLogger(WorkProductServiceImpl.class);

    private static final int KiloBytes = 1024;

    private WorkProductDAO workProductDAO;
    private DirectoryService directoryService;
    private InterestGroupManagementComponent interestGroupManagementComponent;
    private ConfigurationService configurationService;
    private MessageChannel productAssociationChannel;
    private MessageChannel productChangeNotificationChannel;
    private MessageChannel joinedPublishProductRequestChannel;

    // private MessageChannel getProductResponseChannel;

    /**
     * Delete and Archive a work product. The work product must be associated with an interest group
     * owned by the local core. The work product must be already closed .
     *
     * @param identifier the identifier
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus archiveProduct(IdentificationType identifier) {

        if (identifier.getIdentifier() == null
                || identifier.getIdentifier().getStringValue() == null) {
            return new ProductPublicationStatus("No product Identifier to be archived");
        }

        String productID = identifier.getIdentifier().getStringValue();
        WorkProduct product = getProduct(productID);
        if (product == null) {
            return new ProductPublicationStatus(productID + " cannot be located in repository");
        }

        // figure out whether this product associate to an interest group or not
        // if it's associated with an IG then check whether this core owns it.
        // TODO - check with Aruna whether there is anything needs to be done at
        // InterestGroupManagementComponent
        Set<String> igIDSet = product.getAssociatedInterestGroupIDs();
        boolean isOwner = false;
        if (igIDSet != null && igIDSet.size() > 0) {
            for (String igID : igIDSet) {
                if (interestGroupManagementComponent.interestGroupOwnedByCore(igID) == true) {
                    isOwner = true;
                    break;
                }
            }
            // not the owner
            if (isOwner == false) {
                return new ProductPublicationStatus(getDirectoryService().getCoreName()
                        + " is not owner: Cannot archive product: " + productID);
            }
        }

        if (product.isActive() == true) {
            return new ProductPublicationStatus(productID + " has to be closed first");
        }

        ProductPublicationStatus status = deleteWorkProduct(productID);
        // even it's archive, we can still return the latest version of product
        status.setProduct(product);
        return status;
    }

    /**
     * Associate work product to interest group.
     *
     * @param workProductID the work product id
     * @param interestGroupID the interest group id
     *
     * @return the string
     * @ssdd
     */
    @Override
    public String associateWorkProductToInterestGroup(String workProductID, String interestGroupID) {

        WorkProduct wp = getProduct(workProductID);
        if (wp == null)
            return null;

        WorkProduct newWP = new WorkProduct(wp);

        // verify that the association doesn't already exist
        if ((interestGroupID != null)
                && (wp.getAssociatedInterestGroupIDs().contains(interestGroupID) == false)) {
            newWP.getAssociatedInterestGroupIDs().add(interestGroupID);
            publishProduct(newWP);
            notifyOfWorkProductInterestGroupAssociation(newWP, interestGroupID,
                    ProductToInterestGroupAssociationMessage.AssociationType.Associate);
        }
        return newWP.getProductID();
    }

    /**
     * Close product publishes a new version of the product that is marked inactive. The interest
     * group must be owned by the local core.
     *
     * @param productID the product id
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus closeProduct(IdentificationType productID) {

        if (productID == null)
            return new ProductPublicationStatus("No work product identifier specified");

        WorkProduct product = getProduct(productID);
        if (product == null)
            return new ProductPublicationStatus(productID + " cannot be located in repository");

        // figure out whether this product associate to an interest group or not
        // if it's associated with an IG then check whether this core owns it.
        // TODO - check with Aruna whether there is anything needs to be done at
        // InterestGroupManagementComponent
        Set<String> igIDSet = product.getAssociatedInterestGroupIDs();
        boolean isOwner = false;

        if(igIDSet !=null) //fli added to verify it 10/13/2011
        {
            if (igIDSet.size() > 0) {
                for (String igID : igIDSet) {
                    if (interestGroupManagementComponent.interestGroupOwnedByCore(igID) == true) {
                        isOwner = true;
                        break;
                    }
                }
                // not the owner
                if (isOwner == false) {
                    return new ProductPublicationStatus(getDirectoryService().getCoreName()
                            + " is not owner: Cannot close product: " + productID);
                }
            }
        }

        // if it's no IG associated, marked it as inactive and returned
        WorkProduct closedProduct = new WorkProduct(product);
        closedProduct.setActive(false);
        return publishIt(closedProduct, getUserID());
    }

    /**
     * The method will not generate a new version of the work product
     *
     * @param productId
     * @param act
     * @return private ProductPublicationStatus deleteIt(String productId, String act) {
     *         ProductPublicationStatus status = new ProductPublicationStatus();
     *
     *         try { WorkProduct product = getWorkProductDAO().findByProductID(productId);
     *         getWorkProductDAO().makeTransient(product);
     *         status.setStatus(ProductPublicationStatus.SuccessStatus); status.setProduct(product);
     *         return status; } catch (Exception e) { log.error("deleteWorkProduct: " +
     *         e.getMessage()); } return status; }
     */

    private ProductPublicationStatus deleteIt(String productID, boolean needNotify) {

        ProductPublicationStatus status = new ProductPublicationStatus();
        try {

            List<WorkProduct> productList = getWorkProductDAO().findAllClosedVersionOfProduct(
                    productID);
            if (productList == null || productList.size() == 0) {
                return new ProductPublicationStatus(productID + " has NOT been closed yet");
            }

            WorkProductIdentificationDocument identification = WorkProductIdentificationDocument.Factory.newInstance();
            WorkProductPropertiesDocument properties = WorkProductPropertiesDocument.Factory.newInstance();

            int version = 1;
            for (WorkProduct product : productList) {
                if (version < product.getProductVersion().intValue()) {
                    version = product.getProductVersion().intValue();
                }
            }

            for (WorkProduct product : productList) {

                log.debug("DELETE: Product:/" + product.getProductID() + "/, Version:/"
                        + product.getProductVersion() + "/ ...");

                // only save the latest/deleted version
                if (version == product.getProductVersion().intValue()) {
                    identification.setWorkProductIdentification(WorkProductHelper.getWorkProductIdentification(product));
                    properties.setWorkProductProperties(WorkProductHelper.getWorkProductProperties(product));
                }

                String interestGroupID = product.getFirstAssociatedInterestGroupID();
                if (needNotify && interestGroupID != null) {
                    // need to un-associate the product with the interest group
                    notifyOfWorkProductInterestGroupAssociation(product, interestGroupID,
                            ProductToInterestGroupAssociationMessage.AssociationType.Unassociate);
                }
                // WorkProductSerializer.getInstance().addProduct(product);
                getWorkProductDAO().makeTransient(product);
            }

            productList = getWorkProductDAO().findAllVersionOfProduct(productID);
            for (WorkProduct product : productList) {
                log.debug("DELETE: Product:/" + product.getProductID() + "/, Version:/"
                        + product.getProductVersion() + "/ ...");
                // WorkProductSerializer.getInstance().addProduct(product);
                getWorkProductDAO().makeTransient(product);
            }

            // notify PubSub of the delete
            if (identification.isNil() == false && properties.isNil() == false) {
                log.debug("deleteIt: sending DELETE message for product ID=" + productID);
                notifyOfWorkProductChange(identification, properties,
                        ProductChangeNotificationMessage.ChangeIndicator.Delete);
            } else {
                log.error("deleteIt: cannot obtain product type from the closed product ID="
                        + productID + " DELETE message not sent!");
            }

            status.setStatus(ProductPublicationStatus.SuccessStatus);
        } catch (Exception e) {
            log.error("deleteWorkProduct: " + e.getMessage());
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("deleteWorkProduct failed: " + e.getMessage());
        }

        return status;
    }

    /**
     * Queries the DAO to get all versions, including closed versions, of the workproduct and marks
     * them as transient. By default, workproducts are de-associated from interest groups with which
     * they are associated.
     *
     * @param productID the product id
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus deleteWorkProduct(String productID) {

        return deleteIt(productID, true);
    }

    /**
     * Delete work product, but does de-associate workproducts from associated interest groups.
     *
     * @param productID the product id
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus deleteWorkProductWithoutNotify(String productID) {

        return deleteIt(productID, false);
    }

    private WorkProduct doPublish(WorkProduct theProduct, boolean doNotifications) {

        WorkProduct product = null;

        final WorkProductIdentificationDocument identification= WorkProductIdentificationDocument.Factory.newInstance();
        final WorkProductPropertiesDocument properties = WorkProductPropertiesDocument.Factory.newInstance();
        try {
            log.debug(">>>> doPublish:  about to publish: " + theProduct.getProductID() + ", "
                    + theProduct.getProductType() + ", " + theProduct.getMimeType() + ", @ "
                    + theProduct.getUpdatedDate() + " version=" + theProduct.getProductVersion());

            // log.debug("doPublish: buffer=[" + new String(theProduct.getProduct()));

            product = getWorkProductDAO().makePersistent(theProduct);

            identification.addNewWorkProductIdentification().set(
                    WorkProductHelper.getWorkProductIdentification(product));

            properties.addNewWorkProductProperties().set(
                    WorkProductHelper.getWorkProductProperties(product));

            log.debug("======> published product: version="
                    + product.getProductVersion().toString() + " updatedBy=" + product.getUpdatedBy()
                    + " updatedDate=" + product.getUpdatedDate());
        } catch (Exception e) {
            log.error("doPublish. Error="+e.getMessage());
            e.printStackTrace();
        }

        if (product != null && doNotifications) {
            //this is to counter the issue where Notifications were being committed before the WorkProduct was.
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            log.debug("WORK PRODUCT TRANSACTION COMPLETE: Performing Notifications.");
                            try {
                                notifyOfWorkProductChange(identification, properties,
                                        ProductChangeNotificationMessage.ChangeIndicator.Publish);
                            } catch (Exception e) {
                                log.error("Could not Send Notifications : " + e.getMessage(), e);
                                throw new RuntimeException("Could not Send Notifications : " + e.getMessage(), e);
                            }
                        }
                    }
            );

        }

        return product;
    }

    /**
     * Find by interest group and type.
     *
     * @param interestGroupID the interest group id
     * @param productType the product type
     *
     * @return the list< work product>
     * @ssdd
     */
    @Override
    public List<WorkProduct> findByInterestGroupAndType(String interestGroupID, String productType) {

        return getWorkProductDAO().findByInterestGroupAndType(interestGroupID, productType);
    }

    // public IncidentDAO getIncidentDAO() {
    // return incidentDAO;
    // }

    /**
     * Gets the list of work products associated with and interest group.
     *
     * @param interestGroupId the interest group id
     *
     * @return the associated work product list
     * @ssdd
     */
    @Override
    public WorkProduct[] getAssociatedWorkProductList(String interestGroupId) {

        List<WorkProduct> productList = workProductDAO.findByInterestGroup(interestGroupId);
        WorkProduct[] products = null;
        if (productList != null && productList.size() > 0) {
            products = new WorkProduct[productList.size()];
        } else {
            products = new WorkProduct[0];
        }
        return productList.toArray(products);
    }

    public ConfigurationService getConfigurationService() {

        return configurationService;
    }

    public DirectoryService getDirectoryService() {

        return directoryService;
    }

    private String getIdAttribute(XmlObject object) {

        String id = null;
        XmlCursor cursor = object.newCursor();
        cursor.toNextToken();
        id = cursor.getAttributeText(NiemIdQName);
        cursor.dispose();
        return id;
    }

    /**
     * Gets the product using the IdentificationType package id
     *
     * @param pkgId the package id that identifies the work product
     *
     * @return the product
     * @ssdd
     */
    @Override
    public WorkProduct getProduct(IdentificationType pkgId) {

        if (pkgId != null) {
            return getWorkProductDAO().findByWorkProductIdentification(pkgId);
        } else {
            return null;
        }
    }

    /**
     * Gets the product using the work product id string
     *
     * @param id the id
     *
     * @return the product
     * @ssdd
     */
    @Override
    public WorkProduct getProduct(String id) {

        if (id != null) {
            return getWorkProductDAO().findByProductID(id);
        } else
            return null;
    }

    @Override
    public List<WorkProduct> getAllVersionsOfProduct(String id) {

        if (id != null) {
            return getWorkProductDAO().findAllVersionOfProduct(id);
        } else
            return null;
    }

    /**
     * Gets the product association channel.
     *
     * @return the product association channel
     */
    public MessageChannel getProductAssociationChannel() {

        return productAssociationChannel;
    }

    /**
     * Gets the product by type and x query.
     *
     * @param productType the product type
     * @param query the query
     * @param namespaceMap the namespace map
     *
     * @return the product by type and x query
     * @ssdd
     */
    @Override
    public List<WorkProduct> getProductByTypeAndXQuery(String productType, String query,
                                                       Map<String, String> namespaceMap) throws InvalidXpathException {

        List<WorkProduct> listOfProducts = new ArrayList<WorkProduct>();
        List<WorkProduct> products = getWorkProductDAO().findByProductType(productType);
        if (products != null && products.size() > 0) {
            for (WorkProduct product : products) {
                if (query == null || query.length() == 0 || namespaceMap == null
                        || namespaceMap.size() == 0
                        || DocumentUtil.exist(query, product.getProduct(), namespaceMap)) {
                    listOfProducts.add(product);
                }
            }
        }
        return listOfProducts;
    }

    /**
     * Gets the product by identifier and version.
     *
     * @param id the id
     * @param productVersion the product version
     *
     * @return the producti by version
     * @ssdd
     */
    @Override
    public WorkProduct getProductiByVersion(String id, Integer productVersion) {

        if (id != null) {
            return getWorkProductDAO().findByProductIDAndVersion(id, productVersion);
        } else
            return null;
    }

    /**
     * Gets the product identification using the string id.
     *
     * @param id the id
     *
     * @return the product identification
     * @ssdd
     */
    @Override
    public IdentificationType getProductIdentification(String id) {

        if (id != null) {
            WorkProduct product = getWorkProductDAO().findByProductID(id);
            if (product == null) {
                return null;
            }
            return WorkProductHelper.getWorkProductIdentification(product);
        } else
            return null;
    }

    /**
     * Gets the product id list by type and x query.
     *
     * @param productType the product type
     * @param query the query
     * @param namespaceMap the namespace map
     *
     * @return the product id list by type and x query
     *
     * @throws InvalidXpathException the invalid xpath exception
     * @ssdd
     */
    @Override
    public List<String> getProductIDListByTypeAndXQuery(String productType, String query,
                                                        Map<String, String> namespaceMap) throws InvalidXpathException {

        ArrayList<String> productIDs = new ArrayList<String>();
        List<WorkProduct> products = getWorkProductDAO().findByProductType(productType);
        if (products != null && products.size() > 0) {
            for (WorkProduct product : products) {
                try {
                    if (query == null || query.length() == 0 || namespaceMap == null
                            || namespaceMap.size() == 0
                            || DocumentUtil.exist(query, product.getProduct(), namespaceMap)) {
                        productIDs.add(product.getProductID());
                    }
                } catch (InvalidXpathException e) {
                    // TODO - need to propagate this ???
                    log.error(e.getMessage());
                }
            }
        }
        return productIDs;
    }

    /**
     * Gets the service name.
     *
     * @return the service name
     * @ssdd
     */
    @Override
    public String getServiceName() {

        return (PRODUCT_SERVICE_NAME);
    }

    private String getUserID() {

        String principal = ServletUtil.getPrincipalName();
        /*
        if (principal == null) {
            log.warn("Principal is null");
        } else {
            log.warn("Principal: " + principal);
        }
        */

        return new String(principal + "@" + configurationService.getFullyQualifiedHostName());
    }

    public WorkProductDAO getWorkProductDAO() {

        return this.workProductDAO;
    }

    /**
     * Determines if the workproduct id is in among the closed work products
     *
     * @param productID the product id
     *
     * @return true, if is deleted
     * @ssdd
     */
    @Override
    public boolean isDeleted(String productID) {

        List<WorkProduct> deletedProducts = workProductDAO.findAllClosedVersionOfProduct(productID);
        return deletedProducts.size() > 0 ? true : false;
    }

    /**
     * Looks up a work product by workproduct id string
     *
     * @param productID the product id
     *
     * @return true, if is existed
     * @ssdd
     */
    @Override
    public boolean isExisted(String productID) {

        WorkProduct product = getWorkProductDAO().findByProductID(productID);
        return (product != null);
    }

    /**
     * List all work products.
     *
     * @return the list< work product>
     * @ssdd
     */
    @Override
    public List<WorkProduct> listAllWorkProducts() {

        List<WorkProduct> products = getWorkProductDAO().findAll();
        // if (log.isDebugEnabled() && products != null) {
        // for (WorkProduct product : products) {
        // log.debug("listAllWorkProducts: Product ID: " + product.getProductID());
        // }
        // }
        return products;
    }

    /**
     * List work products for a given product type.
     *
     * @param type the type
     *
     * @return the list< work product>
     * @ssdd
     */
    @Override
    public List<WorkProduct> listByProductType(String type) {

        return getWorkProductDAO().findByProductType(type);
    }

    private void notifyOfWorkProductChange(WorkProductIdentificationDocument identification,
                                           WorkProductPropertiesDocument properties,
                                           ProductChangeNotificationMessage.ChangeIndicator changeIndicaor) {

        ProductChangeNotificationMessage notification = new ProductChangeNotificationMessage(
                identification, properties, changeIndicaor);

        log.debug("Work product change notification: " + notification.getProductID());

        Message<ProductChangeNotificationMessage> message = new GenericMessage<ProductChangeNotificationMessage>(
                notification);
        try {
            productChangeNotificationChannel.send(message);
        } catch (Exception e) {
            // System.err.println("notifyOfWorkProductChange Exception sending message: "
            // + e.getMessage());
            log.error("Exception sending message on productChangeNotificationChannel: "
                    + e.getMessage());
        }
    }

    private void notifyOfWorkProductInterestGroupAssociation(WorkProduct product,
                                                             String interestGroupID,
                                                             ProductToInterestGroupAssociationMessage.AssociationType associationType) {

        log.debug("Notify Communication Service to "
                + (associationType == ProductToInterestGroupAssociationMessage.AssociationType.Associate
                ? "associate"
                : "de-associate") + " work product to incident");

        // Incident incident = getIncidentDAO().findByIncidentID(product.getIncidentID());
        // if (incident == null) {
        // log.error("Cannot locate the Incident: " + product.getIncidentID());
        // return;
        // }

        InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(interestGroupID);

        if (igInfo == null) {
            // this should not have happened since we already made this check
            log.error(product.getProductID() + " is associated to an unkown interest group ID  "
                    + interestGroupID + " no association sent");
            return;
        }

        // if it's shared work product not need to notify the communication service
        if (igInfo.getOwningCore().equals(getConfigurationService().getCoreName()) == false) {
            log.error(product.getProductID() + " is owned by " + igInfo.getOwningCore()
                    + " and this core is " + getConfigurationService().getCoreName()
                    + ". No notification needed");
            return;
        }

        ProductToInterestGroupAssociationMessage notification = new ProductToInterestGroupAssociationMessage();
        notification.setAssociationType(associationType);
        notification.setProductId(product.getProductID());
        notification.setProductType(product.getProductType());
        notification.setInterestGroupId(igInfo.getInterestGroupID());
        notification.setOwningCore(igInfo.getOwningCore());

        Message<ProductToInterestGroupAssociationMessage> message = new GenericMessage<ProductToInterestGroupAssociationMessage>(
                notification);
        log.debug("sending AssociateProductToIncidentMessage");
        getProductAssociationChannel().send(message);
    }

    private ProductPublicationStatus publishIt(WorkProduct theProduct, String userID) {

        WorkProduct product = null;
        boolean notifyOfAssociation = false;

        LogEntry logEntry = new LogEntry();

        log.debug("publishIt:  productType=" + theProduct.getProductType() + " productID="
                + theProduct.getProductID() + " version=" + theProduct.getProductVersion());

        ProductPublicationStatus status = new ProductPublicationStatus();

        if (theProduct.getProductID() != null && !theProduct.getProductID().isEmpty()) {
            log.debug("theProduct.getProductID() is " + theProduct.getProductID());
            product = getProduct(theProduct.getProductID());
            if (product != null && product.getId() != null) {
                theProduct.setId(product.getId());
            }
        } else {
            log.debug("theProduct.getProductID() is null or empty");
        }

        if (product != null) {
            log.debug("product from DB: " + product.getProductType() + ", " + product.getMimeType()
                    + ", @ " + product.getUpdatedDate() + " version=" + product.getProductVersion());

            // this is an update, verify that this is valid request, i.e. version number and
            // checksum match those in the current copy in the database
            log.debug("product from DB: " + product.getProductType() + ", " + product.getMimeType()
                    + ", @ " + product.getUpdatedDate() + " version=" + product.getProductVersion());

            // make sure the update is based on the current version of the work product-
            if ((!theProduct.getProductVersion().equals(product.getProductVersion()))
                    || (!theProduct.getChecksum().equals(product.getChecksum()))) {

                status.setStatus("Failure");
                String reason = "Invalid version number and/or checksum: "
                        + " specified version number=" + theProduct.getProductVersion()
                        + "; specified checksum=[" + theProduct.getChecksum() + "]."
                        + "  The current version number=" + product.getProductVersion();
                status.setReasonForFailure(reason);
                return status;
            }
        }

        if (theProduct.getFirstAssociatedInterestGroupID() != null) {
            InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(theProduct.getFirstAssociatedInterestGroupID());
            if (igInfo == null) {
                log.error("===>>> Unkown interest group ID  "
                        + theProduct.getFirstAssociatedInterestGroupID() + " provided for association.");
                status.setStatus("Failure");
                String reason = "Unkown interest group ID: "
                        + theProduct.getFirstAssociatedInterestGroupID() + " provided for association.";
                status.setReasonForFailure(reason);
                return status;
            } else {
                if ((product == null)
                        || (product.getAssociatedInterestGroupIDs().contains(
                        theProduct.getFirstAssociatedInterestGroupID()) == false)) {
                    notifyOfAssociation = true;
                }
            }
        }

        Date date = new Date();
        // the publish/update request is valid
        if ((product != null) && (product.getProductVersion() != null)) {
            // this is an update to an existing product, increment the version number
            Integer newVersion = product.getProductVersion() + 1;
            theProduct.setProductVersion(newVersion);
            theProduct.setCreatedBy(product.getCreatedBy());
            theProduct.setCreatedDate(product.getCreatedDate());

            logEntry.setAction(LogEntry.ACTION_WORKPRODUCT_UPDATE);
            logEntry.setUpdatedBy(product.getUpdatedBy());
        } else {
            // first publication, set the product ID, initial version number, creator and the
            // created date

            // this is done for IMS who needs to generate its own product ID first in order to
            // insert the product ID into the incident document to be published
            // TODO: need a better mechanism for doing this so that WorkProductService is the only
            // place the work product ID is generated.
            if (theProduct.getProductID() == null || theProduct.getProductID().isEmpty()) {
                theProduct.setProductID(UUIDUtil.getID(theProduct.getProductType()));
            }
            theProduct.setProductVersion(1);
            theProduct.setCreatedBy(userID);
            theProduct.setCreatedDate(date);

            logEntry.setAction(LogEntry.ACTION_WORKPRODUCT_CREATE);
            logEntry.setCreatedBy(theProduct.getCreatedBy());
        }

        theProduct.setUpdatedBy(userID);
        theProduct.setUpdatedDate(date);

        log.debug("=======> publishIt: interestGroupID="
                + theProduct.getFirstAssociatedInterestGroupID());

        // set size in kilobytes
        // TODO do we really need this ???
        theProduct.setSize(theProduct.getProduct().toString().length() / KiloBytes);

        String checksum = WorkProductUtil.calculateChecksum(theProduct.getUpdatedDate().toString(),
                theProduct.getProductVersion(), theProduct.getSize());
        theProduct.setChecksum(checksum);

        if (theProduct.getMimeType() == null)
            theProduct.setDefaultMimeType();

        // try {
        // log.debug("======> about to publish: " + theProduct.getProductID() + ", "
        // + theProduct.getProductType() + ", " + theProduct.getMimeType() + ", @ "
        // + theProduct.getUpdatedDate() + " version=" + theProduct.getProductVersion());
        // product = getWorkProductDAO().makePersistent(theProduct);
        // log.debug("======> published product: version="
        // + product.getProductVersion().toString() + " updatedBy="
        // + product.getUpdatedBy() + " updatedDate=" + product.getUpdatedDate());
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        WorkProduct newProduct = doPublish(theProduct, true);

        // close old product if new product was publish successfully and it was not the first
        // version
        // if (newProduct != null && product != null && product.isActive()
        // && theProduct.getProductVersion() > 1) {
        // product.setActive(false);
        // doPublish(product, false);
        // }

        if ((newProduct != null) && (notifyOfAssociation)) {
            // notify Comms of the association
            notifyOfWorkProductInterestGroupAssociation(newProduct,
                    newProduct.getFirstAssociatedInterestGroupID(),
                    ProductToInterestGroupAssociationMessage.AssociationType.Associate);
        }

        if (newProduct != null) {
            logEntry.setWorkProductId(newProduct.getProductID());
            logEntry.setWorkProductType(newProduct.getProductType());
            logEntry.setWorkProductSize(newProduct.getSize().toString());
            logEntry.setCategory(LogEntry.CATEGORY_WORKPRODUCT);
            log.info(logEntry.getLogEntry());

            status.setStatus("Success");
            status.setProduct(newProduct);
            return status;
        } else {
            status.setStatus("Failure");
            status.setReasonForFailure("Internal Error: Unable to publish work product");
            return status;
        }
    }

    /**
     * Publish product in response from a request from a joined core.
     *
     * @param wp the wp
     * @param userID the user id
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus publishProducRequesttFromJoinedCore(WorkProduct wp,
                                                                        String userID) {

        // we are the incident owning core, who has just received a joined core's request to update
        // or publish a work product associated to the shared incident.
        // Note: For now we just turn around and publish the work product as requested
        // In the future, it is possible that some involvement from the corresponding UICDS service
        // may be required.
        return publishIt(wp, userID);
    }

    // public void setGetProductResponseChannel(MessageChannel channel) {
    // getProductResponseChannel = channel;
    // }

    // public void setIncidentDAO(IncidentDAO incidentDAO) {
    // this.incidentDAO = incidentDAO;
    // }

    /**
     * Publish product. Verify that the work product is active. If there is an associated incident,
     * then this is a joined core so send the publish request to the owning core. Otherwise this is
     * the owning core, so publish the work product directly.
     *
     * @param theProduct the the product
     *
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus publishProduct(WorkProduct theProduct) {

        log.debug("publishProduct:  productType=" + theProduct.getProductType() + " productID="
                + theProduct.getProductID() + " productVersion=" + theProduct.getProductVersion()
                + " core=" + directoryService.getCoreName());

        // check whether it's Inactive or not
        if (theProduct != null && theProduct.getProductID() != null) {
            WorkProduct product = getProduct(theProduct.getProductID());
            if (product != null && product.isActive() == false) {
                return new ProductPublicationStatus(theProduct.getProductID() + " is inactive");
            }
        }

        if (theProduct.getProductType() == null) {
            log.error(">>> Error: NULL productType received");
            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus("Failure");
            status.setReasonForFailure("Internal Error: Unable to publish work product");
            return status;
        }

        InterestGroupInfo interestGroupInfo = null;

        String userID = getUserID();

        String interestGroupID = theProduct.getFirstAssociatedInterestGroupID();

        // Check to see if there is an associated incident
        if (interestGroupID != null) {
            interestGroupInfo = interestGroupManagementComponent.getInterestGroup(interestGroupID);
        }

        if ((interestGroupInfo != null)
                && (interestGroupInfo.getOwningCore().equals(directoryService.getCoreName()) != true)) {
            // We are a joined core (i.e. joined to this incident) and therefore this a
            // publish/update by a joined core.
            // Send the publish request to owning core for approval first.

            log.debug("This an publish/update by a joined core=" + directoryService.getCoreName()
                    + "    ...   Send update request to owning core  "
                    + interestGroupInfo.getOwningCore() + " for approval first.");

            try {
                ProductPublicationStatus status = new ProductPublicationStatus();
                String act = WorkProductUtil.getACT();

                // TODO: Problems!!!
                // the package identification is not set yet
                // Maybe we should consider other ways of streaming work product model across the
                // XMPP connection
                // without having to use precis type
                WorkProductDocument doc = WorkProductHelper.toWorkProductDocument(theProduct);

                String wpString = null;
                wpString = doc.toString();

                // log.debug("====> wpString=[" + wpString + "]");

                JoinedPublishProductRequestMessage msg = new JoinedPublishProductRequestMessage();
                msg.setAct(act);
                msg.setUserID(userID);
                msg.setInterestGroupId(theProduct.getFirstAssociatedInterestGroupID());
                msg.setOwningCore(interestGroupInfo.getOwningCore());
                msg.setRequestingCore(directoryService.getCoreName());
                msg.setProductId(theProduct.getProductID());
                msg.setProductType(theProduct.getProductType());
                msg.setWorkProduct(wpString);
                Message<JoinedPublishProductRequestMessage> message = new GenericMessage<JoinedPublishProductRequestMessage>(
                        msg);
                log.debug("===>publishProduct:  sending JoinedPublishProductRequestMessage message");
                joinedPublishProductRequestChannel.send(message);

                status.setStatus("Pending");
                status.setAct(act);
                return status;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            // we are the incident owning core
            return publishIt(theProduct, userID);
        }
    }

    /**
     * Publish product from owner. This is a joined core that has received publication of a work
     * product associated with a shared incident from the incident owning core
     *
     * @param product the product
     * @ssdd
     */
    @Override
    public void publishProductFromOwner(WorkProduct theProduct) {

        // Get the current version of the product
        WorkProduct product = null;
        if (theProduct.getProductID() != null && !theProduct.getProductID().isEmpty()) {
            product = getProduct(theProduct.getProductID());

            //for double secure and use the product id from product. 
            if((product !=null) && (product.getProductID() !=null))
            {
                theProduct.setProductID(product.getProductID());

                //let try second time.
                //theProduct.setId(product.getId());
            }
        }

        // publish the new version
        WorkProduct newProduct = doPublish(theProduct, true);
    }

    /* this is purge when there is orphan incident/work products */
    @Override
    public void purgeWorkProduct(String productID) {

        List<WorkProduct> productList = getWorkProductDAO().findAllVersionOfProduct(productID);
        for (WorkProduct product : productList) {
            log.debug("PURGE: Product:/" + product.getProductID() + "/, Version:/"
                    + product.getProductVersion() + "/ ...");
            getWorkProductDAO().makeTransient(product);
        }
    }

    public void setConfigurationService(ConfigurationService configurationService) {

        this.configurationService = configurationService;
    }

    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    public void setInterestGroupManagementComponent(
            InterestGroupManagementComponent interestGroupManagementComponent) {

        this.interestGroupManagementComponent = interestGroupManagementComponent;
    }

    public void setJoinedPublishProductRequestChannel(
            MessageChannel joinedPublishProductRequestChannel) {

        this.joinedPublishProductRequestChannel = joinedPublishProductRequestChannel;
    }

    public void setProductAssociationChannel(MessageChannel productAssociationChannel) {

        this.productAssociationChannel = productAssociationChannel;
    }

    public void setProductChangeNotificationChannel(MessageChannel channel) {

        productChangeNotificationChannel = channel;
    }

    public void setWorkProductDAO(WorkProductDAO workProductDAO) {

        this.workProductDAO = workProductDAO;
    }

    /**
     * X path executed.
     *
     * @param productID the product id
     * @param path the path
     * @param namespaceMap the namespace map
     *
     * @return true, if successful
     *
     * @throws InvalidXpathException the invalid xpath exception
     * @ssdd
     */
    @Override
    public boolean xPathExecuted(String productID, String path, Map<String, String> namespaceMap)
            throws InvalidXpathException {

        return DocumentUtil.exist(path, getProduct(productID).getProduct(), namespaceMap);
    }
}
