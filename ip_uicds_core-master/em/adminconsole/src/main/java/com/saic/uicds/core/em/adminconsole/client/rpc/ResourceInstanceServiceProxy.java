package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.saic.uicds.core.em.adminconsole.client.model.ResourceInstanceGWT;

/**
 * ResourceInstanceServiceProxy [RCP service for ResourceInstance]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.client.rcp
 */

public interface ResourceInstanceServiceProxy extends RemoteService {

    /**
     * Creates a new ResourceInstance
     * 
     * @param request
     */
    public ResourceInstanceGWT createResourceInstance(ResourceInstanceGWT request);

    /**
     * Deletes an existing resourceInstance
     * 
     * @param request
     */
    public String deleteResourceInstance(String identifier);

    /**
     * Returns a list of ResourceInstances
     * 
     * @param queryString is ignored
     * @return
     */
    public List<ResourceInstanceGWT> getResourceInstanceList();

}
