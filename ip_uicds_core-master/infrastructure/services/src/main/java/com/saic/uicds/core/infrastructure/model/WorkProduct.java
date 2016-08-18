package com.saic.uicds.core.infrastructure.model;

import gov.ucore.ucore.x20.DigestDocument;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.apache.xmlbeans.XmlObject;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A work product is a typed resource that contains data in one of several standard data formats
 * about some aspect of an incident.
 * 
 * @author topher
 * @ssdd
 */
@Entity
@Table(name = "WORK_PRODUCTS")
/**
 * 
@org.hibernate.annotations.Table(appliesTo="work_products", 
	indexes={
		@org.hibernate.annotations.Index(name="work_products_i1", columnNames={"product_id", "product_type"}),
		})
*/
public class WorkProduct
    implements Serializable {

    private static final long serialVersionUID = -7909684672536211321L;
    private static final String UICDSXmlMimeType = "application/uicds+xml";
    
    @Transient
    private Logger log = LoggerFactory.getLogger(WorkProduct.class);
    
    @SuppressWarnings("unused")
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // start of WorkProduct Metadata
    // Identification
    @Column(name = "PRODUCT_ID")
    private String productID;

    @Column(name = "PRODUCT_TYPE")
    private String productType;

    @Column(name = "PRODUCT_TYPE_VERSION")
    private String productTypeVersion;

    @Column(name = "PRODUCT_VERSION")
    private Integer productVersion;

    @Column(name = "CHECKSUM")
    private String checksum;

    // Properties
    @Column(name = "INTEREST_GROUP_ID")
    @CollectionOfElements(fetch = FetchType.EAGER)
    @IndexColumn(name="wp_ig_id_i1")
    private Set<String> associatedInterestGroupIDs = new HashSet<String>();

    @Column(name = "CREATED")
    private Date createdDate;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "LAST_UPDATED")
    private Date updatedDate;

    @Column(name = "LAST_UPDATED_BY")
    private String updatedBy;

    @Column(name = "KILOBYTES")
    private Integer size;

    @Column(name = "MIMETYPE")
    private String mimeType;

    @Column(name = "ACTIVE")
    private boolean active = true;
    
    @Column(name = "RECORD_VERSION")
    @Version
    private Integer version;
    
    /**
     * Following column definition are specific for SQL DB
     */
    @Column(name="DIGEST", columnDefinition = "LONGTEXT")
    @Lob
    @Type(type="com.saic.uicds.core.dao.hb.DigestDocumentUserType")
    private DigestDocument digest;

    @Column(name="PRODUCT", columnDefinition = "LONGTEXT")
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Type(type="com.saic.uicds.core.dao.hb.XmlObjectUserType")
    private XmlObject product;
    

    /**
     * Instantiates a new work product.
     * 
     * @ssdd
     */
    public WorkProduct() {

        super();
    }

    /**
     * Instantiates a new work product.
     * 
     * @param wp the wp
     * @ssdd
     */
	public WorkProduct(WorkProduct wp) {

		super();

		if (wp != null) {

	        if(wp.getAssociatedInterestGroupIDs() !=null)
	        {
		        for (String associatedIntegroupID : wp.getAssociatedInterestGroupIDs()) 
		        {
		            getAssociatedInterestGroupIDs().add(associatedIntegroupID);
		        }
	        }

	        setChecksum(wp.getChecksum());
	        setCreatedBy(wp.getCreatedBy());
	        setCreatedDate(wp.getCreatedDate());
	        setActive(wp.getActive());
	        setMimeType(wp.getMimeType());
	        // Create a new one for the new instances otherwise it is linked to the current entry
	        if (wp.getDigest() != null) {
	            DigestDocument digest = DigestDocument.Factory.newInstance();
	            digest.setDigest(wp.getDigest().getDigest());
	            setDigest(digest);
	        }
	        setProductID(wp.getProductID());
	        setProductType(wp.getProductType());
	        setProductVersion(wp.getProductVersion());
	        setSize(wp.getSize());
	        setUpdatedBy(wp.getUpdatedBy());
	        setUpdatedDate(wp.getUpdatedDate());

	        setProduct(wp.getProduct().copy());
		}
	}

	public String toString(){
		String str = "{";
/*		
		str += "Id="+this.getId()
				+"\nproductType="+this.getProductType()
		+"\nproductId="+this.getProductID()
		+"\nproductVersion="+this.getProductVersion()
		+"\nproductTypeVersion="+this.getProductTypeVersion()
		+"\nchecksum="+this.getChecksum()
		+"\nactive="+this.getActive()
		+"\nsize="+this.getSize()
		+"\ncreatedDate="+this.getCreatedDate()
		+"\ncreatedBy="+this.getCreatedBy()
		+"\nupdatedDate="+this.getUpdatedDate()
		+"\nupdatedBy="+this.updatedBy
		+"\nproduct=[";
		if (product != null)
			str += product.toString();
		str+="]"
		+"\ndigest=[";
		if (digest != null)
			str += digest.toString();
		str+="]\n}";
*/		
		str += "Id="+this.getId()
		+", productId="+this.getProductID()
		+", productVersion="+this.getProductVersion()
		+", checksum="+this.getChecksum()
		+", active="+this.getActive()
		+", version="+this.version
		+"}";
		
		return str;
	}
	
    /**
     * Gets the associated interest group ids.
     * 
     * @return the associated interest group ids
     * @ssdd
     */
    public Set<String> getAssociatedInterestGroupIDs() {

        return associatedInterestGroupIDs;
    }

    /**
     * Gets the checksum.
     * 
     * @return the checksum
     * @ssdd
     */
    public String getChecksum() {

        return checksum;
    }

    /**
     * Gets the created by.
     * 
     * @return the created by
     * @ssdd
     */
    public String getCreatedBy() {

        return createdBy;
    }

    /**
     * Gets the created date.
     * 
     * @return the created date
     * @ssdd
     */
    public Date getCreatedDate() {

        return createdDate;
    }

    /**
     * Gets the active.
     * 
     * @return the active
     * @ssdd
     */
    public boolean getActive() {

        return active;
    }

    /**
     * Gets the digest.
     * 
     * @return the digest
     * @ssdd
     */
    public DigestDocument getDigest() {

        return digest;
    }

    /**
     * Gets the first associated interest group id.
     * 
     * @return the first associated interest group id
     * @ssdd
     */
    public String getFirstAssociatedInterestGroupID() {

        if (associatedInterestGroupIDs != null && associatedInterestGroupIDs.size() > 0) {
            String[] intererstGroupIDs = new String[associatedInterestGroupIDs.size()];
            intererstGroupIDs = (String[]) associatedInterestGroupIDs.toArray(intererstGroupIDs);
            return intererstGroupIDs[0];
        } else {
            return null;        	
        }
    }

    /**
     * Returns the the primary key of the WorkProduct.
     * 
     * @return the id
     * @ssdd
     */
    public Integer getId() {

        return id;
    }

    /**
     * Gets the mime type.
     * 
     * @return the mime type
     * @ssdd
     */
    public String getMimeType() {

        return mimeType;
    }

    /**
     * Gets the product.
     * 
     * @return the product
     * @ssdd
     */
    public XmlObject getProduct() {

        return product;
    }

    /**
     * Gets the product id.
     * 
     * @return the product id
     * @ssdd
     */
    public String getProductID() {

        return productID;
    }

    /**
     * Gets the product type.
     * 
     * @return the product type
     * @ssdd
     */
    public String getProductType() {

        return productType;
    }

    /**
     * Gets the product type version.
     * 
     * @return the product type version
     * @ssdd
     */
    public String getProductTypeVersion() {

        return productTypeVersion;
    }

    /**
     * Gets the product version.
     * 
     * @return the product version
     * @ssdd
     */
    public Integer getProductVersion() {

        return productVersion;
    }

    /**
     * Gets the size.
     * 
     * @return the size
     * @ssdd
     */
    public Integer getSize() {

        return size;
    }

    /**
     * Gets the updated by.
     * 
     * @return the updated by
     * @ssdd
     */
    public String getUpdatedBy() {

        return updatedBy;
    }

    /**
     * Gets the updated date.
     * 
     * @return the updated date
     * @ssdd
     */
    public Date getUpdatedDate() {

        return updatedDate;
    }

    /**
     * Checks if is active.
     * 
     * @return true, if is active
     * @ssdd
     */
    public boolean isActive() {

        return active;
    }

    /**
     * Checks if is uICDS xml.
     * 
     * @return true, if is uICDS xml
     * @ssdd
     */
    public boolean isUICDSXml() {

        return this.mimeType.equals(UICDSXmlMimeType);
    }

    /**
     * Sets the active.
     * 
     * @param active the new active
     * @ssdd
     */
    public void setActive(boolean active) {

        this.active = active;
    }

    /**
     * Sets the associated interest group i ds.
     * 
     * @param associatedInterestGroupIDs the new associated interest group i ds
     * @ssdd
     */
    public void setAssociatedInterestGroupIDs(Set<String> associatedInterestGroupIDs) {

        this.associatedInterestGroupIDs = associatedInterestGroupIDs;
    }

    /**
     * Sets the checksum.
     * 
     * @param checksum the new checksum
     * @ssdd
     */
    public void setChecksum(String checksum) {

        this.checksum = checksum;
    }

    /**
     * Sets the created by.
     * 
     * @param createdBy the new created by
     * @ssdd
     */
    public void setCreatedBy(String createdBy) {

        this.createdBy = createdBy;
    }

    /**
     * Sets the created date.
     * 
     * @param createdDate the new created date
     * @ssdd
     */
    public void setCreatedDate(Date createdDate) {

        this.createdDate = createdDate;
    }

    /**
     * Sets the default mime type.
     * 
     * @ssdd
     */
    public void setDefaultMimeType() {

        this.mimeType = UICDSXmlMimeType;
    }

    /**
     * Sets the digest.
     * 
     * @param digest the new digest
     * @ssdd
     */
    public void setDigest(DigestDocument digest) {

        this.digest = digest;
    }

    /**
     * Sets the id of the WorkProduct.
     * 
     * @param id the id
     * @ssdd
     */
    public void setId(Integer id) {

        this.id = id;
    }

	/**
     * Sets the mime type.
     * 
     * @param mimeType the new mime type
     * @ssdd
     */
    public void setMimeType(String mimeType) {

        this.mimeType = mimeType;
    }

    /**
     * Sets the product.
     * 
     * @param product the new product
     * @ssdd
     */
    public void setProduct(XmlObject product) {

        this.product = product;
    }

    /**
     * Sets the product id.
     * 
     * @param productID the new product id
     * @ssdd
     */
    public void setProductID(String productID) {

        this.productID = productID;
    }

    /**
     * Sets the product type.
     * 
     * @param productType the new product type
     * @ssdd
     */
    public void setProductType(String productType) {

        this.productType = productType;
    }

    /**
     * Sets the product type version.
     * 
     * @param productTypeVersion the new product type version
     * @ssdd
     */
    public void setProductTypeVersion(String productTypeVersion) {

        this.productTypeVersion = productTypeVersion;
    }

    /**
     * Sets the product version.
     * 
     * @param productVersion the new product version
     * @ssdd
     */
    public void setProductVersion(Integer productVersion) {

        this.productVersion = productVersion;
    }

    /**
     * Sets the size.
     * 
     * @param size the new size
     * @ssdd
     */
    public void setSize(Integer size) {

        this.size = size;
    }

    /**
     * Sets the updated by.
     * 
     * @param updatedBy the new updated by
     * @ssdd
     */
    public void setUpdatedBy(String updatedBy) {

        this.updatedBy = updatedBy;
    }

    /**
     * Sets the updated date.
     * 
     * @param updatedDate the new updated date
     * @ssdd
     */
    public void setUpdatedDate(Date updatedDate) {

        this.updatedDate = updatedDate;
    }

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
}
