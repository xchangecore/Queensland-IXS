package com.saic.uicds.core.infrastructure.messages;

import java.util.ArrayList;
import java.util.List;

public class ShareInterestGroupMessage {
    private String interestGroupID;
    private String remoteCore;
    private String interestGroupInfo;
    private List<String> workProductTypesToShare = new ArrayList<String>();

    public String getInterestGroupID() {
        return interestGroupID;
    }

    public void setInterestGroupID(String interestGroupID) {
        this.interestGroupID = interestGroupID;
    }

    public String getRemoteCore() {
        return remoteCore;
    }

    public void setRemoteCore(String remoteCore) {
        this.remoteCore = remoteCore;
    }

    public String getInterestGroupInfo() {
        return interestGroupInfo;
    }

    public void setInterestGroupInfo(String interestGroupInfo) {
        this.interestGroupInfo = interestGroupInfo;
    }

    public List<String> getWorkProductTypesToShare() {
        return workProductTypesToShare;
    }

    public void setWorkProductTypesToShare(List<String> workProductTypesToShare) {
        this.workProductTypesToShare = workProductTypesToShare;
    }

}
