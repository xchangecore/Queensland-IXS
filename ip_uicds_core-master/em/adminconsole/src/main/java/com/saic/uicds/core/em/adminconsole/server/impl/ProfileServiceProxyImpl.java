package com.saic.uicds.core.em.adminconsole.server.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.uicds.resourceProfileService.Interest;
import org.uicds.resourceProfileService.ResourceProfile;
import org.uicds.resourceProfileService.ResourceProfileListType;
import org.uicds.resourceProfileService.ResourceProfile.Interests;
import org.uicds.resourceProfileService.ResourceProfile.ResourceTyping;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.saic.precis.x2009.x06.base.CodespaceValueType;
import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;
import com.saic.uicds.core.em.adminconsole.client.rpc.ProfileServiceProxy;
import com.saic.uicds.core.infrastructure.model.ResourceProfileModel;
import com.saic.uicds.core.infrastructure.service.ResourceProfileService;

public class ProfileServiceProxyImpl extends RemoteServiceServlet implements ProfileServiceProxy {

    Logger log = LoggerFactory.getLogger(this.getClass());

    private ResourceProfileService rpService = null;

    /**
     * Serializable
     */
    private static final long serialVersionUID = -2586988777891299374L;

    public String addInterest(String identifier, String topic) {

        log.debug("Adding interest topic: " + topic);
        IdentifierType identifierType = IdentifierType.Factory.newInstance();
        identifierType.setStringValue(identifier);
        Interest interest = Interest.Factory.newInstance();
        interest.setTopicExpression(topic);
        getResourceProfileService().addInterest(identifierType, interest);
        return "true";
    }

    public ProfileGWT createProfile(ProfileGWT request) {

        ResourceProfile profile = ResourceProfile.Factory.newInstance();
        profile.addNewID().setStringValue(request.getIdentifier());
        profile.setDescription(request.getDescription());
        if (request.getResourceTyping() != null && request.getResourceTyping().size() > 0) {
            ResourceTyping typing = profile.addNewResourceTyping();
            for (String type : request.getResourceTyping().keySet()) {
                log.debug("Add resource type: " + type);
                log.debug("Value of type: " + request.getResourceTyping().get(type));
                CodespaceValueType value = typing.addNewType();
                value.setCodespace("http://www.fema.gov/emergency/nims");
                value.setLabel(type);
                value.setStringValue(request.getResourceTyping().get(type));
            }
        }
        if (request.getInterests() != null && request.getInterests().size() > 0) {
            Interests interests = profile.addNewInterests();
            for (String interestTopic : request.getInterests()) {
                interests.addNewInterest().setTopicExpression(interestTopic);
            }
        }
        ResourceProfileModel model = getResourceProfileService().createProfile(profile);

        ProfileGWT returnedProfile = new ProfileGWT();
        returnedProfile.setIdentifier(model.getIdentifier());
        returnedProfile.setDescription(model.getDescription());
        // returnedProfile.setResourceTyping(model.getResourceTyping());
        // returnedProfile.setSubscriptions(model.getInterests());
        return request;
    }

    public String deleteProfile(String identifier) {

        log.debug("Removing profile w/ identifier: " + identifier);
        IdentifierType identifierType = IdentifierType.Factory.newInstance();
        identifierType.setStringValue(identifier);
        getResourceProfileService().deleteProfile(identifierType);
        return "true";
    }

    public ProfileGWT getProfile(String entityID) {

        IdentifierType identifier = IdentifierType.Factory.newInstance();
        identifier.setStringValue(entityID);
        ResourceProfileModel profile = getResourceProfileService().getProfile(identifier);
        ProfileGWT dataGWT = new ProfileGWT();
        dataGWT.setXML(Util.getPrettyXmlFromString(profile.toString()));
        dataGWT.setRefName(profile.getIdentifier().toString());
        dataGWT.setIdentifier(profile.getIdentifier().toString());
        return dataGWT;
    }

