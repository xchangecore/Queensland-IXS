package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;

import com.saic.uicds.core.em.adminconsole.client.model.CoreConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ExternalToolConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ServiceConfigGWT;

public interface DirectoryServiceProxy extends RemoteService {

    /**
     * Retrieves a list of cores, their addresses, and online status .
     * 
     * @return
     */
    public List<CoreConfigGWT> getCoreList();

    public List<CoreConfigGWT> getCoreListChildren(String coreName, CoreConfigGWT service);

    /**
     * Retrieves the name of local UICDS core
     * 
     * @return
     */
    public String getCoreName();

    /**
     * Retrieves a list of external tools and their WPS interface URLs that are registered with the
     * UICDS core.
     * 
     * @param coreName
     * @return
     */
    public List<ExternalToolConfigGWT> getExternalToolList(String coreName);

    /**
     * Retrieves a list of users (and their online status) that are registered with the UICDS core.
     * 
     * @param coreName
     * @return
     */
    public List<ProfileGWT> getProfileList(String coreName);

    /**
     * Retrieves a list of external data sources
     * 
     * @param coreName
     * @return public List<ExternalDataSourceConfigGWT> getExternalDataSourceList(String coreName);
     */

    /**
     * returns a list published work product types
     * 
     * @return workProductTypeList
     */
    public String[] getPublishedProductTypeList();

    public String getRequestUrl();

    /**
     * Retrieves a list of registered services that are registered with the UICDS core.
     * 
     * @param coreName
     * @return
     */
    public List<ServiceConfigGWT> getServiceList(String coreName);

    public List<ServiceConfigGWT> getServiceListChildren(String coreName, ServiceConfigGWT service);
    
    public Map<String, String> getConfigurationMap();

}
