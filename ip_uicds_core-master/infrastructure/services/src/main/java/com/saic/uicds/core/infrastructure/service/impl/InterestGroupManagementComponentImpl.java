package com.saic.uicds.core.infrastructure.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.transaction.annotation.Transactional;
import org.uicds.agreementService.AgreementListType;
import org.uicds.agreementService.AgreementType;
import org.uicds.agreementService.AgreementType.ShareRules;
import org.uicds.agreementService.AgreementType.ShareRules.ShareRule;
import org.uicds.coreConfig.CoreConfigType;
import org.uicds.coreConfig.CoreStatusType;

import com.saic.precis.x2009.x06.base.CodespaceValueType;
import com.saic.uicds.core.infrastructure.dao.InterestGroupDAO;
import com.saic.uicds.core.infrastructure.exceptions.InvalidInterestGroupIDException;
import com.saic.uicds.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnavailableException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnknownException;
import com.saic.uicds.core.infrastructure.exceptions.XMPPComponentException;
import com.saic.uicds.core.infrastructure.messages.DeleteJoinedInterestGroupMessage;
import com.saic.uicds.core.infrastructure.messages.InterestGroupStateNotificationMessage;
import com.saic.uicds.core.infrastructure.messages.JoinedInterestGroupNotificationMessage;
import com.saic.uicds.core.infrastructure.model.InterestGroup;
import com.saic.uicds.core.infrastructure.service.AgreementService;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.InterestGroupManagementComponent;
import com.saic.uicds.core.infrastructure.util.InterestGroupInfoUtil;

/**
 * The InterestGroupManagementComponentImpl uses Spring Message Channels to communicate with the
 * communications infrastructure about sharing and updating Interest Groups between cores. It
 * maintains state about all the current Interest Groups in
 * {@link com.saic.uicds.core.infrastructure.dao.InterestGroupDAO}.
 * 
 * 
 * @author Aruna Hau
 * @since 1.0
 * @see com.saic.uicds.core.infrastructure.dao.InterestGroupDAO Interest Group DAO
 * @ssdd
 * 
 */
