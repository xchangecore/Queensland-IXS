package com.saic.uicds.core.infrastructure.messages;

public class CoreStatusUpdateMessage {
    String coreName;
    String coreStatus;

    public String getCoreName() {
        return coreName;
    }

    public void setCoreName(String coreName) {
        this.coreName = coreName;
    }

    public String getCoreStatus() {
        return coreStatus;
    }

    public void setCoreStatus(String coreStatus) {
        this.coreStatus = coreStatus;
    }

    public CoreStatusUpdateMessage(String coreName, String coreStatus) {
        setCoreName(coreName);
        setCoreStatus(coreStatus);
    }
}