    public List<ProfileGWT> getProfileList() {

        // FIXME:: The profile service ignores its String parameter, for now...
        ResourceProfileListType list = getResourceProfileService().getProfileList("");
        ResourceProfile[] profiles = list.getResourceProfileArray();
        List<ProfileGWT> listGWT = new ArrayList<ProfileGWT>(profiles.length);
        for (ResourceProfile value : profiles) {
            ProfileGWT temp = new ProfileGWT();
            temp.setXML(value.toString());
            temp.setRefName(value.getID().getStringValue());
            temp.setIdentifier(value.getID().getStringValue());
            temp.setDescription(value.getDescription());
            if (value.getResourceTyping() != null
                && value.getResourceTyping().getTypeArray().length > 0) {
                Map<String, String> typing = new HashMap<String, String>();
                for (CodespaceValueType type : value.getResourceTyping().getTypeArray()) {
                    typing.put(type.getLabel(), type.getStringValue());
                }
                temp.setResourceTyping(typing);
            }
            if (value.getInterests() != null && value.getInterests().getInterestArray().length > 0) {
                // just supporting topic Expressions for interests currently
                Set<String> interests = new HashSet<String>();
                for (Interest interest : value.getInterests().getInterestArray()) {
                    interests.add(interest.getTopicExpression());
                }
                temp.setInterests(interests);
            }
            // if (value.getInterests().getInterestArray().length > 0) {
            // Set<InterestElementGWT> interestsGWT = new HashSet<InterestElementGWT>();
            // for (Interest interest : value.getInterests().getInterestArray()) {
            // InterestElementGWT interestGWT = new InterestElementGWT();
            // interestGWT.setTopicExpression(interest.getTopicExpression());
            // interestsGWT.add(interestGWT);
            // }
            // temp.setSubscriptions(interestsGWT);
            // }
            listGWT.add(temp);
        }
        return listGWT;
    }

    public List<ProfileGWT> getProfileListChildren(ProfileGWT profile) {

        if (!profile.isRoot()) {
            return getProfileList();
        }
        List<ProfileGWT> listGWT = new ArrayList<ProfileGWT>(1);
        profile.setRoot(false);
        listGWT.add(profile);
        return listGWT;
    }

    private ResourceProfileService getResourceProfileService() {

        if (this.rpService == null) {
            boolean loaded = loadRPService();
        }
        return this.rpService;
    }

    private boolean loadRPService() {

        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        this.rpService = (ResourceProfileService) springContext.getBean("resourceProfileService");
        if (rpService == null) {
            throw new RuntimeException("Unable to load ResourceProfileService!");
        } else {
            return true;
        }
    }

    public String removeInterest(String identifier, String topic) {

        log.debug("Removing interest topic: " + topic);
        IdentifierType identifierType = IdentifierType.Factory.newInstance();
        identifierType.setStringValue(identifier);
        Interest interest = Interest.Factory.newInstance();
        interest.setTopicExpression(topic);
        getResourceProfileService().removeInterest(identifierType, interest);
        return "true";
    }

    public ProfileGWT updateProfile(ProfileGWT request) {

        ResourceProfile profile = ResourceProfile.Factory.newInstance();
        profile.addNewID().setStringValue(request.getIdentifier());
        profile.setDescription(request.getDescription());

        ResourceTyping typing = profile.addNewResourceTyping();
        for (String type : request.getResourceTyping().keySet()) {
            log.debug("Add resource type: " + type);
            log.debug("Value of type: " + request.getResourceTyping().get(type));
            CodespaceValueType value = typing.addNewType();
            value.setCodespace("http://www.fema.gov/emergency/nims");
            value.setLabel(type);
            value.setStringValue(request.getResourceTyping().get(type));
        }
        // if (request.getInterests().size() > 0) {
        // Interests interests = profile.addNewInterests();
        // for (String interestTopic : request.getInterests()) {
        // interests.addNewInterest().setTopicExpression(interestTopic);
        // }
        // }
        ResourceProfileModel model = getResourceProfileService().updateProfile(profile);

        ProfileGWT returnedProfile = new ProfileGWT();
        returnedProfile.setIdentifier(model.getIdentifier());
        returnedProfile.setDescription(model.getDescription());
        // returnedProfile.setResourceTyping(model.getResourceTyping());
        // returnedProfile.setSubscriptions(model.getInterests());
        return request;
    }

}
