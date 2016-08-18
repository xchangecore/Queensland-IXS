package com.saic.uicds.core.infrastructure.service;

import java.util.List;

import com.saic.uicds.core.infrastructure.exceptions.InvalidInterestGroupIDException;
import com.saic.uicds.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnavailableException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnknownException;
import com.saic.uicds.core.infrastructure.exceptions.XMPPComponentException;
import com.saic.uicds.core.infrastructure.messages.DeleteJoinedInterestGroupMessage;
import com.saic.uicds.core.infrastructure.messages.JoinedInterestGroupNotificationMessage;
import com.saic.uicds.core.infrastructure.service.impl.InterestGroupInfo;

/**
 * The InterestGroupManagementComponent interface provides management of Interest Groups and the
 * messages to the communications infrastructure to enable sharing and updating of Interest Groups.
 * It uses the communications infrastructure to communicate with the
 * InterestGroupManagementComponent on other cores to coordinate sharing and removing of Interest
 * Groups.
 * <p>
 * Services that want to manage a particular type of Interest Group use an implementation of this
 * service directly to manage the lifecycle of an Interest Group and be notified when new Interest
 * Groups are shared to its core through these Spring Message Channels:<br</>
 * newInterestGroupCreated<br</> shareInterestGroup<br</> deleteInterestGroup<br</>
 * 
 * @author Aruna Hau
 * @since 1.0
 * @ssdd
 * 
 */
public interface InterestGroupManagementComponent {

    public static final String INTEREST_GROUP_MGMT_COMPONENT_NAME = "InterestGroupManagementComponent";
    public static final String CodeSpace = "http://uicds.org/interestgroup#";
    public static final String MANUAL_CODE_SPACE = CodeSpace + "Manual";

    /**
     * Creates the interest group.
     * 
     * @param interestGroupInfo the interest group info
     * 
     * @return the interest group id
     * @ssdd
     */
    public String createInterestGroup(InterestGroupInfo interestGroupInfo);

    /**
     * Update interest group info.
     * 
     * @param interestGroupInfo the interest group info
     * 
     * @throws InvalidInterestGroupIDException the invalid interest group id exception
     * @ssdd
     */
    public void updateInterestGroup(InterestGroupInfo interestGroupInfo)
        throws InvalidInterestGroupIDException;

    /**
     * Delete interest group.
     * 
     * @param interestGroupID the interest group id
     * 
     * @throws InvalidInterestGroupIDException the invalid interest group id exception
     * @ssdd
     */
    public void deleteInterestGroup(String interestGroupID) throws InvalidInterestGroupIDException;

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
    public void shareInterestGroup(String interestGroupID, String targetCore, String detailedInfo,
        boolean agreementChecked) throws InvalidInterestGroupIDException,
        LocalCoreNotOnlineException, RemoteCoreUnavailableException, RemoteCoreUnknownException,
        XMPPComponentException, NoShareAgreementException, NoShareRuleInAgreementException;

    /**
     * Gets the interest group.
     * 
     * @param interestGroupID the interest group id
     * 
     * @return the interest group
     * @ssdd
     */
    public InterestGroupInfo getInterestGroup(String interestGroupID);

    // get list of active interest groups
    public List<InterestGroupInfo> getInterestGroupList();

    /**
     * Unshare interest group.
     * 
     * @param interestGroupID the interest group id
     * @param targetCore the target core
     * @ssdd
     */
    public void unshareInterestGroup(String interestGroupID, String targetCore);

    /**
     * @param message
     * @ssdd
     */
    public void receivedJoinedInterestGroup(JoinedInterestGroupNotificationMessage message);

    /**
     * @param message
     * @ssdd
     */
    public void deleteJoinedInterestGroup(DeleteJoinedInterestGroupMessage message);

    /**
     * Performs initialization operations.
     * 
     * @param message
     * @ssdd
     */
    public void systemInitializedHandler(String message);

    /**
     * this will return whether the core owns this interest group or not
     * 
     * @param interestGroupID
     * @return boolean
     * 
     * @ssdd
     */
    public boolean interestGroupOwnedByCore(String interestGroupID);
}
