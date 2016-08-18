package com.saic.uicds.core.infrastructure.service.impl;

import gov.ucore.ucore.x20.EventType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.oasisOpen.docs.wsn.b2.FilterType;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.uicds.agreementService.AgreementType;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.notificationService.NotifyRequestDocument;
import org.uicds.notificationService.WorkProductDeletedNotificationDocument;
import org.uicds.workProductService.WorkProductPublicationResponseDocument;
import org.uicds.workProductService.WorkProductPublicationResponseType;
import org.w3.x2005.x08.addressing.EndpointReferenceType;
import org.w3.x2005.x08.addressing.MetadataType;

import com.ibm.icu.util.StringTokenizer;
import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.uicds.core.infrastructure.dao.AgreementDAO;
import com.saic.uicds.core.infrastructure.dao.NotificationDAO;
import com.saic.uicds.core.infrastructure.dao.ProductSubscriptionByTypeDAO;
import com.saic.uicds.core.infrastructure.exceptions.EmptySubscriberNameException;
import com.saic.uicds.core.infrastructure.exceptions.InvalidProductIDException;
import com.saic.uicds.core.infrastructure.exceptions.InvalidProductTypeException;
import com.saic.uicds.core.infrastructure.exceptions.NullSubscriberException;
import com.saic.uicds.core.infrastructure.listener.NotificationListener;
import com.saic.uicds.core.infrastructure.messages.AgreementRosterMessage;
import com.saic.uicds.core.infrastructure.messages.ProductChangeNotificationMessage;
import com.saic.uicds.core.infrastructure.messages.ProfileNotificationMessage;
import com.saic.uicds.core.infrastructure.model.Agreement;
import com.saic.uicds.core.infrastructure.model.Notification;
import com.saic.uicds.core.infrastructure.model.NotificationMessage;
import com.saic.uicds.core.infrastructure.model.NotificationSubscription;
import com.saic.uicds.core.infrastructure.model.ProductSubscriptionByType;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.CommunicationsService;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.NotificationService;
import com.saic.uicds.core.infrastructure.service.PubSubService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.util.AgreementUtil;
import com.saic.uicds.core.infrastructure.util.DigestHelper;
import com.saic.uicds.core.infrastructure.util.FilterUtil;
import com.saic.uicds.core.infrastructure.util.LogEntry;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

