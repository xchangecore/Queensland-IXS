package com.saic.uicds.core.infrastructure.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.LockMode;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

import com.saic.uicds.core.infrastructure.dao.hb.NotificationMessageDAOHibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Notification data model.
 *
 * @ssdd
 */
@Entity
@Table(name = "NOTIFICATION")
public class Notification
        implements Serializable {

    private static final long serialVersionUID = -7000808870449438458L;
    public static final String NOTIFICATION_PRODUCT_TYPE = "Notification";

    @Transient
    private static Logger LOG = LoggerFactory.getLogger(Notification.class);

    @Id
    @Column(name = "NOTIFICATION_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ENTITY_ID")
    @Field(index = Index.TOKENIZED)
    private String entityID;

    @Column(name = "ENDPOINT_URL")
    @Field(index = Index.TOKENIZED)
    private String endpointURL;

    @Column(name = "ENDPOINT_WS")
    @Field(index = Index.TOKENIZED)
    private boolean endpointWS; // is the endpoint a webserviceURL

    @OneToMany(cascade = {CascadeType.ALL}, fetch = FetchType.EAGER, mappedBy = "notification")
    @org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private Set<NotificationSubscription> subscriptions = new HashSet<NotificationSubscription>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "notification")
    //@org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private Set<NotificationMessage> messages = new LinkedHashSet<NotificationMessage>();

    //FLi modified on 11/29/2011
    private int msgCount;

    public boolean equals(Object obj) {

        Notification notificationObj = (Notification) obj;
        return (entityID.equals(notificationObj.getEntityID()));
    }

    public int hashCode() {

        return entityID.hashCode();
    }

    /**
     * Gets the id.
     *
     * @return the id
     * @ssdd
     */
    public Integer getId() {

        return id;
    }

    /**
     * Sets the id.
     *
     * @param id
     *            the new id
     * @ssdd
     */
    public void setId(Integer id) {

        this.id = id;
    }

    /**
     * Checks if is endpoint ws.
     *
     * @return true, if is endpoint ws
     * @ssdd
     */
    public boolean isEndpointWS() {

        return endpointWS;
    }

    /**
     * Sets the endpoint ws.
     *
     * @param endpointWS
     *            the new endpoint ws
     * @ssdd
     */
    public void setEndpointWS(boolean endpointWS) {

        this.endpointWS = endpointWS;
    }

    /**
     * Gets the entity id.
     *
     * @return the entity id
     * @ssdd
     */
    public String getEntityID() {

        return entityID;
    }

    /**
     * Sets the entity id.
     *
     * @param entityID
     *            the new entity id
     * @ssdd
     */
    public void setEntityID(String entityID) {

        this.entityID = entityID;
    }

    /**
     * Gets the endpoint url.
     *
     * @return the endpoint url
     * @ssdd
     */
    public String getEndpointURL() {

        return endpointURL;
    }

    /**
     * Sets the endpoint url.
     *
     * @param endpointURL
     *            the new endpoint url
     * @ssdd
     */
    public void setEndpointURL(String endpointURL) {

        this.endpointURL = endpointURL;
    }

    /**
     * Gets the subscriptions.
     *
     * @return the subscriptions
     * @ssdd
     */
    public Set<NotificationSubscription> getSubscriptions() {

        return subscriptions;
    }

    /**
     * Sets the subscriptions.
     *
     * @param subscriptions
     *            the new subscriptions
     * @ssdd
     */
    public void setSubscriptions(Set<NotificationSubscription> subscriptions) {

        this.subscriptions.clear();
        for (NotificationSubscription subscription : subscriptions) {
            subscription.setNotification(this);
            this.subscriptions.add(subscription);
        }
    }

    /**
     * Adds the subscription.
     *
     * @param subscription
     *            the subscription
     * @ssdd
     */
    public void addSubscription(NotificationSubscription subscription) {

        subscription.setNotification(this);
        this.subscriptions.add(subscription);
    }

    public String toString() {

        String sNotf = "";
        sNotf += "EntityID: " + this.entityID + "##";
        sNotf += "EndpointURL: " + this.endpointURL + "##";
        for (NotificationSubscription sub : subscriptions)
            sNotf += "SubID: " + String.valueOf(sub.getSubscriptionID()) + "##";
        sNotf += "Message list size: " + msgCount + "##";
        return sNotf;
    }

    /**
     * Gets the messages set.
     *
     * @return the messages set
     * @ssdd
     */
    public Set<NotificationMessage> getMessages() {

        return messages;
    }

    /**
     * Gets the messages.
     *
     * @return the messages
     * @ssdd
     */
    public Set<String> getMessagesAsString() {

        Set<String> msgSet = new LinkedHashSet<String>();
        for (NotificationMessage msg : messages) {
            msgSet.add(new String(msg.getMessage()));
        }
        return msgSet;
    }

    /**
     * Sets the messages.
     *
     * @param messages
     *            the new messages
     * @ssdd
     */
    public void setMessages(Set<NotificationMessage> messages) {
        LOG.info("setMessages: " + (messages != null ? messages.size() : "null"));
        this.messages.clear();
        for (NotificationMessage message : messages) {
            message.setNotification(this);
            this.messages.add(message);
        }

        setMsgCount(this.messages.size());;
    }

    /**
     * Adds the message.
     *
     * @param subscriptionID
     *            the subscription id
     * @param msgType
     *            the msg type
     * @param message
     *            the message
     * @ssdd
     */
    public void addMessage(Integer subscriptionID, String msgType, String message) {

        NotificationMessage msg = new NotificationMessage();
        msg.setNotification(this);
        msg.setMessage(message.getBytes());
        msg.setType(msgType);
        msg.setSubscriptionID(subscriptionID);
        NotificationMessageDAOHibernate.getInstance().makePersistent(msg);
        //this.messages.add(msg);
    }

    /**
     * Clear messages.
     *
     * @ssdd
     */
    public void clearMessages() {

        // removes all associated notifMessages
        for (NotificationMessage message : getMessages()) {
            clearMessage(message);
        }
        //getMessages().clear();
    }

    /*
     * To remove the NotificationMessage from the Set. If it's successful then removed it from
     * database.
     */
    private void clearMessage(NotificationMessage message) {
        try {
            //NotificationMessageDAOHibernate.getInstance().lock(message, LockMode.UPGRADE);
            //NotificationMessageDAOHibernate.getInstance().refresh(message);
            NotificationMessageDAOHibernate.getInstance().makeTransient(message);
        } catch (EntityNotFoundException enf) {
            LOG.warn(String.format("Message %d was already deleted: %s", message.getId(), enf.getMessage()), enf);
        }

    }

    /**
     * Clear messages of old product versions.
     *
     * @ssdd
     */
    public void clearoldMessage(String messageType) {

        if (!("WorkProductDeleted".equals(messageType))) {
            Set<NotificationMessage> messages = this.getMessages();
            if (!messages.isEmpty()) {
                for (NotificationMessage message : messages) {
                    if (messageType.equals(message.getType())) {
                        clearMessage(message);
                        messages.remove(message);
                        break;

                    }
                }

            }
        }

    }

    //FLi modified on 11/29/2011
    public int getMsgCount() {
        msgCount= this.messages.size();
        return msgCount;
    }

    public void setMsgCount(int msgCount) {
        this.msgCount = msgCount;
    }

}