@Transactional
public class InterestGroupManagementComponentImpl
    implements InterestGroupManagementComponent {

    Logger log = LoggerFactory.getLogger(InterestGroupManagementComponentImpl.class);

    private InterestGroupDAO interestGroupDAO;
    private MessageChannel interestGroupStateNotificationChannel;
    private MessageChannel newJoinedInterestGroupChannel;
    private MessageChannel deleteJoinedInterestGroupChannel;

    public void setInterestGroupDAO(InterestGroupDAO interestGroupDAO) {

        this.interestGroupDAO = interestGroupDAO;
    }

    public void setInterestGroupStateNotificationChannel(
        MessageChannel interestGroupStateNotificationChannel) {

        this.interestGroupStateNotificationChannel = interestGroupStateNotificationChannel;
    }

    public void setNewJoinedInterestGroupChannel(MessageChannel newJoinedInterestGroupChannel) {

        this.newJoinedInterestGroupChannel = newJoinedInterestGroupChannel;
    }

    public void setDeleteJoinedInterestGroupChannel(MessageChannel deleteJoinedInterestGroupChannel) {

        this.deleteJoinedInterestGroupChannel = deleteJoinedInterestGroupChannel;
    }

    private ConfigurationService configurationService;

    public void setConfigurationService(ConfigurationService configurationService) {

        this.configurationService = configurationService;
    }

    private DirectoryService directoryService;

    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    private AgreementService agreementService;

    public void setAgreementService(AgreementService agreementService) {

        this.agreementService = agreementService;
    }

    /**
     * Creates the interest group and sends a state change notification.j
     * 
     * @param interestGroupInfo the interest group info
     * 
     * @return the string
     * @ssdd
     */
    @Override
    public String createInterestGroup(InterestGroupInfo interestGroupInfo) {

        log.debug("createInterestGroup: name=" + interestGroupInfo.getName() + " owningCore="
            + interestGroupInfo.getOwningCore());
        String interestGroupID = "IG-" + UUID.randomUUID().toString();

        log.debug("Creating interest group id=" + interestGroupID);

        InterestGroup interestGroup = new InterestGroup();
        interestGroup.setInterestGroupID(interestGroupID);
        interestGroup.setInterestGroupType(StringEscapeUtils.escapeXml(interestGroupInfo.getInterestGroupType()));
        interestGroup.setInterestGroupSubtype(StringEscapeUtils.escapeXml(interestGroupInfo.getInterestGroupSubType()));
        interestGroup.setDescription(StringEscapeUtils.escapeXml(interestGroupInfo.getDescription()));
        interestGroup.setName(StringEscapeUtils.escapeXml(interestGroupInfo.getName()));
        interestGroup.setOwningCore(interestGroupInfo.getOwningCore());
        interestGroup.setSharingStatus(InterestGroupStateNotificationMessage.SharingStatus.None.toString());

        log.debug("createInterestGroup: persist the created interest group id=" + interestGroupID);

        try {
            interestGroupDAO.makePersistent(interestGroup);
        } catch (HibernateException e) {
            log.error("createInterestGroup: HibernateException makePersistent interestGroupDAO: "
                + e.getMessage() + " from " + e.toString());
        } catch (Exception e) {
            log.error("createInterestGroup Exception makePersistent interestGroupDAO: "
                + e.getMessage() + " from " + e.toString());
        }

        // List<InterestGroup> igList = interestGroupDAO.findAll();
        // log.debug("===> found " + igList.size() + " interest groups in database");
        // for (InterestGroup ig : igList) {
        // log.debug("========> igID=" + ig.getInterestGroupID());
        // }

        // send interest group state change to Comms
        log.debug("===> notify Comms of new interest group");
        InterestGroupStateNotificationMessage mesg = new InterestGroupStateNotificationMessage();
        mesg.setState(InterestGroupStateNotificationMessage.State.NEW);
        mesg.setInterestGroupID(interestGroupID);
        mesg.setInterestGroupType(interestGroupInfo.getInterestGroupType());
        mesg.setOwningCore(interestGroupInfo.getOwningCore());
        mesg.setSharingStatus(interestGroup.getSharingStatus());
        // set to null those values not relevant when state=NEW
        mesg.setInterestGroupInfo(null);
        Message<InterestGroupStateNotificationMessage> notification = new GenericMessage<InterestGroupStateNotificationMessage>(
            mesg);
        interestGroupStateNotificationChannel.send(notification);

        return interestGroupID;
    }

    /**
     * Update interest group.
     * 
     * @param interestGroupInfo the interest group info
     * 
     * @throws InvalidInterestGroupIDException the invalid interest group id exception
     * @ssdd
     */
    @Override
    public void updateInterestGroup(InterestGroupInfo interestGroupInfo)
        throws InvalidInterestGroupIDException {

        InterestGroup interestGroup = interestGroupDAO.findByInterestGroup(interestGroupInfo.getInterestGroupID());
        if (interestGroup == null) {
            throw new InvalidInterestGroupIDException(interestGroupInfo.getInterestGroupID());
        } else {
            interestGroup.setInterestGroupSubtype(interestGroupInfo.getInterestGroupSubType());
            interestGroup.setDescription(interestGroupInfo.getDescription());
            interestGroup.setName(interestGroupInfo.getName());

            log.debug("updateInterestGroup: " + interestGroupInfo.toString());
            try {
                interestGroupDAO.makePersistent(interestGroup);
            } catch (HibernateException e) {
                log.error("updateInterestGroup: HibernateException makePersistent interestGroupDAO: "
                    + e.getMessage() + " from " + e.toString());
            } catch (Exception e) {
                log.error("updateInterestGroup: Exception makePersistent interestGroupDAO: "
                    + e.getMessage() + " from " + e.toString());
            }

            // send interest group state change to Comms
            // Note: although comms does not take any action when receiving this state change, we
            // send it anyway in case this needs to be handled differently in the future.
            InterestGroupStateNotificationMessage mesg = new InterestGroupStateNotificationMessage();
            mesg.setState(InterestGroupStateNotificationMessage.State.UPDATE);
            mesg.setInterestGroupID(interestGroupInfo.getInterestGroupID());
            mesg.setInterestGroupType(interestGroupInfo.getInterestGroupType());
            mesg.setOwningCore(interestGroupInfo.getOwningCore());
            mesg.setSharingStatus(interestGroup.getSharingStatus());
            // set to null those values not relevant when state=UPDATE
            mesg.setInterestGroupInfo(null);
            Message<InterestGroupStateNotificationMessage> notification = new GenericMessage<InterestGroupStateNotificationMessage>(
                mesg);
            interestGroupStateNotificationChannel.send(notification);
        }
    }

    /**
     * Delete interest group.
     * 
     * @param interestGroupID the interest group id
     * 
     * @throws InvalidInterestGroupIDException the invalid interest group id exception
     * @ssdd
     */
    @Override
    public void deleteInterestGroup(String interestGroupID) throws InvalidInterestGroupIDException {

        log.debug("deleteInterestGroup - interestGroup ID=" + interestGroupID);
        InterestGroup interestGroup = interestGroupDAO.findByInterestGroup(interestGroupID);
        if (interestGroup == null) {
            throw new InvalidInterestGroupIDException(interestGroupID);
        } else {
            log.debug("===> notify Comms of deleted  interest group");
            InterestGroupStateNotificationMessage mesg = new InterestGroupStateNotificationMessage();
            mesg.setState(InterestGroupStateNotificationMessage.State.DELETE);
            mesg.setInterestGroupID(interestGroupID);
            // set to null those values not relevant when state=NEW
            mesg.setInterestGroupType(null);
            mesg.setOwningCore(null);
            mesg.setSharingStatus(null);
            mesg.setInterestGroupInfo(null);
            Message<InterestGroupStateNotificationMessage> notification = new GenericMessage<InterestGroupStateNotificationMessage>(
                mesg);
            interestGroupStateNotificationChannel.send(notification);

            log.debug("deleteInterestGroup: remove interest group; ID=" + interestGroupID
                + " from DB");
            try {
                interestGroupDAO.delete(interestGroupID, true);
            } catch (HibernateException e) {
                log.error("deleteInterestGroup: HibernateException makeTransient interestGroupDAO: "
                    + e.getMessage() + " from " + e.toString());
            } catch (Exception e) {
                log.error("deleteInterestGroup: Exception makeTransient interestGroupDAO: "
                    + e.getMessage() + " from " + e.toString());
            }
        }

    }

    /**
     * Received joined interest group.
     * 
     * @param message the message
     * @ssdd
     */
    @Override
    public void receivedJoinedInterestGroup(JoinedInterestGroupNotificationMessage message) {

        log.debug("receivedJoinedInterestGroup - received notification of joined interest group id="
            + message.interestGroupID
            + " owner="
            + message.owner
            + " ownerProps="
            + message.ownerProperties);

        InterestGroup interestGroup = null;

        try {
            // persist the new interest group
            log.debug("receivedJoinedInterestGroup - persist the joined interest group id="
                + message.interestGroupID);
            interestGroup = InterestGroupInfoUtil.toInterestGroup(message.getInterestGroupInfo());
            interestGroup.setSharingStatus(InterestGroupStateNotificationMessage.SharingStatus.Joined.toString());
            log.debug("receivedJoinedInterestGroup:  ==> sharing status="
                + interestGroup.getSharingStatus());
            interestGroup.setOwnerProperties(message.ownerProperties);
            interestGroup.setJoinedWpTypeList(message.joinedWPTypes);
            try {
                interestGroupDAO.makePersistent(interestGroup);
            } catch (HibernateException e) {
                log.error("receivedJoinedInterestGroup: HibernateException makePersistent interestGroupDAO: "
                    + e.getMessage() + " from " + e.toString());
            } catch (Exception e) {
                log.error("receivedJoinedInterestGroup: Exception makePersistent interestGroupDAO: "
                    + e.getMessage() + " from " + e.toString());
            }

            // send upstream to the domain services that manage interest groups
            // Replace the interest group info with detailed info specific to the domain
            // interest group management services,
            // e.g. incident data for IMS
            String detailedInfo = InterestGroupInfoUtil.toInterestDetailedInfoString(message.getInterestGroupInfo());
            message.setInterestGroupInfo(detailedInfo);
            Message<JoinedInterestGroupNotificationMessage> notification = new GenericMessage<JoinedInterestGroupNotificationMessage>(
                message);
            newJoinedInterestGroupChannel.send(notification);

        } catch (Throwable e) {
            log.error("receivedJoinedInterestGroupHandler: error parsing received incident info");
            e.printStackTrace();
        }

    }

    /**
     * Share interest group.
     * 
     * @param interestGroupID the interest group id
     * @param targetCore the target core
     * @param detailedInfo the detailed info
     * @param agreementChecked the agreement checked
     * 
     * @throws InvalidInterestGroupIDException the invalid interest group id exception
     * @throws LocalCoreNotOnlineException the local core not online exception
     * @throws RemoteCoreUnavailableException the remote core unavailable exception
     * @throws XMPPComponentException the XMPP component exception
     * @throws NoShareAgreementException the no share agreement exception
     * @throws NoShareRuleInAgreementException the no share rule in agreement exception
     * @ssdd
     */
    @Override
    public void shareInterestGroup(String interestGroupID, String targetCore, String detailedInfo,
        boolean agreementChecked) throws InvalidInterestGroupIDException,
        LocalCoreNotOnlineException, RemoteCoreUnavailableException, RemoteCoreUnknownException,
        XMPPComponentException, NoShareAgreementException, NoShareRuleInAgreementException {

        log.debug("shareInterestGroup: igID=" + interestGroupID + " targetCore=" + targetCore
            + " detailedInfo=[" + detailedInfo + "]");
        InterestGroup interestGroup = interestGroupDAO.findByInterestGroup(interestGroupID);
        if (interestGroup == null) {
            throw new InvalidInterestGroupIDException(interestGroupID);
        } else {

            CoreConfigType coreConfig = directoryService.getCoreConfig(configurationService.getCoreName());
            if ((coreConfig == null) || (coreConfig.getOnlineStatus() != CoreStatusType.ONLINE)) {
                throw new LocalCoreNotOnlineException();
            } else {
                CoreConfigType remoteCoreConfig = directoryService.getCoreConfig(targetCore);

                if (remoteCoreConfig == null) {
                    throw new RemoteCoreUnknownException(targetCore);
                } else if (remoteCoreConfig.getOnlineStatus() != CoreStatusType.ONLINE) {
                    throw new RemoteCoreUnavailableException(targetCore);
                } else {

                    // NULL workProductTypesToShare list means agreement indicates no sharing
                    List<String> workProductTypesToShare = new ArrayList<String>();
                    if (!agreementChecked) {
                        workProductTypesToShare = getShareAgreement(interestGroup, targetCore);
                    }
                    if (workProductTypesToShare != null) {
                        try {

                            // send interest group state change to Comms
                            InterestGroupStateNotificationMessage mesg = new InterestGroupStateNotificationMessage();
                            mesg.setState(InterestGroupStateNotificationMessage.State.SHARE);
                            mesg.setInterestGroupID(interestGroup.getInterestGroupID());
                            mesg.setInterestGroupType(interestGroup.getInterestGroupType());
                            mesg.setOwningCore(interestGroup.getOwningCore());
                            mesg.getSharedCoreList().add(targetCore);
                            mesg.setWorkProductTypesToShare(workProductTypesToShare);
                            mesg.setInterestGroupInfo(InterestGroupInfoUtil.toXMLString(
                                interestGroup, detailedInfo));
                            mesg.setSharingStatus(interestGroup.getSharingStatus());
                            Message<InterestGroupStateNotificationMessage> notification = new GenericMessage<InterestGroupStateNotificationMessage>(
                                mesg);
                            interestGroupStateNotificationChannel.send(notification);
                        } catch (IllegalStateException e) {
                            throw new RemoteCoreUnavailableException("core unavailable");
                        } catch (Exception e) {
                            log.error("shareInterestGroup - exception caught from the XMPP component");
                            e.printStackTrace();
                            throw new XMPPComponentException(e.getMessage());
                        }

                        // TODO: RDW These next lines need to happen after getting the response from
                        // interestGroupStateNotificationChannel.send(notification);
                        interestGroup.setSharingStatus(InterestGroupStateNotificationMessage.SharingStatus.Shared.toString());
                        interestGroup.getSharedCoreList().add(targetCore);

                        log.debug("shareInterestGroup: update sharingStatus ["
                            + InterestGroupStateNotificationMessage.SharingStatus.Shared.toString()
                            + "] and persist the shared interest group id="
                            + interestGroup.getInterestGroupID());
                        try {
                            interestGroupDAO.makePersistent(interestGroup);
                        } catch (HibernateException e) {
                            log.error("shareInterestGroup: HibernateException makePersistent interestGroupDAO: "
                                + e.getMessage() + " from " + e.toString());
                        } catch (Exception e) {
                            log.error("shareInterestGroup: Exception makePersistent interestGroupDAO: "
                                + e.getMessage() + " from " + e.toString());
                        }
                    }
                }
            }
        }
    }

    // returns list of work product types to be shared.
    // - empty list : share everything
    // - non-empty list : share work product types in list (not implemented - for now we share
    // everything)
    private List<String> getShareAgreement(InterestGroup interestGroup, String targetCore)
        throws NoShareAgreementException, NoShareRuleInAgreementException {

        log.debug("getShareAgreement - interestGroupID=" + interestGroup.getInterestGroupID());

        List<String> workProductTypesToShare = new ArrayList<String>();
        String igCodespace = InterestGroupManagementComponent.CodeSpace
            + interestGroup.getInterestGroupType();

        // TODO: ticket #248
        // since agreement is stored by coreJID and share incident is by host name (for now)
        // we cannot get the agreement by coreJID. For now, we have to get all the agreements and
        // loop through to get the right one.
        // This can go back to just getting agreement by coreJID when ticket #248 is implemented
        boolean agreementFound = false;
        boolean shareRulesEnabled = false;
        boolean shareRuleMatched = false;
        boolean shareRuleFound = false;

        AgreementListType agreementList = agreementService.getAgreementList();
        for (AgreementType agreement : agreementList.getAgreementArray()) {

            // If there's no active agreement with this core, we share all types
            if ((agreement != null)
                && (agreement.getPrincipals().getRemoteCore().getStringValue().contains(targetCore))) {
                agreementFound = true;
                ShareRules shareRules = agreement.getShareRules();
                if (shareRules != null) {
                    // if share rules exist, there must be an exact rule match (interestGroup's
                    // codepsace/subtype)
                    // or there will be no sharing
                    if (shareRules.getEnabled()) {
                        shareRulesEnabled = true;
                        ShareRule[] shareRuleArray = shareRules.getShareRuleArray();
                        if (shareRules.sizeOfShareRuleArray() > 0) {
                            for (ShareRule shareRule : shareRuleArray) {
                                if (shareRule.getEnabled() && (shareRule.getCondition() != null)) {
                                    shareRuleFound = true;
                                    CodespaceValueType cs = shareRule.getCondition().getInterestGroup();
                                    String codeSpace = cs.getCodespace();
                                    log.debug("===> codespace=[" + codeSpace + "]");
                                    log.debug("===> igCodspce=[" + igCodespace + "]");
                                    String codespaceValue = cs.getStringValue();
                                    if ((codeSpace.equals(igCodespace))
                                        && (codespaceValue.equalsIgnoreCase(interestGroup.getInterestGroupSubtype()))) {
                                        shareRuleMatched = true;
                                        // for now: share everything - in the future add the work
                                        // product type to workProductTypesToShare
                                    } else if ((codeSpace.equals(InterestGroupManagementComponent.MANUAL_CODE_SPACE))
                                        && (codespaceValue.equalsIgnoreCase(Boolean.TRUE.toString()))) {
                                        shareRuleMatched = true;
                                    }
                                }
                            }
                        }
                    } // end if shareRule enabled
                } // end if shareRules not null
            } // end of agreement not null
        }

        if (!agreementFound) {
            // no agreement between the core and the target core
            throw new NoShareAgreementException(interestGroup.getOwningCore(), targetCore);
        } else if ((!shareRulesEnabled) || ((shareRuleFound) && (!shareRuleMatched))) {
            // One of the following:
            // - shareRules is not enabled, or
            // - shareRules is enabled and at least one rule is defined but no rule match was found
            // Important: the order of the above conditional statement is important and should NOT
            // be altered!!!

            throw new NoShareRuleInAgreementException(interestGroup.getOwningCore(), targetCore,
                interestGroup.getInterestGroupType(), interestGroup.getInterestGroupSubtype());
        }

        return workProductTypesToShare;
    }

    /**
     * Delete joined interest group.
     * 
     * @param message the message
     * @ssdd
     */
    public void deleteJoinedInterestGroup(DeleteJoinedInterestGroupMessage message) {

        try {
            interestGroupDAO.delete(message.getInterestGroupID(), true);
        } catch (HibernateException e) {
            log.error("deleteJoinedInterestGroup: HibernateException makeTransient interestGroupDAO: "
                + e.getMessage() + " from " + e.toString());
        } catch (Exception e) {
            log.error("deleteJoinedInterestGroup: Exception makeTransient interestGroupDAO: "
                + e.getMessage() + " from " + e.toString());
        }

        // notify IMS of the deletion of the restored interest group
        deleteJoinedInterestGroupChannel.send(new GenericMessage<DeleteJoinedInterestGroupMessage>(
            message));

    }

    @Override
    public void unshareInterestGroup(String interestGroupID, String targetCore) {

        // TODO Auto-generated method stub

    }

    /**
     * System initialized handler.
     * 
     * @param message the message
     */
    @Override
    public void systemInitializedHandler(String message) {

        // No need to do anything yet
        log.debug("InterestGroupManagementComponent - initialized");
        List<InterestGroup> igList = interestGroupDAO.findAll();
        log.debug("===> found " + igList.size() + " interest groups in database");
        for (InterestGroup ig : igList) {
            String interestGroupID = ig.getInterestGroupID();
            log.debug("========> igID=" + interestGroupID);

            InterestGroupStateNotificationMessage mesg = new InterestGroupStateNotificationMessage();
            mesg.setState(InterestGroupStateNotificationMessage.State.RESTORE);
            mesg.setInterestGroupID(interestGroupID);
            mesg.setInterestGroupType(ig.getInterestGroupType());
            mesg.setOwningCore(ig.getOwningCore());
            mesg.setSharingStatus(ig.getSharingStatus());
            mesg.setSharedCoreList(ig.getSharedCoreList());
            mesg.setOwmnerProperties(ig.getOwnerProperties());
            mesg.setJoinedWPTypes(ig.getJoinedWpTypeList());

            // set to null those values not relevant when state=RESTORE
            mesg.setInterestGroupInfo(null);
            Message<InterestGroupStateNotificationMessage> notification = new GenericMessage<InterestGroupStateNotificationMessage>(
                mesg);
            interestGroupStateNotificationChannel.send(notification);
        }
    }

    /**
     * Gets the interest group.
     * 
     * @param interestGroupID the interest group id
     * 
     * @return the interest group
     * @ssdd
     */
    @Override
    public InterestGroupInfo getInterestGroup(String interestGroupID) {

        // log.debug("getInterestGroup: id=" + interestGroupID);
        InterestGroup ig = interestGroupDAO.findByInterestGroup(interestGroupID);
        return toInterestGroupInfo(ig);
    }

    @Override
    public List<InterestGroupInfo> getInterestGroupList() {

        log.debug("getInterestGroupList:");
        List<InterestGroupInfo> igInfoList = new ArrayList<InterestGroupInfo>();
        List<InterestGroup> igList = interestGroupDAO.findAll();
        for (InterestGroup ig : igList) {
            InterestGroupInfo igInfo = toInterestGroupInfo(ig);
            if (igInfo != null) {
                igInfoList.add(igInfo);
            }
        }
        return igInfoList;
    }

    /**
     * Determines if an interest group owned by core.
     * 
     * @param interestGroupID the interest group id
     * 
     * @return true, if successful
     * @ssdd
     */
    public boolean interestGroupOwnedByCore(String interestGroupID) {

        InterestGroup ig = interestGroupDAO.findByInterestGroup(interestGroupID);
        if (ig == null) {
            log.error("interestGroupOwnedByCore - interest group ID=" + interestGroupID
                + " not found!");
            ;
            return false;
        } else {
            log.debug("interestGroupOwnedByCore: interest group owned by:[" + ig.getOwningCore()
                + "]");
            return ig.getOwningCore().equals(directoryService.getCoreName());
        }
    }

    private InterestGroupInfo toInterestGroupInfo(InterestGroup ig) {

        InterestGroupInfo info = null;
        if (ig != null) {
            info = new InterestGroupInfo();
            info.setInterestGroupID(ig.getInterestGroupID());
            info.setInterestGroupType(ig.getInterestGroupType());
            info.setInterestGroupSubType(ig.getInterestGroupSubtype());
            info.setName(ig.getName());
            info.setDescription(ig.getDescription());
            info.setOwningCore(ig.getOwningCore());
        }
        return info;
    }

}
