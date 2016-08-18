package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;

public interface LoggingServiceProxyAsync extends RemoteService{

    /**
     * Register CoreId
     * 
     * @param request
     */
    public void registerCoreId(List<String> jidList, AsyncCallback<List<String>> callback);
    
    public void unRegisterCoreId(List<String> jidList, AsyncCallback<List<String>> callback);

    public void getRegisteredCoreIds(AsyncCallback<List<String>> callback);
    
    public void setLocalCoreJid(String localCoreLid, AsyncCallback<String> callback);

    public void updateComponentHistory(AsyncCallback<Map<String,List<String>>> callback);

    public void sendHistroy(boolean send, AsyncCallback<String> asyncCallback);

    public void getConnectionInfo(AsyncCallback<Map<String, String>> asyncCallback);

    public void setConnectionInfo(Map<String, String> connectionInfo,
            AsyncCallback<String> asyncCallback);

    public void getAllJids(AsyncCallback<List<String>> asyncCallback);

    public void unregisterLocalJid(String connectionJid, boolean remote, AsyncCallback<String> asyncCallback);


    }
