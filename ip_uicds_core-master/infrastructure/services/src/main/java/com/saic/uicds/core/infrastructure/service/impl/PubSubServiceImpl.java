package com.saic.uicds.core.infrastructure.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hibernate.HibernateException;
import org.hibernate.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.saic.uicds.core.infrastructure.dao.ProductSubscriptionByIDDAO;
import com.saic.uicds.core.infrastructure.dao.ProductSubscriptionByTypeDAO;
import com.saic.uicds.core.infrastructure.exceptions.EmptySubscriberNameException;
import com.saic.uicds.core.infrastructure.exceptions.InvalidProductIDException;
import com.saic.uicds.core.infrastructure.exceptions.InvalidXpathException;
import com.saic.uicds.core.infrastructure.exceptions.NullSubscriberException;
import com.saic.uicds.core.infrastructure.listener.NotificationListener;
import com.saic.uicds.core.infrastructure.listener.NotificationListenerCollection;
import com.saic.uicds.core.infrastructure.messages.AgreementRosterMessage;
import com.saic.uicds.core.infrastructure.messages.ProductChangeNotificationMessage;
import com.saic.uicds.core.infrastructure.messages.ProfileNotificationMessage;
import com.saic.uicds.core.infrastructure.model.NamespaceMap;
import com.saic.uicds.core.infrastructure.model.ProductSubscriptionByID;
import com.saic.uicds.core.infrastructure.model.ProductSubscriptionByType;
import com.saic.uicds.core.infrastructure.service.PubSubNotificationService;
import com.saic.uicds.core.infrastructure.service.PubSubService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;

/**
 * The PubSubService implementation.
 * 
 * @author Aruna Hau
 * @since 1.0
 * @see com.saic.uicds.core.infrastructure.model.ProductSubscriptionByID ProductSubscriptionByID
 *      Data Model
 * @see com.saic.uicds.core.infrastructure.model.ProductSubscriptionByType ProductSubscriptionByType
 *      Data Model
 * @ssdd
 * 
 */
