package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProfileGWT implements Serializable {

    private static final long serialVersionUID = 7143149091080849373L;

    public static final String PROFILE_PRODUCT_TYPE = "Profile";

    private boolean root = false;
    private boolean leaf = true;
    private String entityID;
    private String identifier;
    private String description;
    private String refName;
    private String XML;

    private Map<String, String> resourceTyping = new HashMap<String, String>();

    private Set<String> interests = new HashSet<String>(); // just topic expressions for now

    public String getDescription() {

        return description;
    }

    public String getEntityID() {

        return entityID;
    }

    public String getIdentifier() {

        return identifier;
    }

    public Set<String> getInterests() {

        return interests;
    }

    public String getRefName() {

        return refName;
    }

    public Map<String, String> getResourceTyping() {

        return resourceTyping;
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

    public void setDescription(String description) {

        this.description = description;
    }

    public void setEntityID(String entityID) {

        this.entityID = entityID;
    }

    public void setIdentifier(String identifier) {

        this.identifier = identifier;
    }

    public void setInterests(Set<String> interests) {

        this.interests = interests;
    }

    public void setLeaf(boolean leaf) {

        this.leaf = leaf;
    }

    public void setRefName(String refName) {

        this.refName = refName;
    }

    public void setResourceTyping(Map<String, String> resourceTyping) {

        this.resourceTyping = resourceTyping;
    }

    public void setRoot(boolean root) {

        this.root = root;
    }

    public void setXML(String xML) {

        XML = xML;
    }
}
