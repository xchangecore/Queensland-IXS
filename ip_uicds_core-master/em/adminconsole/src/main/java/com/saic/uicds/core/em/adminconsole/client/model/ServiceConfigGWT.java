package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;

public class ServiceConfigGWT implements Serializable {

    private boolean root = false;
    private boolean leaf = true;
    private String coreName;
    private String serviceName;
    private String URN;
    private String XML;

    /**
     * 
     */
    private static final long serialVersionUID = -7993965046982980429L;

    public String getCoreName() {

        return coreName;
    }

    public String getServiceName() {

        return serviceName;
    }

    public String getURN() {

        return URN;
    }

    public String getXML() {

        return XML;
    }

    public boolean isLeaf() {

        return leaf;
    }

    public boolean isRoot() {

        return root;
    }

    public void setCoreName(String coreName) {

        this.coreName = coreName;
    }

    public void setLeaf(boolean leaf) {

        this.leaf = leaf;
    }

    public void setRoot(boolean root) {

        this.root = root;
    }

    public void setServiceName(String serviceName) {

        this.serviceName = serviceName;
    }

    public void setURN(String uRN) {

        URN = uRN;
    }

    public void setXML(String xML) {

        XML = xML;
    }
}
