package com.saic.uicds.core.infrastructure.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * The ShareRule data model.
 * 
 * @ssdd
 */
@Entity
// @Embeddable
public class ShareRule {

    @Id
    @Column(name = "SHARE_RULE_ID")
    @GeneratedValue
    private Integer id;

    private boolean enabled;
    private String ruleID;

    // @ManyToOne(optional = true)
    @ManyToOne(targetEntity = Agreement.class)
    @JoinColumn(name = "AGREEMENT_ID", nullable = false)
    private Agreement agreement;

    // @Embedded
    // @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    // @org.hibernate.annotations.Cascade(value =
    // org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    // private CodeSpaceValueType interestGroup;

    private String interestGroupCodeSpace;
    private String interestGroupLabel;
    private String interestGroupValue;

    // @Embedded
    // @OneToMany(mappedBy = "shareRule")
    @OneToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
    @org.hibernate.annotations.Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    Set<CodeSpaceValueType> workProducts;

    public boolean equals(Object obj) {

        ShareRule shareRuleObject = (ShareRule) obj;
        String hash = interestGroupCodeSpace + interestGroupValue;
        String shareRuleObjectHash = shareRuleObject.getInterestGroup().getCodeSpace()
            + shareRuleObject.getInterestGroup().getValue();
        return (hash.equals(shareRuleObjectHash));
    }

    public int hashCode() {

        String hash = interestGroupCodeSpace + interestGroupValue;
        return hash.hashCode();
    }

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

    /**
     * Gets the agreement.
     * 
     * @return the agreement
     * @ssdd
     */
    public Agreement getAgreement() {

        return agreement;
    }

    /**
     * Sets the agreement.
     * 
     * @param agreement the new agreement
     * @ssdd
     */
    public void setAgreement(Agreement agreement) {

        this.agreement = agreement;
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
     * Gets the rule id.
     * 
     * @return the rule id
     * @ssdd
     */
    public String getRuleID() {

        return ruleID;
    }

    /**
     * Sets the rule id.
     * 
     * @param ruleID the new rule id
     * @ssdd
     */
    public void setRuleID(String ruleID) {

        this.ruleID = ruleID;
    }

    /**
     * Gets the interest group.
     * 
     * @return the interest group
     * @ssdd
     */
    public CodeSpaceValueType getInterestGroup() {

        CodeSpaceValueType c = new CodeSpaceValueType();
        c.setCodeSpace(interestGroupCodeSpace);
        c.setLabel(interestGroupLabel);
        c.setValue(interestGroupValue);
        return c;
    }

    /**
     * Sets the interest group.
     * 
     * @param interestGroup the new interest group
     * @ssdd
     */
    public void setInterestGroup(CodeSpaceValueType interestGroup) {

        interestGroupCodeSpace = interestGroup.getCodeSpace();
        interestGroupLabel = interestGroup.getLabel();
        interestGroupValue = interestGroup.getValue();
    }

    /**
     * Gets the work products.
     * 
     * @return the work products
     * @ssdd
     */
    public Set<CodeSpaceValueType> getWorkProducts() {

        if (workProducts == null) {
            workProducts = new HashSet<CodeSpaceValueType>();
        }
        return workProducts;
    }

    /**
     * Sets the work products.
     * 
     * @param workProducts the new work products
     * @ssdd
     */
    public void setWorkProducts(Set<CodeSpaceValueType> workProducts) {

        this.workProducts = getWorkProducts();
        this.workProducts.clear();
        for (CodeSpaceValueType workProduct : workProducts) {
            // workProduct.setShareRule(this);
            this.workProducts.add(workProduct);
        }
        // this.workProducts = workProducts;
    }

}