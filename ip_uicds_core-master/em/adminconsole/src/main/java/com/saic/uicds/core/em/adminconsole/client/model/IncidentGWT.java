package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class IncidentGWT implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6374667493671246882L;

    private String summary;
    private boolean root = false;
    private boolean leaf = false;
    private boolean closed = false;

    private Boolean isIncident = false;

    private String incidentID;
    private boolean digest = false;
    private String workProductID;
    private String name;
    private String title;
    private Set<IncidentGWT> children;
    
    public void setChildren(Set<IncidentGWT> children) {
        this.children=new TreeSet<IncidentGWT>();
        Iterator<IncidentGWT> it=children.iterator();
        while(it.hasNext()){
            this.children.add(it.next());
        }
    }
    public IncidentGWT() {

    }
    public IncidentGWT(String title) {

        setTitle(title);
    }

    public IncidentGWT(String name, IncidentGWT[] children) {

        this(name);

        this.children = new HashSet<IncidentGWT>();
        for (int i = 0; i < children.length; i++) {
            this.children.add(children[i]);
        }
    }

    public Set<IncidentGWT> getChildren() {

        return this.children;
    }

    public String getIncidentID() {

        return incidentID;
    }

    public boolean getIsIncident() {

        return isIncident;
    }

    public String getName() {

        return name;
    }

    /**
     * Return the summary of the incident.
     * 
     * @return
     */
    public String getSummary() {

        return summary;
    }
    
    public boolean hasDigest() {
        return digest;
    }

    public void setDigest(boolean digest) {
        this.digest = digest;
    }

    public String getTitle() {

        return title;
    }

    public String getWorkProductID() {

        return workProductID;
    }

    public boolean isClosed() {

        return closed;
    }

    public boolean isLeaf() {

        return leaf;
    }

    public boolean isRoot() {

        return root;
    }

    public void setClosed(boolean closed) {

        this.closed = closed;
    }

    public void setIncidentID(String incidentID) {

        this.incidentID = incidentID;
    }

    public void setIsIncident(boolean isIncident) {

        this.isIncident = isIncident;
    }

    public void setLeaf(boolean leaf) {

        this.leaf = leaf;
    }

    public void setName(String name) {

        this.name = name;
    }

    public void setRoot(boolean root) {

        this.root = root;
    }

    /**
     * Set the summary of the incident.
     * 
     * @param summary
     */
    public void setSummary(String summary) {

        this.summary = summary;
    }

    /**
     * Set the title of the incident.
     * 
     * @param title
     */
    public void setTitle(String title) {

        this.title = title;
        setName(title);
    }

    public void setWorkProductID(String workProductID) {

        this.workProductID = workProductID;
    }

    @Override
    public String toString() {

        return getName();
    }
}
