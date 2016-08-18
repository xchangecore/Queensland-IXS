package com.saic.uicds.core.infrastructure.messages;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AgreementRosterMessage {
    public static final String NAME = "AgreementRosterMessage";

    public static enum State {
        CREATE, AMEND, RESCIND,
    };

    private String agreementID;

    // Map of <Key: coreName , Value: State>
    private Map<String, State> cores = new HashMap<String, State>();

    public AgreementRosterMessage(String agreementID, Map<String, State> cores) {
        setAgreementID(agreementID);
        setCores(cores);
    }

    public String getAgreementID() {
        return agreementID;
    }

    public void setAgreementID(String agreementID) {
        this.agreementID = agreementID;
    }

    public Map<String, State> getCores() {
        return cores;
    }

    public void setCores(Map<String, State> cores) {
        this.cores = cores;
    }

    @Override
    public String toString() {
        Set<String> keys = cores.keySet();
        String out = "";
        for (String value : keys) {
            out += ", " + value;
        }

        return out;
    }

}
