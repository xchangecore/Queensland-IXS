package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;
import java.util.Date;

public class WorkProductGWT implements Serializable {

    private static final long serialVersionUID = -7909684672536211321L;

    private static final String UICDSXmlMimeType = "application/uicds+xml";

    private boolean root = false;
    private boolean leaf = true;
    private boolean digest = false;
    private Integer id;
    private String incidentID;
    private String productID;
    private String productType;
    private String mimeType;
    private String product;
    private String productHtml;
    private String submitterID;
    private Date publishedDate;

    private boolean closed;

    public WorkProductGWT() {

        super();
        setClosed(false);
    }

    /**
     * Returns the the primary key of the WorkProduct.
     * 
     * @return
     */
    public Integer getId() {

        return id;
    }

    public String getIncidentID() {

        return incidentID;
    }

    public String getMimeType() {

        return mimeType;
    }

    public String getProduct() {

        return product;
    }

    public String getProductHtml() {

        return productHtml;
    }

    public String getProductID() {

        return productID;
    }

    public String getProductType() {

        return productType;
    }

    /**
     * Returns the date/time the work product was published.
     * 
     * @return
     */
    public Date getPublishedDate() {

        return publishedDate;
    }

    public String getSubmitterID() {

        return submitterID;
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

    public boolean isUICDSXml() {

        return this.mimeType.equals(UICDSXmlMimeType);
    }

    public void setClosed(boolean closed) {

        this.closed = closed;
    }

    public void setDefaultMimeType() {

        this.mimeType = UICDSXmlMimeType;
    }

    /**
     * Sets the id of the WorkProduct.
     * 
     * @param id
     */
    public void setId(Integer id) {

        this.id = id;
    }

    public void setIncidentID(String incidentID) {

        this.incidentID = incidentID;
    }

    public void setLeaf(boolean leaf) {

        this.leaf = leaf;
    }

    public void setMimeType(String mimeType) {

        this.mimeType = mimeType;
    }

    public void setProduct(String product) {

        this.product = product;
    }

    public void setProductHtml(String productHtml) {

        this.productHtml = productHtml;
    }

    public void setProductID(String productID) {

        this.productID = productID;
    }

    public void setProductType(String productType) {

        this.productType = productType;
    }

    /**
     * Sets the publication date/time of the WorkProduct.
     * 
     * @param published
     */
    public void setPublishedDate(Date published) {

        this.publishedDate = published;
    }

    public boolean hasDigest() {
        return digest;
    }

    public void setDigest(boolean digest) {
        this.digest = digest;
    }

    public void setRoot(boolean root) {

        this.root = root;
    }

    public void setSubmitterID(String submitterID) {

        this.submitterID = submitterID;
    }
}