/**
 * The NotificationService implementation.
 *
 * @see com.saic.uicds.core.infrastructure.model.Agreement Agreement Data Model
 * @see com.saic.uicds.core.infrastructure.model.Notification Notification Data Model
 * @see com.saic.uicds.core.infrastructure.model.NotificationMessage Notification Message Data Model
 * @see com.saic.uicds.core.infrastructure.model.NotificationSubscription NotificationSubscription
 *      Data Model
 * @see com.saic.uicds.core.infrastructure.model.ProductSubscriptionByType ProductSubscriptionByType
 *      Data Model
 * @see com.saic.uicds.core.infrastructure.model.Profile Profile Data Model
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class NotificationServiceImpl
        implements NotificationService, ServiceNamespaces {

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    /** The configuration service. */
    private ConfigurationService configurationService;

    /** The pub sub service. */
    private PubSubService pubSubService;

    /** The directory service. */
    private DirectoryService directoryService;

    private CommunicationsService communicationsService;

    private class MessageComparator
            implements Comparator<NotificationMessage> {

        @Override
        public int compare(NotificationMessage msg1, NotificationMessage msg2) {

            return msg1.getId() - msg2.getId();
        }
    }

    /**
     * Sets the directory service.
     *
     * @param directoryService the new directory service
     */
    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    public CommunicationsService getCommunicationsService() {

        return communicationsService;
    }

    public void setCommunicationsService(CommunicationsService communicationsService) {

        this.communicationsService = communicationsService;
    }

    /** The work product service. */
    private WorkProductService workProductService;

    /**
     * Sets the work product service.
     *
     * @param workProductService the new work product service
     */
    public void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }

    /** The notification dao. */
    private NotificationDAO notificationDAO;

    /**
     * Sets the notification dao.
     *
     * @param n the new notification dao
     */
    public void setNotificationDAO(NotificationDAO n) {

        this.notificationDAO = n;
    }

    /**
     * Gets the notification dao.
     *
     * @return the notification dao
     */
    public NotificationDAO getNotificationDAO() {

        return notificationDAO;
    }

    /** The agreement dao. */
    private AgreementDAO agreementDAO;

    /**
     * Gets the agreement dao.
     *
     * @return the agreement dao
     */
    public AgreementDAO getAgreementDAO() {

        return agreementDAO;
    }

    /**
     * Sets the agreement dao.
     *
     * @param agreementDAO the new agreement dao
     */
    public void setAgreementDAO(AgreementDAO agreementDAO) {

        this.agreementDAO = agreementDAO;
    }

    /** The product subscription by type dao. */
    private ProductSubscriptionByTypeDAO productSubscriptionByTypeDAO;

    /**
     * Sets the product subscription by type dao.
     *
     * @param n the new product subscription by type dao
     */
    public void setProductSubscriptionByTypeDAO(ProductSubscriptionByTypeDAO n) {

        this.productSubscriptionByTypeDAO = n;
    }

    /**
     * Gets the product subscription by type dao.
     *
     * @return the product subscription by type dao
     */
    public ProductSubscriptionByTypeDAO getProductSubscriptionByTypeDAO() {

        return productSubscriptionByTypeDAO;
    }

    /** The web service template. */
    private WebServiceTemplate webServiceTemplate;

    /**
     * Sets the web service template.
     *
     * @param webServiceTemplate the new web service template
     */
    public void setWebServiceTemplate(WebServiceTemplate webServiceTemplate) {

        this.webServiceTemplate = webServiceTemplate;
    }

    /** The log. */
    Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    /**
     * System initialized handler.
     *
     * @param messgae the messgae
     */
    public void systemInitializedHandler(String messgae) {

        WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        directoryService.registerUICDSService(NS_NotificationService, NOTIFICATION_SERVICE_NAME,
                typeList, typeList);
        sendSunscriberInterface();
    }

    /**
     * Gets the matching messages for the specified entity Id.
     *
     * @param entityId the entity id
     * @return the matching messages
     * @ssdd
     */
    @Override
    @Transactional
    public /* synchronized */ IdentificationType[] getMatchingMessages(String entityId) {

        IdentificationType[] identifications = null;

        Notification notification = notificationDAO.findByEntityId(entityId);
        if (notification != null) {
            List<IdentificationType> workProductIdentificationList = new ArrayList<IdentificationType>();

            log.debug("getMatchingMessages for entityId=" + notification.getEntityID());
            Set<NotificationSubscription> notificationSubscriptionSet = notification.getSubscriptions();

            for (NotificationSubscription notificationSubscription : notificationSubscriptionSet) {

                Integer subscriptionId = notificationSubscription.getSubscriptionID();

                List<ProductSubscriptionByType> productSubscriptionList = productSubscriptionByTypeDAO.findBySubscriptionId(subscriptionId);
                for (ProductSubscriptionByType productSubscription : productSubscriptionList) {
                    List<WorkProduct> workProductList = workProductService.listByProductType(productSubscription.getProductType());
                    for (WorkProduct workProduct : workProductList) {
                        IdentificationType identification = WorkProductHelper.getWorkProductIdentification(workProduct);
                        workProductIdentificationList.add(identification);
                    }
                }
            }

            if (workProductIdentificationList != null && workProductIdentificationList.size() > 0) {
                identifications = new IdentificationType[workProductIdentificationList.size()];
            } else {
                identifications = new IdentificationType[0];
            }
            workProductIdentificationList.toArray(identifications);
        }
        return identifications;
    }

    /**
     * Searches all notifications and sends any subscription messages to the subscriber interface.
     *
     * @ssdd
     */
    public /* synchronized */ void sendSunscriberInterface() {

        // search all notifications and update any subscription msgs which have specified ID
        List<Notification> notifications = notificationDAO.findAll();
        if (!notifications.isEmpty()) {
            pubSubService.subscriberInterface(this);
        }
    }

    /**
     * Creates the pull point.
     *
     * @param entityID the entity id
     * @return the endpoint reference type
     * @ssdd
     */
    @Override
    public /* synchronized */ EndpointReferenceType createPullPoint(String entityID) {

        XmlOptions options = new XmlOptions();
        options.setSaveInner();
        EndpointReferenceType endpoint = EndpointReferenceType.Factory.newInstance();

        // Set the url for the notification service for pull points
        endpoint.addNewAddress().setStringValue(
                getConfigurationService().getWebServiceBaseURL() + "/" + entityID);

        // Add the service identification
        MetadataType metadata = endpoint.addNewMetadata();
        XmlCursor xc = metadata.newCursor();
        xc.toNextToken();
        xc.insertElementWithText("scheme",
                getConfigurationService().getServiceNameURN(NOTIFICATION_SERVICE_NAME));
        xc.dispose();

        // create or update notification model and persist
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification == null) {
            notification = new Notification();
            notification.setEntityID(entityID);
            notification.setEndpointWS(false);// not webservice but pullpoint
        }
        notification.setEndpointURL(endpoint.getAddress().getStringValue());
        makePersistent(notification);

        return endpoint;
    }

    @Override
    public /* synchronized */ boolean destroyPullPoint(String entityID) {

        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification == null) {
            return false;
        }

        // Remove all the subscriptions for this pull point
        for (NotificationSubscription subscription : notification.getSubscriptions()) {
            pubSubService.unsubscribeBySubscriptionID(subscription.getSubscriptionID());
        }

        // Given the CASCADE, clearing subscriptions and making the notification transient causes an
        // constraint error.
        // HashSet<NotificationSubscription> subscriptions = new
        // HashSet<NotificationSubscription>();
        // notification.setSubscriptions(subscriptions);

        // Remove all the messages
        notification.clearMessages();

        // making the notification transient causes an constraint error if you try
        // a findByEntityId again on the same entity So until we figure out that issue
        // we'll just leave the Notification object
        makeTransient(notification);
        notification = null;

        return true;
    }

    /**
     * Gets the pull point.
     *
     * @param entityID the entity id
     * @return the pull point or null if one doesn't exist for the entity id
     * @ssdd
     */
    public /* synchronized */ EndpointReferenceType getPullPoint(String entityID) {

        EndpointReferenceType endpoint = null;
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification != null) {
            endpoint = EndpointReferenceType.Factory.newInstance();
            endpoint.addNewAddress().setStringValue(notification.getEndpointURL());
            // Add the service identification
            MetadataType metadata = endpoint.addNewMetadata();
            XmlCursor xc = metadata.newCursor();
            xc.toNextToken();
            xc.insertElementWithText("scheme",
                    getConfigurationService().getServiceNameURN(NOTIFICATION_SERVICE_NAME));
            xc.dispose();
        } else {
            return null;
        }
        return endpoint;
    }

    /**
     * Update endpoint.
     *
     * @param entityID the entity id
     * @param endpointAddress the endpoint address
     * @param isWebService the is web service
     * @ssdd
     */
    public /* synchronized */ void updateEndpoint(String entityID, String endpointAddress,
                                                  boolean isWebService) {

        EndpointReferenceType endpoint = null;
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification == null) {
            notification = new Notification();
            notification.setEntityID(entityID);
        }
        notification.setEndpointURL(endpointAddress);
        notification.setEndpointWS(isWebService); // flag to tell notification if this is WS URL
        notification = makePersistent(notification);
        if (notification == null) {
            log.error("Error updating notification - makePersistent returned null");
        }
    }

    /**
     * Gets the messages.
     *
     * @param entityID the entity id
     * @param num the num
     * @return the messages
     * @ssdd
     */
    @Override
    @Transactional
    public /* synchronized */ NotificationMessageHolderType[] getMessages(String entityID, int num) {

        NotificationMessageHolderType[] response = null;

        // make a performance log entry
        LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_NOTIFICATION);
        logEntry.setAction(LogEntry.ACTION_NOTIFICATION_POLL);
        logEntry.setEntityId(entityID);
        log.info(logEntry.getLogEntry());
        try {
            // if there's an entry for this id (i.e. bonnera@core.1.saic.com)
            Notification notification = notificationDAO.findByEntityId(entityID);
            if (notification != null) {
                // Got one!
                // log.debug("Found " + entityID);
                ArrayList<NotificationMessageHolderType> messages = new ArrayList<NotificationMessageHolderType>();

                // Get Subscriptions, iterate through, and build message per workProduct message
                Set<NotificationMessage> notfMessages = notification.getMessages();
                if (notfMessages.size() > 0) {
                    if (log.isInfoEnabled()) {
                        log.info("Have " + notfMessages.size() + " notifications for " + entityID);
                    }
                    List<NotificationMessage> notificationMessages = new ArrayList<NotificationMessage>();
                    notificationMessages.addAll(notfMessages);
                    Collections.sort(notificationMessages, new MessageComparator());
                    for (NotificationMessage msg : notificationMessages) {
                        // log.debug("Notification message content: " + msg.getMessage());
                        messages.add(createNotificationMessageHolder(msg.getType(),
                                new String(msg.getMessage())));
                    }

                    // Removes messages from queue
                    clearNotificationMessages(notification);
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("No notifications for " + entityID);
                    }
                }

                response = new NotificationMessageHolderType[messages.size()];
                response = messages.toArray(response);

            } else {
                log.error(entityID + " not found in subscription map");
            }
        } catch (Exception e) {
             log.error(String.format("Error with getMessages(%s): %s", entityID, e.getMessage()), e);
        }
        return response;
    }

    /**
     * Gets the current message.
     *
     * @param topic the topic
     * @return the current message
     * @ssdd
     */
    @Override
    public NotificationMessageHolderType getCurrentMessage(QName topic) {

        log.debug("Looking for last current message on topic: " + topic.toString());
        String message = pubSubService.getLastPublishedMessage(topic.toString());
        log.debug("Last current message received: " + message);

        NotificationMessageHolderType t = NotificationMessageHolderType.Factory.newInstance();

        if (message != null) {
            if (topic.toString().toLowerCase().startsWith("profile")) {
                t = createNotificationMessageHolder(ProfileNotificationMessage.NAME, message);
            } else if (topic.toString().toLowerCase().startsWith("agreement")) {
                t = createNotificationMessageHolder(AgreementRosterMessage.NAME, message);
            } else {
                t = createNotificationMessageHolder("WorkProductID", message);
            }
        }
        return t;
    }

    /**
     * Creates the notification message holder.
     *
     * @param msgType the msg type
     * @param message the message
     * @return the notification message holder type
     */
    private NotificationMessageHolderType createNotificationMessageHolder(String msgType,
                                                                          String message) {

        NotificationMessageHolderType t = NotificationMessageHolderType.Factory.newInstance();
        Message m = t.addNewMessage();
        XmlCursor xc = m.newCursor();
        xc.toNextToken();
        XmlCursor ec = null;

        // if notification msg is profile msg type
        // if (msgType.equals(ProfileNotificationMessage.NAME)) {
        // Profile profile = profileDAO.findByEntityID(message);
        // if (profile != null) {
        // UserProfileType userProfile = ProfileUtil.copyProperties(profile);
        // ec = userProfile.newCursor();
        // }
        // }
        // if notification msg is agreement msg type
        // else if (msgType.equals(AgreementRosterMessage.NAME)) {
        if (msgType.equals(AgreementRosterMessage.NAME)) {
            Agreement agreement = agreementDAO.findByCoreID(message);
            if (agreement != null) {
                AgreementType agreementType = AgreementUtil.copyProperties(agreement);
                ec = agreementType.newCursor();
            }
        }
        // if notification msg is a notify msg
        else if (msgType.equals(NOTIFY_MESSAGE)) {
            log.debug("NOTIFY MESSAGE FOUND");
            XmlObject doc;
            try {
                doc = XmlObject.Factory.parse(message);
                if (doc != null) {
                    ec = doc.newCursor();
                }
            } catch (XmlException e) {
                log.debug("createNotificationMessageHolder: Error parsing message [" + message
                        + "]  into xml object");
                e.printStackTrace();
            }

            // xc.toChild(notificationMsg.getMessage());
        } else if (msgType.equals("WorkProductDeleted")) {
            log.debug("WorkProductDeleted MESSAGE FOUND");
            XmlObject doc;
            try {
                doc = XmlObject.Factory.parse(message);
                if (doc != null) {
                    ec = doc.newCursor();
                }
            } catch (XmlException e) {
                log.debug("createNotificationMessageHolder: Error parsing message [" + message
                        + "]  into xml object");
                e.printStackTrace();
            }
        }
        // else if notification msg is workProductID
        else {
            // WorkProduct product = workProductService.getProduct(message);
            // WorkProductNotificationType wpNotification =
            // WorkProductNotificationType.Factory.newInstance();
            // wpNotification.addNewWorkProduct().set(WorkProductHelper.toWorkProductSummary(product));
            // ec = wpNotification.newCursor();
            log.debug("WorkProductID MESSAGE FOUND");
            XmlObject doc;
            try {
                doc = XmlObject.Factory.parse(message);
                if (doc != null) {
                    ec = doc.newCursor();
                }
            } catch (XmlException e) {
                log.debug("createNotificationMessageHolder: Error parsing message [" + message
                        + "]  into xml object");
                e.printStackTrace();
            }
        }

        if (ec != null) {
            ec.toFirstContentToken();
            ec.moveXml(xc);
            ec.dispose();
        }
        xc.dispose();

        return t;
    }

    /**
     * Invalid xpath notification.
     *
     * @param subscriptionId the subscription id
     * @param errorMessage the error message
     * @ssdd
     */
    @Override
    public /* synchronized */ void InvalidXpathNotification(Integer subscriptionId, String errorMessage) {

        // search all notifications and update any subscription msgs which have specified ID
        for (Notification notification : notificationDAO.findAll()) {
            for (NotificationSubscription sub : notification.getSubscriptions()) {
                if (subscriptionId.compareTo(sub.getSubscriptionID()) == 0) {
                    ProductPublicationStatus status = new ProductPublicationStatus();
                    status.setStatus(ProductPublicationStatus.FailureStatus);
                    status.setReasonForFailure("Subscription for [" + notification.getEntityID()
                            + "]. " + errorMessage);
                    WorkProductPublicationResponseType errorResponse = WorkProductHelper.toWorkProductPublicationResponse(status);
                    WorkProductPublicationResponseDocument errorResponseDoc = WorkProductPublicationResponseDocument.Factory.newInstance();
                    errorResponseDoc.addNewWorkProductPublicationResponse().set(errorResponse);

                    addNotificationMessage(notification, subscriptionId, NOTIFY_MESSAGE,
                            errorResponseDoc.toString());
                }
            }
        }
    }

    /**
     * Gets the service name.
     *
     * @return the service name
     */
    @Override
    public String getServiceName() {

        return (NOTIFICATION_SERVICE_NAME);
    }

    /**
     * New work product version. Searches all notifications and updates any subscription messages
     * which have the specified subscription Id Executes every time a new version on any workproduct
     * is updated
     *
     * @param workProductID the work product id
     * @param subscriptionId the subscription id
     * @ssdd
     */
    @Override
    // @Transactional(propagation = Propagation.REQUIRES_NEW)
    public /* synchronized */ void newWorkProductVersion(final String productID,
                                                         final Integer subscriptionId) {

        // get the product
        final WorkProduct product = workProductService.getProduct(productID);

        final WorkProductDocument productDoc = WorkProductDocument.Factory.newInstance();
        final WorkProductDocument.WorkProduct summary = WorkProductHelper.toWorkProductSummary(product);
        productDoc.setWorkProduct(summary);
        String productVersion = WorkProductHelper.getProductVersion(summary);
        final int version = Integer.parseInt(productVersion);

        try {
            TransactionTemplate tt = new TransactionTemplate(platformTransactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            // tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
            tt.execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(TransactionStatus status) {

                    // search all notifications and update any subscription msgs which have
                    // specified ID
                    List<Notification> notifications = notificationDAO.findBySubscriptionId(subscriptionId);

                    for (Notification notification : notifications) {
                        log.debug("adding workproduct: " + productID + " to subid: "
                                + subscriptionId + " to entity: " + notification.getEntityID());

                        productDoc.setWorkProduct(summary);

                        // if endpoint is web service url
                        if (notification.isEndpointWS()) {
                            log.debug("Notification going to be SENT to webServiceURL: "
                                    + notification.getEndpointURL());
                            String url = notification.getEndpointURL();
                            try {
                                URI u = new URI(url);
                                String proto = u.getScheme();
                                // log.debug("Handling protocol: " + proto);
                                if (proto.equalsIgnoreCase("xmpp")) {
                                    try {
                                        log.debug("XMPP message sent to: " + u.getHost());
                                        communicationsService.sendXMPPMessage(
                                                getNotificationMessageBody(product), null,
                                                productDoc.xmlText(), u.getSchemeSpecificPart());
                                    } catch (IllegalArgumentException e) {
                                        log.error("IllegalArgumentException newWorkProductVersion sending XMPP message: "
                                                + e.getMessage());
                                    } catch (Exception e) {
                                        log.error("Exception newWorkProductVersion IllegalArgumentException: "
                                                + e.getMessage());
                                    }
                                } else {
                                    // invoke specified webService
                                    log.debug("WS-Notify message sent to :" + u.toString());
                                    invokeWebServiceTemplate(notification, "WorkProductID",
                                            productDoc.toString());
                                }
                            } catch (URISyntaxException e) {
                                log.debug("Error decoding URI: " + e.getMessage());
                            }
                        }
                        // if endpoint is pullpoint address
                        else {
                            if (version > 1) {
                                notification.clearoldMessage(productID);
                            }

                            notification.addMessage(subscriptionId, productID,
                                    productDoc.toString());
                        }
                    }
                }
            });
        } catch (Exception e) {
            // System.err.println("productChangeNotificationHandler calling publishWorkProduct Exception sending message: "
            // + e.getMessage());
            log.error("Exception handling product change notification message when publishing: "
                    + e.getMessage());
        }

        /*
        for (Notification notification : notificationDAO.findAll()) {
            for (NotificationSubscription sub : notification.getSubscriptions()) {
                if (subscriptionId.compareTo(sub.getSubscriptionID()) == 0) {
                    log.debug("adding workproduct: " + productID + " to subid: " + subscriptionId
                        + " to entity: " + notification.getEntityID());

                    productDoc.setWorkProduct(summary);

                    // if endpoint is web service url
                    if (notification.isEndpointWS()) {
                        log.debug("Notification going to be SENT to webServiceURL: "
                            + notification.getEndpointURL());
                        String url = notification.getEndpointURL();
                        try {
                            URI u = new URI(url);
                            String proto = u.getScheme();
                            log.debug("Handling protocol: " + proto);
                            if (proto.equalsIgnoreCase("xmpp")) {
                                try {
                                    log.debug("XMPP message sent to: " + u.getHost());
                                    communicationsService.sendXMPPMessage(
                                        getNotificationMessageBody(product), null,
                                        productDoc.xmlText(), u.getSchemeSpecificPart());
                                } catch (IllegalArgumentException e) {
                                    log.error("ERROR: " + e.getMessage());
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            } else {
                                // invoke specified webService
                                log.debug("WS-Notify message sent to :" + u.toString());
                                invokeWebServiceTemplate(notification, "WorkProductID",
                                    productDoc.toString());
                            }
                        } catch (URISyntaxException e) {
                            log.debug("Error decoding URI: " + e.getMessage());
                        }
                    }
                    // if endpoint is pullpoint address
                    else {
                        if (version > 1) {
                            notification.clearoldMessage(productID);
                        }

                        notification.addMessage(subscriptionId, productID, productDoc.toString());
                    }
                }
            }
        }
        
        */
    }

    private String getNotificationMessageBody(WorkProduct product) {

        StringBuffer body = new StringBuffer();
        body.append("UICDS ");
        body.append(product.getProductType());
        body.append(" work product");

        EventType event = null;
        if (product.getDigest() != null) {
            event = DigestHelper.getFirstEventWithActivityNameIdentifier(product.getDigest().getDigest());
        }
        if (event != null) {
            body.append(" associated with incident ");
            body.append(event.getIdentifierArray(0).getStringValue());
        }

        if (product.isActive()) {
            if (product.getProductVersion() == 1) {
                body.append(" created");
            } else {
                body.append(" updated ");
            }
        } else {
            body.append(" deleted ");
        }
        body.append(" by ");
        body.append(product.getUpdatedBy());
        body.append(" at ");
        body.append(product.getUpdatedDate().toString());

        return body.toString();
    }

    /**
     * Invoke web service template.
     *
     * @param notification the notification
     * @param msgType the msg type
     * @param message the message
     */
    private void invokeWebServiceTemplate(Notification notification, String msgType, String message) {

        // set url on webservice template
        webServiceTemplate.setDefaultUri(notification.getEndpointURL());

        // create the NotificationMessageRequest
        NotifyRequestDocument request = NotifyRequestDocument.Factory.newInstance();

        request.addNewNotifyRequest().addNewNotificationMessage().setMessage(
                createNotificationMessageHolder(msgType, message).getMessage());

        XmlCursor xc = request.getNotifyRequest().newCursor();
        xc.toNextToken();
        QName to = new QName("http://www.w3.org/2005/08/addressing", "To");
        xc.insertElementWithText(to, notification.getEntityID());
        xc.dispose();
        log.debug("NOTIFY_REQUEST: " + request.toString());

        // invoke webService
        webServiceTemplate.marshalSendAndReceive(request);
    }

    /**
     * Adds notification messages for the specified entity Id
     *
     * @param entityID the entity id
     * @param notifications the notifications
     * @ssdd
     */
    @Override
    public /* synchronized */ void notify(String entityID, NotificationMessageHolderType[] notifications) {

        // find Notification for desired entityID
        log.debug("Notify entityID: " + entityID);
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification != null) {
            log.debug("notifications to add: " + notifications.length);
            // for each notify message add it to the list of notification messages
            for (NotificationMessageHolderType notf : notifications) {
                NotificationMessageHolderType.Message msg = notf.getMessage();
                if (msg != null) {
                    addNotificationMessage(notification, 0, NOTIFY_MESSAGE, msg.toString());
                }

            }
        }
    }

    /**
     * Subscribes to messages of the specified topic type on the specified endpoint
     *
     * @param who the endpoint reference
     * @param what the filter type
     * @ssdd
     */
    @Override
    public void subscribe(EndpointReferenceType who, FilterType what)
            throws InvalidProductTypeException, NullSubscriberException, EmptySubscriberNameException,
            InvalidProductIDException {

        if (who.getAddress() != null) {

            String entityID = who.getAddress().getStringValue().substring(
                    who.getAddress().getStringValue().lastIndexOf("/") + 1);
            log.debug("subscribing who: " + entityID);

            // Process topic subscriptions - Allow TopicExpression elements from either
            // the ProfileService schema or the WS-Topics schema
            String topicExpression = FilterUtil.getTopic(what);
            log.debug("subscribing what: " + topicExpression);

            // get the namespace map for xpaths
            Map<String, String> namespaceMap = FilterUtil.getNamespaceMap(what);

            // get the xpath
            String xPath = FilterUtil.getXPath(what);
            log.debug("xPath: " + xPath);

            // Process xpath expressions

            StringTokenizer tokenizer = new StringTokenizer(topicExpression, "/");
            int topicTreeCount = tokenizer.countTokens();

            String productType = "";
            String productTypeValue = "";
            if (topicTreeCount > 0) {
                productType = tokenizer.nextToken();
                if (tokenizer.hasMoreElements() && topicTreeCount > 1) {
                    productTypeValue = tokenizer.nextToken();
                } else {
                    productTypeValue = "*";
                }

                // WorkProductID TopicExpression (workproduct/* or workproduct/1234)
                if (productType.equalsIgnoreCase("workproduct")) {
                    if (productTypeValue.equals("*")) {
                        log.debug("Subscribing to ALL WorkProduct updates.");
                        subscribeWorkProductID(productTypeValue, entityID);
                    } else {
                        log.debug("Subscribing to WorkProductByID: " + productTypeValue);
                        subscribeWorkProductID(String.valueOf(productTypeValue), entityID);
                    }
                }
                // AgreementID TopicExpression (agreement/* or agreement/1234)
                else if (productType.equalsIgnoreCase("agreement")) {
                    if (productTypeValue.equals("*")) {
                        log.debug("Subscribing to ALL Agreement updates.");
                        subscribeAgreement(productTypeValue, entityID);
                    } else {
                        log.debug("Subscribing to AgreementID: " + productTypeValue);
                        subscribeAgreement(productTypeValue, entityID);
                    }
                }
                // ProfileID TopicExpression (profile/* or profile/user@core1)
                else if (productType.equalsIgnoreCase("profile")) {
                    if (productTypeValue.equals("*")) {
                        log.debug("Subscribing to ALL Profile updates.");
                        subscribeProfile(productTypeValue, entityID);
                    } else {
                        log.debug("Subscribing to Profile Name: " + productTypeValue);
                        subscribeProfile(productTypeValue, entityID);
                    }
                }
                // IncidentID TopicExpression (incident/* or incident/12345)
                else if (productType.equalsIgnoreCase("Incident")) {
                    if (productTypeValue.equals("*")) {
                        log.debug("Subscribing to ALL Incident updates.");
                        subscribeIncidentIdAndWorkProductType(productType, productTypeValue, xPath,
                                namespaceMap, entityID);
                    } else {
                        log.debug("Subscribing to IncidentID: " + productTypeValue);
                        subscribeIncidentIdAndWorkProductType(productType, productTypeValue, xPath,
                                namespaceMap, entityID);
                    }
                }
                // WorkProductType TopicExpression (all others)
                else {
                    // if productType has incident qualifier (<productType>/incident/*)
                    if (productTypeValue.equalsIgnoreCase("Incident")) {
                        String incidentIdQualifier = "";
                        if (tokenizer.hasMoreTokens() && topicTreeCount > 2) {
                            incidentIdQualifier = tokenizer.nextToken();
                        } else {
                            incidentIdQualifier = "*";
                        }
                        log.debug("Subscribing to ProductType : " + productType
                                + " associated with IncidentID : " + incidentIdQualifier);
                        subscribeIncidentIdAndWorkProductType(productType, incidentIdQualifier,
                                xPath, namespaceMap, entityID);
                    }
                    // All other productTypes in from <productType>/*
                    else if (productTypeValue.equals("*")) {
                        log.debug("Subscribing ProductType: " + productType);
                        subscribeWorkProductType(productType, xPath, namespaceMap, entityID);
                    } else {
                        // the type could be something like application/pdf
                        String type = productType + "/" + productTypeValue;
                        log.debug("Subscribing ProductType: " + type);
                        subscribeWorkProductType(type, xPath, namespaceMap, entityID);
                    }
                }
            } else {
                throw new InvalidProductTypeException();
            }
        }
    }

    /**
     * Subscribes to notifications for the specified work product id.
     *
     * @param workProductID the work product id
     * @param entityID the entity id
     * @return the integer
     * @throws InvalidProductIDException the invalid product id exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public /* synchronized */ Integer subscribeWorkProductID(String workProductID, String entityID)
            throws InvalidProductIDException, NullSubscriberException, EmptySubscriberNameException {

        // Sub with PubSub
        Integer subscriptionID = getPubSubService().subscribeWorkProductID(workProductID, this);

        // create/update notification and persist
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification == null) {
            notification = new Notification();
            notification.setEntityID(entityID);
        }
        NotificationSubscription sub = new NotificationSubscription();
        sub.setSubscriptionID(subscriptionID);

        addNotificationSubscription(notification, sub);

        return subscriptionID;
    }

    /**
     * Subscribe to notifications for the specified work product type.
     *
     * @param wpType the wp type
     * @param xpContext the xp context
     * @param namespaceMap the namespace map
     * @param entityID the entity id
     * @return the integer
     * @throws InvalidProductTypeException the invalid product type exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public /* synchronized */ Integer subscribeWorkProductType(String wpType, String xpContext,
                                                               Map<String, String> namespaceMap, String entityID) throws InvalidProductTypeException,
            NullSubscriberException, EmptySubscriberNameException {

        Integer subID = getPubSubService().subscribeWorkProductType(wpType, xpContext,
                namespaceMap, this);

        // create/update notification and persist
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification == null) {
            notification = new Notification();
            notification.setEntityID(entityID);
        }
        NotificationSubscription sub = new NotificationSubscription();
        sub.setSubscriptionID(subID);

        addNotificationSubscription(notification, sub);

        return subID;
    }

    /**
     * Subscribes to notifications for the specified incident id and work product type.
     *
     * @param wpType the wp type
     * @param incidentID the incident id
     * @param xpContext the xp context
     * @param namespaceMap the namespace map
     * @param entityID the entity id
     * @return the integer
     * @throws InvalidProductTypeException the invalid product type exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    public /* synchronized */ Integer subscribeIncidentIdAndWorkProductType(String wpType,
                                                                            String incidentID, String xpContext, Map<String, String> namespaceMap, String entityID)
            throws InvalidProductTypeException, NullSubscriberException, EmptySubscriberNameException {

        Integer subID = getPubSubService().subscribeInterestGroupIdAndWorkProductType(wpType,
                incidentID, xpContext, namespaceMap, this);

        // create/update notification and persist
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification == null) {
            notification = new Notification();
            notification.setEntityID(entityID);
        }
        NotificationSubscription sub = new NotificationSubscription();
        sub.setSubscriptionID(subID);

        addNotificationSubscription(notification, sub);

        return subID;
    }

    /**
     * Subscribes to profile notifications.
     *
     * @param profileID the profile id
     * @param entityID the entity id
     * @throws InvalidProductIDException the invalid product id exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public /* synchronized */ void subscribeProfile(String profileID, String entityID)
            throws InvalidProductIDException, NullSubscriberException, EmptySubscriberNameException {

        // create/update notification and persist
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification == null) {
            notification = new Notification();
            notification.setEntityID(entityID);
        }

        Integer subscriptionID = new Random().nextInt();

        NotificationSubscription sub = new NotificationSubscription();
        sub.setSubscriptionID(subscriptionID);

        addNotificationSubscription(notification, sub);

        final String eeID = entityID;
        final Integer sID = subscriptionID;
        getPubSubService().addProfileListener(profileID,
                new NotificationListener<ProfileNotificationMessage>() {
                    public void onChange(ProfileNotificationMessage message) {

                        Notification notification = notificationDAO.findByEntityId(eeID);
                        if (notification != null) {
                            addNotificationMessage(notification, sID, ProfileNotificationMessage.NAME,
                                    message.toString());
                        }

                    }
                });
    }

    /**
     * Subscribes to agreements.
     *
     * @param agreementID the agreement id
     * @param entityID the entity id
     * @throws InvalidProductIDException the invalid product id exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public /* synchronized */ void subscribeAgreement(String agreementID, String entityID)
            throws InvalidProductIDException, NullSubscriberException, EmptySubscriberNameException {

        // create/update notification and persist
        Notification notification = notificationDAO.findByEntityId(entityID);
        if (notification == null) {
            notification = new Notification();
            notification.setEntityID(entityID);
        }

        Integer subscriptionID = new Random().nextInt();

        NotificationSubscription sub = new NotificationSubscription();
        sub.setSubscriptionID(subscriptionID);

        addNotificationSubscription(notification, sub);

        final String eeID = entityID;
        final Integer sID = subscriptionID;
        getPubSubService().addAgreementListener(agreementID,
                new NotificationListener<AgreementRosterMessage>() {
                    public void onChange(AgreementRosterMessage message) {

                        Notification notification = notificationDAO.findByEntityId(eeID);

                        addNotificationMessage(notification, sID, AgreementRosterMessage.NAME,
                                message.getAgreementID());
                    }
                });

    }

    /**
     * Gets the configuration service.
     *
     * @return the configuration service
     */
    @Override
    public ConfigurationService getConfigurationService() {

        return this.configurationService;
    }

    /**
     * Sets the configuration service.
     *
     * @param service the new configuration service
     */
    @Override
    public void setConfigurationService(ConfigurationService service) {

        configurationService = service;
    }

    /**
     * Gets the pub sub service.
     *
     * @return the pub sub service
     */
    @Override
    public PubSubService getPubSubService() {

        return this.pubSubService;
    }

    /**
     * Sets the pub sub service.
     *
     * @param service the new pub sub service
     */
    @Override
    public void setPubSubService(PubSubService service) {

        pubSubService = service;
    }

    /**
     * Notifies of a work product deletion.
     *
     * @param workProductID the work product id
     * @param workProductType the work product type
     * @param subscriptionId the subscription id
     * @ssdd
     */
    @Override
    public /* synchronized */ void workProductDeleted(
            ProductChangeNotificationMessage productChangedMessage, Integer subscriptionId) {

        // search all notifications and update any subscription msgs which have specified ID
        List<Notification> notifications = notificationDAO.findBySubscriptionId(subscriptionId);
        for (Notification notification : notifications) {
            WorkProductDeletedNotificationDocument doc = WorkProductDeletedNotificationDocument.Factory.newInstance();
            doc.addNewWorkProductDeletedNotification();
            doc.getWorkProductDeletedNotification().addNewWorkProductIdentification().set(
                    productChangedMessage.getIdentification().getWorkProductIdentification());
            doc.getWorkProductDeletedNotification().addNewWorkProductProperties().set(
                    productChangedMessage.getProperties().getWorkProductProperties());

            // if endpoint is web service url
            if (notification.isEndpointWS()) {
                log.debug("Notification going to be SENT to webServiceURL: "
                        + notification.getEndpointURL());
                // invoke specified webService
                invokeWebServiceTemplate(notification, "WorkProductDeleted", doc.toString());
            }
            // if endpoint is pullpoint address
            else {
                addNotificationMessage(notification, subscriptionId, "WorkProductDeleted",
                        doc.toString());
            }

        }

        /*
        List<Notification> notifications = notificationDAO.findAll();
        for (Notification notification : notifications) {
            Set<NotificationSubscription> subs = notification.getSubscriptions();
            for (NotificationSubscription sub : subs) {
                if (subscriptionId.compareTo(sub.getSubscriptionID()) == 0) {
                    log.debug("delete workproduct: " + productChangedMessage.getProductID()
                        + " to subid: " + subscriptionId + " to entity: "
                        + notification.getEntityID());

                    WorkProductDeletedNotificationDocument doc = WorkProductDeletedNotificationDocument.Factory.newInstance();
                    doc.addNewWorkProductDeletedNotification();
                    doc.getWorkProductDeletedNotification().addNewWorkProductIdentification().set(
                        productChangedMessage.getIdentification().getWorkProductIdentification());
                    doc.getWorkProductDeletedNotification().addNewWorkProductProperties().set(
                        productChangedMessage.getProperties().getWorkProductProperties());

                    // if endpoint is web service url
                    if (notification.isEndpointWS()) {
                        log.debug("Notification going to be SENT to webServiceURL: "
                            + notification.getEndpointURL());
                        // invoke specified webService
                        invokeWebServiceTemplate(notification, "WorkProductDeleted", doc.toString());
                    }
                    // if endpoint is pullpoint address
                    else {
                        addNotificationMessage(notification, subscriptionId, "WorkProductDeleted",
                            doc.toString());
                    }

                }
            }
        }
        */
    }

    /**
     * Clear notification messages.
     *
     * @ssdd
     */

    // @Transactional
    private /* synchronized */ void clearNotificationMessages(Notification notification) {

        notification.clearMessages();
        //notificationDAO.makePersistent(notification);

    }

    /**
     * Add a notification message.
     *
     * @param subscriptionID the subscription id
     * @param msgType the msg type
     * @param message the message
     * @ssdd
     */

    // @Transactional
    private /* synchronized */ void addNotificationMessage(Notification notification,
                                                           Integer subscriptionID, String msgType, String message) {

        notification.addMessage(subscriptionID, msgType, message);
        //notificationDAO.makePersistent(notification);

    }

    /**
     * Adds the subscription.
     *
     * @param subscription the subscription
     * @ssdd
     */
    // @Transactional
    private /* synchronized */ void addNotificationSubscription(Notification notification,
                                                                NotificationSubscription subscription) {

        notification.addSubscription(subscription);
        notificationDAO.makePersistent(notification);

    }

    // @Transactional
    private /* synchronized */ Notification makePersistent(Notification notification) {

        return notificationDAO.makePersistent(notification);
    }

    // @Transactional
    private /* synchronized */ void makeTransient(Notification notification) {

        notificationDAO.makeTransient(notification);
    }

    // FLi modified on 11/29/2011
    public int findMsgCountByEntityId(String entityId) {

        return notificationDAO.findMsgCountByEntityId(entityId);
    }
}
