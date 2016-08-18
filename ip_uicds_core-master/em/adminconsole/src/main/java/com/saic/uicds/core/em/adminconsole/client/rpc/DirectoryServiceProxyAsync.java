package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.saic.uicds.core.em.adminconsole.client.model.CoreConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ExternalToolConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ServiceConfigGWT;

public interface DirectoryServiceProxyAsync {

    /**
     * Retrieves a list of cores, their addresses, and online status .
     * 
     * @return
     */
    void getCoreList(AsyncCallback<List<CoreConfigGWT>> callback);

    void getCoreListChildren(String coreName, CoreConfigGWT service,
        AsyncCallback<List<CoreConfigGWT>> callback);

    /**
     * Retrieves the name of local UICDS core
     * 
     * @return
     */
    void getCoreName(AsyncCallback<String> callback);

    /**
     * Retrieves a list of external tools and their WPS interface URLs that are registered with the
     * UICDS core.
     * 
     * @param coreName
     * @return
     */
    void getExternalToolList(String coreName, AsyncCallback<List<ExternalToolConfigGWT>> callback);

    /**
     * Retrieves a list of external data sources
     * 
     * @param coreName
     * @return void getExternalDataSourceList(String coreName,
     *         AsyncCallback<List<ExternalDataSourceConfigGWT>> callback);
     */

    /**
     * Retrieves a list of users (and their online status) that are registered with the UICDS core.
     * 
     * @param coreName
     * @return
     */
    void getProfileList(String coreName, AsyncCallback<List<ProfileGWT>> callback);

    /**
     * returns a list published work product types
     * 
     * @return workProductTypeList
     */
    void getPublishedProductTypeList(AsyncCallback<String[]> callback);

    void getRequestUrl(AsyncCallback<String> callback);

    /**
     * Retrieves a list of registered services that are registered with the UICDS core.
     * 
     * @param coreName
     * @return
     */
    void getServiceList(String coreName, AsyncCallback<List<ServiceConfigGWT>> callback);

    void getServiceListChildren(String coreName, ServiceConfigGWT service,
        AsyncCallback<List<ServiceConfigGWT>> callback);

    void getConfigurationMap(AsyncCallback<Map<String, String>> asyncCallback);

}
