package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;

public class CoreConfigGWT implements Serializable {

    private static final long serialVersionUID = -7993965046982980429L;

    private boolean root = false;
    private boolean leaf = true;
    private String onlineStatus;
    private String coreName;
    private String XML;

    public String getCoreName() {

        return coreName;
    }

    public String getOnlineStatus() {

        return this.onlineStatus;
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

    public void setOnlineStatus(String onlineStatus) {

        this.onlineStatus = onlineStatus;
    }

    public void setRoot(boolean root) {

        this.root = root;
    }

    public void setXML(String xML) {

        XML = xML;
    }
}