public class PubSubServiceImpl
    implements PubSubService {

    Logger log = LoggerFactory.getLogger(PubSubServiceImpl.class);

    private ProductSubscriptionByIDDAO productSubscriptionByIDDAO;

    // map of currentMessages keyed by topicName
    private Map<String, String> mapCurrentMessages = new HashMap<String, String>();

    public void setProductSubscriptionByIDDAO(ProductSubscriptionByIDDAO dao) {

        productSubscriptionByIDDAO = dao;
    }

    private ProductSubscriptionByTypeDAO productSubscriptionByTypeDAO;

    public void setProductSubscriptionByTypeDAO(ProductSubscriptionByTypeDAO dao) {

        productSubscriptionByTypeDAO = dao;
    }

    // String is Profile.getRef().getId() ala profileID
    private HashMap<String, NotificationListenerCollection<ProfileNotificationMessage>> profileListeners = new HashMap<String, NotificationListenerCollection<ProfileNotificationMessage>>();

    // String is ??? TODO:: Need to find a way to point to specific agreements
    private HashMap<String, NotificationListenerCollection<AgreementRosterMessage>> agreementListeners = new HashMap<String, NotificationListenerCollection<AgreementRosterMessage>>();

    private HashMap<String, PubSubNotificationService> subscriberMap = new HashMap<String, PubSubNotificationService>();

    Random randomGenerator = new Random();

    private WorkProductService workProductService;

    public void setWorkProductService(WorkProductService ws) {

        workProductService = ws;
    }

    @PostConstruct
    public void init() {

        if (log.isDebugEnabled()) {
            log.debug("PubSubServiceImpl - init() starts");
        }
    }

    /**
     * Subscribe to a specific work product.
     * 
     * @param workProductID the work product id
     * @param subscriber the subscriber
     * 
     * @return the integer
     * 
     * @throws InvalidProductIDException the invalid product id exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public Integer subscribeWorkProductID(String workProductID, PubSubNotificationService subscriber)
        throws InvalidProductIDException, NullSubscriberException, EmptySubscriberNameException {

        return subscribeProductID(workProductID, subscriber, false);
    }

    /**
     * Subscribe to new work product version.
     * 
     * @param workProductID the work product id
     * @param subscriber the subscriber
     * 
     * @return the integer
     * 
     * @throws InvalidProductIDException the invalid product id exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public Integer subscribeWorkProductIDNewVersions(String workProductID,
        PubSubNotificationService subscriber) throws InvalidProductIDException,
        NullSubscriberException, EmptySubscriberNameException {

        return subscribeProductID(workProductID, subscriber, true);
    }

    private Integer subscribeProductID(String workProductID, PubSubNotificationService subscriber,
        boolean newVersionsOnly) throws InvalidProductIDException, NullSubscriberException,
        EmptySubscriberNameException {

        if (log.isDebugEnabled()) {
            log.debug("subscribeWorkProductID: prodID=" + workProductID);
        }

        if (subscriber == null) {
            throw new NullSubscriberException();
        }

        if (subscriber.getServiceName().toString().isEmpty()) {
            throw new EmptySubscriberNameException();
        }

        // Make sure a work product with the specified ID has been published by the Product Service.
        if (!workProductService.isExisted(workProductID)) {
            throw new InvalidProductIDException();
        }

        Integer subscriptionId = randomGenerator.nextInt();

        // Create and persist the new subscription
        ProductSubscriptionByID subscription = new ProductSubscriptionByID(workProductID,
            subscriber.getServiceName(), subscriptionId);
        productSubscriptionByIDDAO.makePersistent(subscription);

        // Add the subscriber-service info to the subscriber map
        if (log.isDebugEnabled()) {
            log.debug("===> subscribeWorkProductID: adding interface " + subscriber
                + "  for subscriber " + subscriber.getServiceName() + " to map!");
        }

        subscriberMap.put(subscriber.getServiceName(), subscriber);

        // Notify subscriber of the product
        if (!newVersionsOnly) {
            try {
                subscriber.newWorkProductVersion(workProductID, subscriptionId);
            } catch (HibernateException e) {
                log.error("subscribeProductID HibernateException notifying at subscription: "
                    + e.getMessage());
                log.error("Exception type:" + e.getClass().getName());
                log.error(ExceptionUtils.getFullStackTrace(e));
            } catch (Exception e) {
                log.error("subscribeProductID notifying at subscription: " + e.getMessage());
                log.error("Exception type:" + e.getClass().getName());
                log.error(ExceptionUtils.getFullStackTrace(e));
            } catch (Throwable e) {
                log.error("subscribeProductID notifying at subscription: " + e.getMessage());
            }
        }

        return subscriptionId;
    }

    /**
     * Subscribe to work products of the specified type.
     * 
     * @param workProductType the work product type
     * @param xPath the x path
     * @param namespaceMap the namespace map
     * @param subscriber the subscriber
     * 
     * @return the integer
     * 
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public Integer subscribeWorkProductType(String workProductType, String xPath,
        Map<String, String> namespaceMap, PubSubNotificationService subscriber)
        throws NullSubscriberException, EmptySubscriberNameException {

        return subscribeInterestGroupIdAndWorkProductType(workProductType, null, xPath,
            namespaceMap, subscriber);
    }

    /**
     * Subscribe to work products of a specific type and interest group.
     * 
     * @param workProductType the work product type
     * @param interestGroupID the interest group id
     * @param xPath the x path
     * @param namespaceMap the namespace map
     * @param subscriber the subscriber
     * 
     * @return the integer
     * 
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public Integer subscribeInterestGroupIdAndWorkProductType(String workProductType,
        String interestGroupID, String xPath, Map<String, String> namespaceMap,
        PubSubNotificationService subscriber) throws NullSubscriberException,
        EmptySubscriberNameException {

        if (log.isDebugEnabled()) {
            log.debug("subscribeInterestGroupIdAndWorkProductType: prodType=" + workProductType
                + " for subscriber:" + subscriber.getServiceName() + " interface:" + subscriber);
            if (namespaceMap != null) {
                Set<String> keys = namespaceMap.keySet();
                for (String key : keys) {
                    log.debug("==> prefix=" + key + " namespace=" + namespaceMap.get(key));
                }
            }
        }

        if (subscriber == null) {
            throw new NullSubscriberException();
        }

        if (subscriber.getServiceName().toString().isEmpty()) {
            throw new EmptySubscriberNameException();
        }

        // persist the productSubscriptionByType
        Integer subscriptionId = persistProductSubscriptionByType(workProductType, interestGroupID,
            xPath, namespaceMap, subscriber);

        // Add the subscriber-service info to the subscriber map - overwite if one exists
        if (log.isDebugEnabled()) {
            log.debug("===> subscribeInterestGroupIdAndWorkProductType: adding interface for subscriber "
                + subscriber.getServiceName() + " interface" + subscriber + " to map!");
        }
        subscriberMap.put(subscriber.getServiceName(), subscriber);

        // Get IDs of published products with the specified product type and notify the subscriber
        // TBD: Need to catch exception InvalidXPathException and notify subscriber
        // RDW (28 Sept 2011) - Commenting out the following section because once I fixed the check
        // on the xpath (by adding !xPath.isEmpty() the behavior of the first GetMessages would
        // change
        // to deliver notifications like GetMatchingMessages returns. Also with about 100 work
        // products
        // on uicds-test4.saic.com it took about 40 seconds to do all 100 newWorkProduct callbacks.
        // Also commented this out because IncidentManagementService also subscribes (not just
        // resource instances so I didn't want it getting these notifications either.
        /*
        List<String> prodoctIDList;
        try {
            if (xPath != null && !xPath.isEmpty() && namespaceMap != null) {
                if (log.isDebugEnabled()) {
                    log.debug("calling getProductIDListByTypeAndXQuery() with xPath=" + xPath
                        + " nameSpaceMap:");
                    Set<String> keys = namespaceMap.keySet();
                    for (String key : keys) {
                        log.debug("==> prefix=" + key + " namespace=" + namespaceMap.get(key));
                    }
                }
            }

            prodoctIDList = workProductService.getProductIDListByTypeAndXQuery(workProductType,
                xPath, namespaceMap);

            for (String productID : prodoctIDList) {
                // log.info("Notifying of product: " + productID);
                subscriber.newWorkProductVersion(productID, subscriptionId);
            }
        } catch (InvalidXpathException e) {

            e.printStackTrace();
        }
        */

        // log.info("DONE");
        return subscriptionId;
    }

    /**
     * Unsubscribe - remove a work product subscription
     * 
     * @param subscriptionID the subscription id
     * @ssdd
     */
    @Override
    public void unsubscribeBySubscriptionID(Integer subscriptionID) {

        if (log.isDebugEnabled()) {
            log.debug("unsubscribeWorkProduct: subID=" + subscriptionID);
        }

        // Find and delete the product subscriptions by ID
        List<ProductSubscriptionByID> subscriptionsByID = productSubscriptionByIDDAO.findBySubscriptionId(subscriptionID);
        for (ProductSubscriptionByID subscription : subscriptionsByID) {
            // remove subscription from database
            if (log.isDebugEnabled()) {
                log.debug("unsubscribeWorkProduct: remove subscriptionbyID subID: "
                    + subscriptionID + " from database");
            }
            productSubscriptionByIDDAO.makeTransient(subscription);

            // Don't remove subscriber name/interface from map since it may have active
            // subscriptions.

        }

        // Find and delete the product subscriptions by Types
        List<ProductSubscriptionByType> subscriptionsByType = productSubscriptionByTypeDAO.findBySubscriptionId(subscriptionID);
        for (ProductSubscriptionByType subscription : subscriptionsByType) {
            // remove subscription from database
            if (log.isDebugEnabled()) {
                log.debug("unsubscribeWorkProduct: remove subscriptionbyType subID: "
                    + subscriptionID + " from database");
            }
            productSubscriptionByTypeDAO.makeTransient(subscription);

            // Don't remove subscriber name/interface from map since it may have active
            // subscriptions.

        }

    }

    public List<Integer> getSubscriptionsByServiceName(String serviceName) {

        List<Integer> list = new ArrayList<Integer>();

        log.debug("getSubscriptionsByServiceName. serviceName="+serviceName);
        // Find and delete the product subscriptions by ID
        List<ProductSubscriptionByID> subscriptionsByID = productSubscriptionByIDDAO.findBySubscriberName(serviceName);
        log.debug("subscriptionsByID. Count:"+subscriptionsByID.size());
        for (ProductSubscriptionByID subscription : subscriptionsByID) {
            list.add(subscription.getSubscriptionId());
        }

        log.debug("productSubscriptionByType");
        // Find and delete the product subscriptions by Types
        List<ProductSubscriptionByType> subscriptionsByType = productSubscriptionByTypeDAO.findBySubscriberName(serviceName);
        log.debug("size:"+subscriptionsByType.size());
        
        for (ProductSubscriptionByType subscription : subscriptionsByType) {
            list.add(subscription.getSubscriptionId());
        }

        return list;
    }

    /**
     * Adds the profile listener.
     * 
     * @param profileID the profile id
     * @param listener the listener
     * 
     * @throws InvalidProductIDException the invalid product id exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public void addProfileListener(String profileID,
        NotificationListener<ProfileNotificationMessage> listener)
        throws InvalidProductIDException, NullSubscriberException, EmptySubscriberNameException {

        NotificationListenerCollection<ProfileNotificationMessage> list = profileListeners.get(profileID);
        if (list == null) {
            list = new NotificationListenerCollection<ProfileNotificationMessage>();
            list.add(listener);
            profileListeners.put(profileID, list);
        } else {
            list.add(listener);
        }
    }

    /**
     * Profile notification handler.
     * 
     * @param message the message
     * @ssdd
     */
    @Override
    public void profileNotificationHandler(ProfileNotificationMessage message) {

        String profileID = message.getUserID();
        // Get list of listeners listening for changes on this specific profileID
        NotificationListenerCollection<ProfileNotificationMessage> profileIdListeners = profileListeners.get(profileID);
        if (profileIdListeners != null) {
            profileIdListeners.fireChangeEvent(message);
        }
        // Also Get list of listeners listening for changes on ANY profile
        NotificationListenerCollection<ProfileNotificationMessage> anyProfileListerners = profileListeners.get("*");
        if (anyProfileListerners != null) {
            anyProfileListerners.fireChangeEvent(message);
        }

        // add new profileNotificationMessage to last current message for profile topics
        mapCurrentMessages.put("profile/*", profileID);
        mapCurrentMessages.put(new String("profile/" + profileID).toLowerCase(), profileID);

    }

    /**
     * Adds the agreement listener.
     * 
     * @param agreementID the agreement id
     * @param listener the listener
     * 
     * @throws InvalidProductIDException the invalid product id exception
     * @throws NullSubscriberException the null subscriber exception
     * @throws EmptySubscriberNameException the empty subscriber name exception
     * @ssdd
     */
    @Override
    public void addAgreementListener(String agreementID,
        NotificationListener<AgreementRosterMessage> listener) throws InvalidProductIDException,
        NullSubscriberException, EmptySubscriberNameException {

        NotificationListenerCollection<AgreementRosterMessage> list = agreementListeners.get(agreementID);
        if (list == null) {
            list = new NotificationListenerCollection<AgreementRosterMessage>();
            list.add(listener);
            agreementListeners.put(agreementID, list);
        } else {
            list.add(listener);
        }

    }

    /**
     * Agreement notification handler.
     * 
     * @param message the message
     * @ssdd
     */
    @Override
    public void agreementNotificationHandler(AgreementRosterMessage message) {

        String agreementID = message.getAgreementID();

        // Get list of listeners listening for changes on this specific profileID
        NotificationListenerCollection<AgreementRosterMessage> agreementIdListeners = agreementListeners.get(agreementID);
        if (agreementIdListeners != null) {
            agreementIdListeners.fireChangeEvent(message);
        }
        // Also Get list of listeners listening for changes on ANY agreement
        NotificationListenerCollection<AgreementRosterMessage> anyAgreementListerners = agreementListeners.get("*");
        if (anyAgreementListerners != null) {
            anyAgreementListerners.fireChangeEvent(message);
        }

        // add new agreementNotificationMessage to last current message for agreement topics
        mapCurrentMessages.put("agreement/*", agreementID);
        mapCurrentMessages.put(new String("agreement/" + agreementID).toLowerCase(), agreementID);

    }

    /**
     * Product change notification handler. Receives messages on the
     * productChangeNotificationHandler Spring Integration message channel from the Work Product
     * Service to indicate a change in a work product.
     * 
     * @param message Work product change notification message (ProductChangeNotificationMessage)
     * @ssdd
     */
    public void productChangeNotificationHandler(ProductChangeNotificationMessage message) {

        log.debug("Receive productChangeNotification for wp ID: " + message.getProductID()
            + " changeIndicator=" + message.getChangeIndicator());

        if (message.getChangeIndicator() == ProductChangeNotificationMessage.ChangeIndicator.Publish) {

            try {
                publishWorkProduct(message);
            } catch (Exception e) {
                // System.err.println("productChangeNotificationHandler calling publishWorkProduct Exception sending message: "
                // + e.getMessage());
                log.error("Exception sending message: " + e.getMessage());
            }

            // pass to newCurrentMessage
            try {
                newPublishedMessage(message);
            } catch (Exception e) {
                // System.err.println("productChangeNotificationHandler calling newPublishedMessage Exception sending message: "
                // + e.getMessage());
                log.error("Exception sending message: " + e.getMessage());
            }
        } else if (message.getChangeIndicator() == ProductChangeNotificationMessage.ChangeIndicator.Delete) {
            deleteWorkProduct(message);
        } else {
            log.error("productChangeNotificationHandler: unkown change indicator ind="
                + message.getChangeIndicator());
        }
    }

    /**
     * Publish work product. Publish the specific work product to subscribers. Publish the work
     * products for the specified type to subscribers if it passes interest group constraints.
     * 
     * @param workProductID the work product id
     * @param workProductType the work product type
     * @param interestGroupID the interest group id
     * @ssdd
     */
    @Override
    public void publishWorkProduct(ProductChangeNotificationMessage message) {

        log.debug("publishWorkProduct: prodID=" + message.getProductID() + " prodType="
            + message.getType() + " interestGroupID=" + message.getInterestGroupID());

        // Get a list of subscriptions by ID from the DAO and notify all subscribers
        List<ProductSubscriptionByID> subscriptionsByID = productSubscriptionByIDDAO.findByProductID(message.getProductID());

        if (log.isDebugEnabled()) {
            log.debug("publishWorkProduct: looking for subscriptionById - found "
                + subscriptionsByID.size());
        }

        for (ProductSubscriptionByID subscription : subscriptionsByID) {

            if (log.isDebugEnabled()) {
                log.debug("publishWorkProduct: getting subscriptionByID data for subscriber: "
                    + subscription.getSubscriberName());
            }

            PubSubNotificationService service = subscriberMap.get(subscription.getSubscriberName());
            // shouldn't be null here since it was checked when we added it to the map
            // but never hurts to check first before using it
            if (service != null) {
                log.debug("publishWorkProduct: notifying by ID subscriber "
                    + service.getServiceName() + " interface=" + service + "prodId="
                    + message.getProductID() + " subID=" + subscription.getSubscriptionId());
                try {
                    service.newWorkProductVersion(message.getProductID(),
                        subscription.getSubscriptionId());
                } catch (HibernateException e) {
                    log.error("publishWorkProduct HibernateException notifying of delete: "
                        + e.getMessage());
                    log.error("Exception type:" + e.getClass().getName());
                    log.error(ExceptionUtils.getFullStackTrace(e));
                } catch (Exception e) {
                    log.error("publishWorkProductException notifying of delete: " + e.getMessage());
                    log.error("Exception type:" + e.getClass().getName());
                    log.error(ExceptionUtils.getFullStackTrace(e));
                } catch (Throwable e) {
                    log.error("publishWorkProductThrowable notifying of delete: " + e.getMessage());
                }

            }
        }

        // Get a list of subscriptions by type from the DAO and notify all subscribers
        List<ProductSubscriptionByType> subscriptionsByType = productSubscriptionByTypeDAO.findByProductType(message.getType());

        if (log.isDebugEnabled()) {
            log.debug("publishWorkProduct: looking for subscriptionByType - found "
                + subscriptionsByType.size());
        }

        for (ProductSubscriptionByType subscription : subscriptionsByType) {

            // first check to see if subscription has any incident constraints
            // if incident compliance does not pass...do not publish
            if (isInterestGroupCompliant(subscription.getInterestGroupID(),
                message.getInterestGroupID())) {

                if (log.isDebugEnabled()) {
                    log.debug("publishWorkProduct: getting subscriptionByType data for subscriber: "
                        + subscription.getSubscriberName());
                }

                PubSubNotificationService service = subscriberMap.get(subscription.getSubscriberName());

                log.debug("serviceName=" + subscription.getSubscriberName() + " interface="
                    + service);

                // shouldn't be null here since it was checked when we added it to the map
                // but never hurts to check first before using it
                if (service != null) {
                    // TBD: send only if xPath is empty or xPath execute successfully on the work
                    // product
                    // if the xPath fails execution, send InvalidXPathNotification to subscriber

                    if (subscription.getXPath() == null || subscription.getXPath().isEmpty()
                        || subscription.getNamespacemap() == null) {
                        log.debug("publishWorkProduct: notifying by Type subscriber "
                            + service.getServiceName() + " interface=" + service.getServiceName()
                            + " prodId=" + message.getProductID() + " subID="
                            + subscription.getSubscriptionId());

                        try {
                            service.newWorkProductVersion(message.getProductID(),
                                subscription.getSubscriptionId());
                        } catch (HibernateException e) {
                            log.error("publishWorkProduct HibernateException notifying of delete: "
                                + e.getMessage());
                            log.error("Exception type:" + e.getClass().getName());
                            log.error(ExceptionUtils.getFullStackTrace(e));
                        } catch (Exception e) {
                            log.error("publishWorkProductException notifying of delete: "
                                + e.getMessage());
                            log.error("Exception type:" + e.getClass().getName());
                            log.error(ExceptionUtils.getFullStackTrace(e));
                        } catch (Throwable e) {
                            log.error("publishWorkProductThrowable notifying of delete: "
                                + e.getMessage());
                        }
                    } else {
                        try {
                            if (workProductService.xPathExecuted(message.getProductID(),
                                subscription.getXPath(), subscription.getNamespaceMap())) {
                                log.debug("===****===> publishWorkProduct: notifying by Type subscriber "
                                    + service.getServiceName()
                                    + " interface="
                                    + service
                                    + " prodId="
                                    + message.getProductID()
                                    + " subID="
                                    + subscription.getSubscriptionId());
                                try {
                                    service.newWorkProductVersion(message.getProductID(),
                                        subscription.getSubscriptionId());
                                } catch (HibernateException e) {
                                    log.error("publishWorkProduct HibernateException notifying of delete: "
                                        + e.getMessage());
                                    log.error("Exception type:" + e.getClass().getName());
                                    log.error(ExceptionUtils.getFullStackTrace(e));
                                } catch (Exception e) {
                                    log.error("publishWorkProductException notifying of delete: "
                                        + e.getMessage());
                                    log.error("Exception type:" + e.getClass().getName());
                                    log.error(ExceptionUtils.getFullStackTrace(e));
                                } catch (Throwable e) {
                                    log.error("publishWorkProductThrowable notifying of delete: "
                                        + e.getMessage());
                                }
                            }
                        } catch (InvalidXpathException e) {
                            // TODO: this needs to go back to the subscriber through some async
                            // channel
                            log.error("Invalid XPATH: " + subscription.getXPath());
                            service.InvalidXpathNotification(subscription.getSubscriptionId(),
                                e.getMessage());
                        }
                    }
                }
            }
        }

    }

    /**
     * Delete a specific work product and work products by type.
     * 
     * @param workProductID the work product id
     * @param workProductType the work product type
     * @ssdd
     */
    @Override
    public void deleteWorkProduct(ProductChangeNotificationMessage changedMessage) {

        log.debug("deleteWorkProduct: prodID=" + changedMessage.getProductID() + " prodType="
            + changedMessage.getType());

        // Get a list of subscriptions by ID from the DAO and notify all subscribers
        List<ProductSubscriptionByID> subscriptionsByID = productSubscriptionByIDDAO.findByProductID(changedMessage.getProductID());

        if (log.isDebugEnabled()) {
            log.debug("deleteWorkProduct: looking for subscriptionById - found "
                + subscriptionsByID.size());
        }

        for (ProductSubscriptionByID subscription : subscriptionsByID) {

            if (log.isDebugEnabled()) {
                log.debug("deleteWorkProduct: getting subscriptionByID data for subscriber: "
                    + subscription.getSubscriberName());
            }

            PubSubNotificationService service = subscriberMap.get(subscription.getSubscriberName());
            // shouldn't be null here since it was checked when we added it to the map
            // but never hurts to check first before using it
            if (service != null) {
                log.debug("deleteWorkProduct: notifying by ID subscriber "
                    + service.getServiceName() + " interface=" + service + "prodId="
                    + changedMessage.getProductID() + " subID=" + subscription.getSubscriptionId());
                service.workProductDeleted(changedMessage, subscription.getSubscriptionId());
            }

            // delete the subscription by this service (only subscription by ID only)
            productSubscriptionByIDDAO.makeTransient(subscription);
        }

        // Get a list of subscriptions by type from the DAO and notify all subscribers
        List<ProductSubscriptionByType> subscriptionsByType = productSubscriptionByTypeDAO.findByProductType(changedMessage.getType());

        if (log.isDebugEnabled()) {
            log.debug("deleteWorkProduct: looking for subscriptionByType - found "
                + subscriptionsByType.size());
        }

        for (ProductSubscriptionByType subscription : subscriptionsByType) {

            if (log.isDebugEnabled()) {
                log.debug("deleteWorkProduct: getting subscriptionByType data for subscriber: "
                    + subscription.getSubscriberName());
            }

            PubSubNotificationService service = subscriberMap.get(subscription.getSubscriberName());

            log.debug("serviceName=" + subscription.getSubscriberName() + " interface=" + service);

            // shouldn't be null here since it was checked when we added it to the map
            // but never hurts to check first before using it
            if (service != null) {
                // TBD: send only if xPath is empty or xPath execute successfully on the work
                // product
                // if the xPath fails execution, send InvalidXPathNotification to subscriber

                if (subscription.getXPath() == null || subscription.getXPath().isEmpty()
                    || subscription.getNamespaceMap() == null) {
                    log.debug("deleteWorkProduct: notifying by Type subscriber "
                        + service.getServiceName() + " interface=" + service + " prodId="
                        + changedMessage.getProductID() + " subID="
                        + subscription.getSubscriptionId());

                    try {
                        service.workProductDeleted(changedMessage, subscription.getSubscriptionId());
                    } catch (HibernateException e) {
                        log.error("deleteWorkProduct HibernateException notifying of delete: "
                            + e.getMessage());
                        log.error("Exception type:" + e.getClass().getName());
                        log.error(ExceptionUtils.getFullStackTrace(e));
                    } catch (Exception e) {
                        log.error("deleteWorkProduct Exception notifying of delete: "
                            + e.getMessage());
                        log.error("Exception type:" + e.getClass().getName());
                        log.error(ExceptionUtils.getFullStackTrace(e));
                    } catch (Throwable e) {
                        log.error("deleteWorkProduct Throwable notifying of delete: "
                            + e.getMessage());
                    }
                } else {
                    try {
                        if (workProductService.xPathExecuted(changedMessage.getProductID(),
                            subscription.getXPath(), subscription.getNamespaceMap())) {
                            log.debug("===****===> deleteWorkProduct: notifying by Type subscriber "
                                + service.getServiceName()
                                + " interface="
                                + service
                                + " prodId="
                                + changedMessage.getProductID()
                                + " subID="
                                + subscription.getSubscriptionId());
                            try {
                                service.workProductDeleted(changedMessage,
                                    subscription.getSubscriptionId());
                            } catch (HibernateException e) {
                                log.error("deleteWorkProduct HibernateException notifying of delete: "
                                    + e.getMessage());
                                log.error("Exception type:" + e.getClass().getName());
                                log.error(ExceptionUtils.getFullStackTrace(e));
                            } catch (Exception e) {
                                log.error("deleteWorkProduct Exception notifying of delete: "
                                    + e.getMessage());
                                log.error("Exception type:" + e.getClass().getName());
                                log.error(ExceptionUtils.getFullStackTrace(e));
                            } catch (Throwable e) {
                                log.error("deleteWorkProduct Throwable notifying of delete: "
                                    + e.getMessage());
                            }
                        }
                    } catch (InvalidXpathException e) {
                        // TODO: this needs to go back to the subscriber through some async
                        // channel
                        log.error("Invalid XPATH: " + subscription.getXPath());
                        service.InvalidXpathNotification(subscription.getSubscriptionId(),
                            e.getMessage());
                    }
                }
            }
        }
    }

    public void subscriberInterface(PubSubNotificationService subscriber) {

        if (log.isDebugEnabled()) {
            log.debug("===> subscriberInterface: adding interface for subscriber "
                + subscriber.getServiceName() + " interface" + subscriber + " to map!");
        }
        subscriberMap.put(subscriber.getServiceName(), subscriber);
    }

    // Check to see if the subscription has an incident qualifier. if so, check to ensure
    // wProduct is associated with incident and its incidentID matches the subscription's qualifier
    // If no subscription qualifier is present simply exit and return true
    private boolean isInterestGroupCompliant(String subscriptionInterestGroupID,
        String wpInterestGroupID) {

        boolean bValid = false;

        // if subscription has incident qualifier and wp is associated to an incident
        if (subscriptionInterestGroupID != null && wpInterestGroupID != null) {
            // if subscription is interested in ANY workProducts associated with incidents
            // return true b/c we know workProductInterestGroupID != null
            if (subscriptionInterestGroupID.equals("*")) {
                bValid = true;
            } else {
                // if subscription's incidentID qualifier matches the incidentID of the incident
                // associated with the workProudct return true
                if (subscriptionInterestGroupID.equalsIgnoreCase(wpInterestGroupID)) {
                    bValid = true;
                } else {
                    log.debug("isInterestGroupCompliant: Failed-  workProduct is not associated with incidentID: "
                        + subscriptionInterestGroupID);
                }
            }
        }
        // if there is NO subscription incident qualifer, skip this and return true
        else if (subscriptionInterestGroupID == null) {
            bValid = true;
        }
        // if subscription has incidentID qualifier but wpType is not associated with any incident
        else if (wpInterestGroupID == null && subscriptionInterestGroupID != null) {
            log.debug("isIncidentCompliant: Failed- workProduct is not associated with ANY incidents");
        }
        return bValid;
    }

    /**
     * Get current/last message based on topicName
     * 
     * @param topicName
     * @return message last message received on topic
     * 
     * @return the last published message
     * @ssdd
     */
    public String getLastPublishedMessage(String topicName) {

        return mapCurrentMessages.get(topicName.toLowerCase());
    }

    /**
     * Add incoming message as "last published message" for a particular topicName. Used by
     * notificationService getCurrentMessage(topic)
     * 
     * @param message to be stored
     * @param wpType work Product type of the product that was changed
     * @param productID id of the product type being updated, null if none
     * @param incidentID id of the incident this product is associated with, null if none
     */
    private void newPublishedMessage(ProductChangeNotificationMessage message) {

        try {
            String topicName = null;
            // determine the topicName this message.getProductID() will be published into
            if (message.getType() != null) {
                // add message.getProductID() to base topicName if message.getType()!=null (i.e
                // alert/*)
                log.debug("newCurrentMessage: " + message.getType() + "/*" + ", "
                    + message.getProductID());
                mapCurrentMessages.put(new String(message.getType() + "/*").toLowerCase(),
                    message.getProductID());

                if (message.getType().equalsIgnoreCase("workproduct"))
                    topicName = message.getType() + "/" + message.getProductID();
                else if (message.getType().equalsIgnoreCase("incident"))
                    topicName = message.getType() + "/" + message.getInterestGroupID();
                else if (message.getInterestGroupID() != null) {
                    // add message.getProductID() to incident/* subtopic if incident (i.e.
                    // <message.getType()>/incident/*)
                    log.debug("newCurrentMessage: " + message.getType() + "/*" + ", "
                        + message.getProductID());
                    mapCurrentMessages.put(
                        new String(message.getType() + "/incident/*").toLowerCase(),
                        message.getProductID());
                    topicName = message.getType() + "/incident/" + message.getInterestGroupID();
                }
            }
            // store each new current message.getProductID() by topic name
            if (topicName != null) {
                log.debug("newCurrentMessage: " + topicName + ", " + message.getProductID());
                mapCurrentMessages.put(topicName.toLowerCase(), message.getProductID());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    // if the subscription existed, delete the old entry then persist the new one
    private final Integer persistProductSubscriptionByType(String workProductType,
        String interestGroupID, String xPath, Map<String, String> namespaceMap,
        PubSubNotificationService subscriber) {

        // Create and persist the new subscription
        Integer subscriptionId = randomGenerator.nextInt();

        HashSet<NamespaceMap> namespaces = new HashSet<NamespaceMap>();
        for (String prefix : namespaceMap.keySet()) {
            NamespaceMap nsitem = new NamespaceMap();
            nsitem.setPrefix(prefix);
            nsitem.setUri(namespaceMap.get(prefix));
        }

        ProductSubscriptionByType subscription = new ProductSubscriptionByType(workProductType,
            interestGroupID, xPath, subscriber.getServiceName(), subscriptionId, namespaces);
        log.debug("persistProductSubscriptionByType: PERSIST: Type: " + workProductType
            + ", SubscriptionId: " + subscriptionId + ", Subscriber: "
            + subscriber.getServiceName());
        productSubscriptionByTypeDAO.makePersistent(subscription);

        return subscriptionId;
    }
}
