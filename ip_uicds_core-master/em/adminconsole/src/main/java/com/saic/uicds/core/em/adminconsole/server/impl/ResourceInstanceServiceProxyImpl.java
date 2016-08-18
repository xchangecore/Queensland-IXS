package com.saic.uicds.core.em.adminconsole.server.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.uicds.resourceInstanceService.ResourceInstance;
import org.uicds.resourceInstanceService.ResourceInstance.ProfileIDs;
import org.uicds.resourceInstanceService.ResourceInstance.SourceIdentification;
import org.uicds.resourceInstanceService.ResourceInstanceListType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.uicds.core.em.adminconsole.client.model.ResourceInstanceGWT;
import com.saic.uicds.core.em.adminconsole.client.rpc.ResourceInstanceServiceProxy;
import com.saic.uicds.core.infrastructure.exceptions.ResourceInstanceDoesNotExist;
import com.saic.uicds.core.infrastructure.exceptions.ResourceProfileDoesNotExist;
import com.saic.uicds.core.infrastructure.model.ResourceInstanceModel;
import com.saic.uicds.core.infrastructure.service.ResourceInstanceService;

/**
 * ResourceInstanceServiceProxyImpl [Implements creation, deletes, and list access of
 * ResourceInstance]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.server.impl
 */

public class ResourceInstanceServiceProxyImpl extends RemoteServiceServlet implements
        ResourceInstanceServiceProxy {

    /**
     * 
     */
    private static final long serialVersionUID = -4676678324697462142L;

    Logger log = LoggerFactory.getLogger(this.getClass());

    private ResourceInstanceService resourceInstanceService = null;

    public ResourceInstanceGWT createResourceInstance(ResourceInstanceGWT request) {
        ResourceInstance resourceInstance = ResourceInstance.Factory.newInstance();
        //  Resource identifier
        IdentifierType identifier = resourceInstance.addNewID();
        identifier.setStringValue(request.getIdentifier());

        // SourceIdentification
        SourceIdentification sourceIdentification = resourceInstance.addNewSourceIdentification();
        IdentifierType localResourceId = sourceIdentification.addNewLocalResourceID();
        localResourceId.setLabel(request.getLocalResourceId());
        localResourceId.setStringValue(request.getLocalResourceId());

        // Profiles
        ProfileIDs profileIds = resourceInstance.addNewProfileIDs();
        IdentifierType profile = profileIds.addNewProfileID();
        if (request.getProfiles() != null && request.getProfiles().size() > 0) {
            String id = request.getProfiles().iterator().next();
            profile.setLabel(id);
            profile.setStringValue(id);
        }
        resourceInstance.setProfileIDs(profileIds);

        ResourceInstanceModel model = null;
        try {
            // create new ResourceInstance by registering to ResourceInstanceService
            model = getResourceInstanceService().register(identifier, localResourceId, profile);
        } catch (ResourceProfileDoesNotExist e) {
            log.error("ResoureceProfile:" + profile.getStringValue() + "DoesNotExist");
        }

        ResourceInstanceGWT returnedResource = new ResourceInstanceGWT();
        returnedResource.setIdentifier(model.getIdentifier());
        return request;
    }

    public String deleteResourceInstance(String identifier) {

        log.debug("Removing resource w/ identifier: " + identifier);
        IdentifierType identifierType = IdentifierType.Factory.newInstance();
        identifierType.setStringValue(identifier);
        try {
            // delete ResourceInstance by unregistering with ResourceInstance service 
            getResourceInstanceService().unregister(identifierType);
        } catch (ResourceInstanceDoesNotExist e) {
            e.printStackTrace();
        }
        return "true";
    }

    public List<ResourceInstanceGWT> getResourceInstanceList() {

        ResourceInstanceListType list = getResourceInstanceService().getResourceInstanceList("");
        ResourceInstance[] resources = list.getResourceInstanceArray();
        List<ResourceInstanceGWT> listGWT = new ArrayList<ResourceInstanceGWT>(resources.length);
        for (ResourceInstance resource : resources) {
            
            ResourceInstanceGWT temp = new ResourceInstanceGWT();
            // identifier
            temp.setIdentifier(resource.getID().getStringValue());
            
            // localResourceId
            if (resource.getSourceIdentification() != null) {
                temp.setLocalResourceId(resource.getSourceIdentification().getLocalResourceID()
                        .getStringValue());
            }
            
            // ProfileId
            if (resource.getProfileIDs() != null
                    && resource.getProfileIDs().getProfileIDArray().length > 0) {
                Set<String> profilesSet = new HashSet<String>();
                for (IdentifierType profile : resource.getProfileIDs().getProfileIDArray()) {
                    profilesSet.add(profile.getStringValue());
                }
                temp.setProfiles(profilesSet);
            }
            temp.setXML(resource.toString());
            listGWT.add(temp);
        }
        return listGWT;
    }

    private ResourceInstanceService getResourceInstanceService() {
        if (this.resourceInstanceService == null) {
            loadRIService();
        }
        return this.resourceInstanceService;
    }

    private boolean loadRIService() {
        WebApplicationContext springContext = WebApplicationContextUtils
                .getWebApplicationContext(this.getServletContext());
        this.resourceInstanceService = (ResourceInstanceService) springContext
                .getBean("resourceInstanceService");
        if (resourceInstanceService == null) {
            throw new RuntimeException("Unable to load ResourceInstanceService!");
        } else {
            return true;
        }
    }

    public ResourceInstanceGWT updateResourceInstance(ResourceInstanceGWT request) {
        // TODO: Not completely implemented
        ResourceInstanceListType list = getResourceInstanceService().getResourceInstanceList("");
        ResourceInstance[] resources = list.getResourceInstanceArray();
        ResourceInstance resourceInstance = null;
        for (ResourceInstance resource : resources) {
            if (resource.getID().getStringValue().equals(request.getIdentifier())) {
                resourceInstance = resource;
            }
        }
        resourceInstance.unsetProfileIDs();

        // SourceIdentification
        SourceIdentification sourceIdentification = resourceInstance.addNewSourceIdentification();
        IdentifierType localResourceId = sourceIdentification.addNewLocalResourceID();
        localResourceId.setLabel(request.getLocalResourceId());
        localResourceId.setStringValue(request.getLocalResourceId());

        // Profiles
        ProfileIDs profileIds = resourceInstance.addNewProfileIDs();
        for (String id : request.getProfiles()) {
            IdentifierType profile = profileIds.addNewProfileID();
            profile.setLabel(id);
            profile.setStringValue(id);
        }
        resourceInstance.setProfileIDs(profileIds);

        getResourceInstanceService().updateEndpoint(null, null, true);

        ResourceInstanceGWT returnedResource = new ResourceInstanceGWT();
        returnedResource.setIdentifier(resourceInstance.getID().getStringValue());
        return request;
    }

}
