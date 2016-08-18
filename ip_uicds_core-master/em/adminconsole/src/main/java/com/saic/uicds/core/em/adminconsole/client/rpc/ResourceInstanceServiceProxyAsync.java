package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.saic.uicds.core.em.adminconsole.client.model.ResourceInstanceGWT;

/**
 * ResourceInstanceServiceProxyAsync [Asynchronous calls to ResourceInstanceService]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.client.rcp
 */

public interface ResourceInstanceServiceProxyAsync extends RemoteService{

    /**
     * Creates a new ResourceInstance
     * 
     * @param request
     */
    void createResourceInstance(ResourceInstanceGWT request, AsyncCallback<ResourceInstanceGWT> callback);

    /**
     * Deletes an existing ResourceInstance
     * 
     * @param request
     */
    void deleteResourceInstance(String identifier, AsyncCallback<String> callback);

    /**
     * Returns a list of ResourceInstances
     * 
     * @param queryString is ignored
     * @return
     */
    void getResourceInstanceList(AsyncCallback<List<ResourceInstanceGWT>> callback);

}
