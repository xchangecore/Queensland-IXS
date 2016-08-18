package com.saic.uicds.core.em.adminconsole.client.rpc;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;

public interface LoggingServiceProxy extends RemoteService {

    /**
     * Register CoreId
     * 
     * @param request
     */
    public List<String> registerCoreId(List<String> jidList);

    public List<String> unRegisterCoreId(List<String> jidList);

    public List<String> getRegisteredCoreIds();

    public String setLocalCoreJid(String localCoreJid);
    
    public Map<String,List<String>> updateComponentHistory();
    
    public String sendHistroy(boolean send);
    
    public Map<String, String> getConnectionInfo();
    
    public String setConnectionInfo(Map<String, String> connectionInfo);
    
    public List<String> getAllJids();
    
    public String unregisterLocalJid(String connectionJid, boolean remote);
}
