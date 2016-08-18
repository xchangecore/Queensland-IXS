package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * ResourceInstanceGWT [Has GWT UI fields for ResourceInstance. Identifier,ResourceId, and existing
 * ResourceProfile are required for new instance]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.client.model
 */

public class ResourceInstanceGWT implements Serializable {

    private static final long serialVersionUID = -1814159993788896634L;

    public static final String RESOURCE_INSTANCE_TYPE = "ResouceInstance";

    private boolean root = false;
    private boolean leaf = true;

    private String identifier;
    private String localResourceId;
    private String XML;
    private Set<String> profiles = new HashSet<String>();

    public String getIdentifier() {

        return identifier;
    }

    public String getLocalResourceId() {
        return localResourceId;
    }

    public Set<String> getProfiles() {
        return profiles;
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

    public void setIdentifier(String identifier) {

        this.identifier = identifier;
    }

    public void setLeaf(boolean leaf) {

        this.leaf = leaf;
    }

    public void setLocalResourceId(String localResourceId) {
        this.localResourceId = localResourceId;
    }

    public void setProfiles(Set<String> profiles) {
        this.profiles = profiles;
    }

    public void setRoot(boolean root) {

        this.root = root;
    }

    public void setXML(String xML) {
        XML = xML;
    }

}
