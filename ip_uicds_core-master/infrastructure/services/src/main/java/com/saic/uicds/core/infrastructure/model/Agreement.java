package com.saic.uicds.core.infrastructure.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * The Agreement data model.
 * 
 * @ssdd
 */
@Entity
public class Agreement {

    @Id
    @Column(name = "AGREEMENT_ID")
    @GeneratedValue
    private Integer id;

    private boolean enabled;

    private String remoteCodeSpace;
    private String remoteValue;
    private String localCodeSpace;
    private String localValue;

    // @Embedded
    // @OneToMany(mappedBy = "agreement")
    // private Map<String,CodeSpaceValueType> principals;

    // @Embedded
    // @OneToMany(mappedBy = "agreement")
    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    @org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    private Set<ShareRule> shareRules;

    /**
     * Gets the id.
     * 
     * @return the id
     * @ssdd
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the id.
     * 
     * @param id the new id
     * @ssdd
     */
    public void setId(Integer id) {
        this.id = id;
    }

    public boolean equals(Object obj) {
        Agreement agrObj = (Agreement) obj;
        return (remoteValue.equals(agrObj.getRemoteValue()));
    }

    public int hashCode() {
        return remoteValue.hashCode();
    }

    // public Map<String,CodeSpaceValueType> getPrincipals() {
    // if (principals == null) {
    // principals = new HashMap<String,CodeSpaceValueType>();
    // }
    // return principals;
    // }

    /**
     * Gets the local core.
     * 
     * @return the local core
     * @ssdd
     */
    public CodeSpaceValueType getLocalCore() {
        CodeSpaceValueType c = new CodeSpaceValueType();
        c.setCodeSpace(localCodeSpace);
        c.setValue(localValue);
        return c;
    }

    /**
     * Gets the remote code space.
     * 
     * @return the remote code space
     * @ssdd
     */
    public String getRemoteCodeSpace() {
        return remoteCodeSpace;
    }

    /**
     * Sets the remote code space.
     * 
     * @param remoteCodeSpace the new remote code space
     */
    public void setRemoteCodeSpace(String remoteCodeSpace) {
        this.remoteCodeSpace = remoteCodeSpace;
    }

    /**
     * Gets the remote value.
     * 
     * @return the remote value
     * @ssdd
     */
    public String getRemoteValue() {
        return remoteValue;
    }

    /**
     * Sets the remote value.
     * 
     * @param remoteValue the new remote value
     * @ssdd
     */
    public void setRemoteValue(String remoteValue) {
        this.remoteValue = remoteValue;
    }

    /**
     * Gets the local code space.
     * 
     * @return the local code space
     * @ssdd
     */
    public String getLocalCodeSpace() {
        return localCodeSpace;
    }

    /**
     * Sets the local code space.
     * 
     * @param localCodeSpace the new local code space
     * @ssdd
     */
    public void setLocalCodeSpace(String localCodeSpace) {
        this.localCodeSpace = localCodeSpace;
    }

    /**
     * Gets the local value.
     * 
     * @return the local value
     * @ssdd
     */
    public String getLocalValue() {
        return localValue;
    }

    /**
     * Sets the local value.
     * 
     * @param localValue the new local value
     * @ssdd
     */
    public void setLocalValue(String localValue) {
        this.localValue = localValue;
    }

    /**
     * Sets the local core.
     * 
     * @param localCore the new local core
     * @ssdd
     */
    public void setLocalCore(CodeSpaceValueType localCore) {
        localCodeSpace = localCore.getCodeSpace();
        localValue = localCore.getValue();
        // localCore.setAgreement(this);
        // getPrincipals().put(LOCAL, localCore);
    }

    /**
     * Gets the remote core.
     * 
     * @return the remote core
     * @ssdd
     */
    public CodeSpaceValueType getRemoteCore() {
        CodeSpaceValueType c = new CodeSpaceValueType();
        c.setCodeSpace(remoteCodeSpace);
        c.setValue(remoteValue);
        return c;
    }

    /**
     * Sets the remote core.
     * 
     * @param remoteCore the new remote core
     * @ssdd
     */
    public void setRemoteCore(CodeSpaceValueType remoteCore) {
        remoteCodeSpace = remoteCore.getCodeSpace();
        remoteValue = remoteCore.getValue();
        // remoteCore.setAgreement(this);
        // getPrincipals().put(REMOTE, remoteCore);
    }

    /**
     * Checks if is enabled.
     * 
     * @return true, if is enabled
     * @ssdd
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled.
     * 
     * @param enabled the new enabled
     * @ssdd
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the share rules.
     * 
     * @return the share rules
     * @ssdd
     */
    public Set<ShareRule> getShareRules() {
        if (shareRules == null) {
            shareRules = new HashSet<ShareRule>();
        }
        return shareRules;
    }

    /**
     * Sets the share rules.
     * 
     * @param shareRules the new share rules
     * @ssdd
     */
    public void setShareRules(Set<ShareRule> shareRules) {
        this.shareRules = getShareRules();
        this.shareRules.clear();
        for (ShareRule rule : shareRules) {
            rule.setAgreement(this);
            this.shareRules.add(rule);
        }
        // this.shareRules = shareRules;
    }
}