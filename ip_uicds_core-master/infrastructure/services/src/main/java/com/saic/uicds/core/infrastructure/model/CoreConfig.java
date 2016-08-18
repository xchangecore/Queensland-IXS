package com.saic.uicds.core.infrastructure.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The CoreConfig data model.
 * 
 * @ssdd
 */
@Entity
@Table(name = "CORE_CONFIG")
public class CoreConfig {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "URL")
    private String url;

    @Column(name = "LOCAL_CORE")
    private boolean localCore;

    @Column(name = "ONLINE_STATUS")
    private String onlineStatus;

    public CoreConfig() {

    }

    /**
     * Instantiates a new core config.
     * 
     * @param name the name
     * @param url the url
     * @param onlineStatus the online status
     * @ssdd
     */
    public CoreConfig(String name, String url, String onlineStatus) {
        this.name = name;
        this.url = url;
        this.onlineStatus = onlineStatus;
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
     * Gets the id.
     * 
     * @return the id
     * @ssdd
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Sets the name.
     * 
     * @param name the new name
     * @ssdd
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     * @ssdd
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the url.
     * 
     * @param url the new url
     * @ssdd
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the url.
     * 
     * @return the url
     * @ssdd
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Sets the online status.
     * 
     * @param onlineStatus the new online status
     * @ssdd
     */
    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    /**
     * Gets the online status.
     * 
     * @return the online status
     * @ssdd
     */
    public String getOnlineStatus() {
        return this.onlineStatus;
    }

    /**
     * Sets the local core.
     * 
     * @param localCore the new local core
     * @ssdd
     */
    public void setLocalCore(boolean localCore) {
        this.localCore = localCore;
    }

    /**
     * Gets the local core.
     * 
     * @return the local core
     * @ssdd
     */
    public boolean getLocalCore() {
        return this.localCore;
    }
}
