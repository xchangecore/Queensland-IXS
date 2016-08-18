package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;

public class ExternalDataSourceConfigGWT implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -709423650907037610L;

    private Integer id;
    private String urn;
    private String coreName;

    public ExternalDataSourceConfigGWT(String urn, String coreName) {

        setUrn(urn);
        setCoreName(coreName);
    }

    public String getCoreName() {

        return this.coreName;
    }

    public Integer getId() {

        return this.id;
    }

    public String getUrn() {

        return this.urn;
    }

    public void setCoreName(String coreName) {

        this.coreName = coreName;
    }

    public void setId(Integer id) {

        this.id = id;
    }

    public void setUrn(String urn) {

        this.urn = urn;
    }

}
