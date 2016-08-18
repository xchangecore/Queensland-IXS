package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;

public interface ProfileServiceProxy extends RemoteService {

    /**
     * Adds an interest to profile
     * 
     * @param request
     */
    public String addInterest(String identifier, String topic);

    public ProfileGWT createProfile(ProfileGWT request);

    /**
     * Deletes an existing profile
     * 
     * @param request
     */
    public String deleteProfile(String identifier);

    /**
     * Returns the profile associated with a UICDS entity
     * 
     * @param entityID
     * @return
     */
    public ProfileGWT getProfile(String entityID);

    /**
     * Returns a list of profiles whose ids partially match the supplied queryString.
     * 
     * @param queryString is ignored
     * @return
     */
    public List<ProfileGWT> getProfileList();

    public List<ProfileGWT> getProfileListChildren(ProfileGWT profile);

    /**
     * Deletes an existing interst from profile
     * 
     * @param request
     */
    public String removeInterest(String identifier, String topic);

    /**
     * Update a profile
     * 
     * @param request
     * @return
     */
    public ProfileGWT updateProfile(ProfileGWT request);
}
