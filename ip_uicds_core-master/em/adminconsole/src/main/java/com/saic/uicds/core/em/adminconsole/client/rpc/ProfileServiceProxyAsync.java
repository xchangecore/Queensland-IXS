package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;

public interface ProfileServiceProxyAsync {

    /**
     * Adds an interest to profile
     * 
     * @param request
     */
    public void addInterest(String identifier, String topic, AsyncCallback<String> callback);

    public void createProfile(ProfileGWT request, AsyncCallback<ProfileGWT> callback);

    /**
     * Deletes an existing profile
     * 
     * @param request
     */
    public void deleteProfile(String profileID, AsyncCallback<String> callback);

    /**
     * Returns the profile associated with a UICDS entity
     * 
     * @param entityID
     * @return
     */
    void getProfile(String entityID, AsyncCallback<ProfileGWT> callback);

    /**
     * Returns a list of profiles whose ids partially match the supplied queryString.
     * 
     * @param queryString is ignored
     * @return
     */
    void getProfileList(AsyncCallback<List<ProfileGWT>> callback);

    void getProfileListChildren(ProfileGWT profile, AsyncCallback<List<ProfileGWT>> callback);

    /**
     * Deletes an existing interst from profile
     * 
     * @param request
     */
    public void removeInterest(String identifier, String topic, AsyncCallback<String> callback);

    /**
     * Update a profile
     * 
     * @param request
     * @return
     */
    public void updateProfile(ProfileGWT request, AsyncCallback<ProfileGWT> callback);
}
