package com.saic.uicds.core.em.adminconsole.server.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.saic.uicds.core.em.adminconsole.client.rpc.LoggingServiceProxy;
import com.saic.uicds.core.infrastructure.service.LoggingService;
import com.saic.uicds.core.infrastructure.service.impl.LoggingServiceImpl;

/**
 * ResourceInstanceServiceProxyImpl [Implements creation, deletes, and list access of
 * ResourceInstance]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.server.impl
 */

public class LoggingServiceProxyImpl extends RemoteServiceServlet implements
        LoggingServiceProxy {

    /**
     * 
     */
    private static final long serialVersionUID = -7479399369758984481L;

    Logger log = LoggerFactory.getLogger(this.getClass());
    
    private LoggingService loggingService=null;
    
    private Map<String,String> connectionInfo=new HashMap<String, String>();

    public Map<String, String> getConnectionInfo() {
        return connectionInfo;
    }

    public String setConnectionInfo(Map<String, String> connectionInfo) {
        this.connectionInfo.putAll(connectionInfo);
        return null;
    }

    public List<String> registerCoreId(List<String> jidList) {
        LoggingServiceImpl loggingServiceImpl = (LoggingServiceImpl) getLoggingService();
        loggingServiceImpl.setRegisteredJidList(jidList);
        loggingServiceImpl.updateEacUsersFile();
        loggingServiceImpl.sendAcknowledgementToJids(jidList);
        return loggingServiceImpl.getRegisteredJidList();
    }

    public List<String> getRegisteredCoreIds() {
        LoggingServiceImpl loggingServiceImpl = (LoggingServiceImpl) getLoggingService();
        return loggingServiceImpl.getRegisteredJidList();
    }
    
    private LoggingService getLoggingService() {
        if (this.loggingService == null) {
            loadLoggingService();
        }
        return this.loggingService;
    }

    private boolean loadLoggingService() {
        WebApplicationContext springContext = WebApplicationContextUtils
                .getWebApplicationContext(this.getServletContext());
        this.loggingService = (LoggingService) springContext
                .getBean("loggingService");
        if (loggingService == null) {
            throw new RuntimeException("Unable to load LoggingService!");
        } else {
            return true;
        }
    }

    public String setLocalCoreJid(String localCoreJid) {
        LoggingServiceImpl loggingServiceImpl = (LoggingServiceImpl) getLoggingService();
        loggingServiceImpl.setLocalCoreJid(localCoreJid);
        return loggingServiceImpl.getLocalCoreJid();
    }

    public List<String> unRegisterCoreId(List<String> jidList) {
        LoggingServiceImpl loggingServiceImpl = (LoggingServiceImpl) getLoggingService();
        loggingServiceImpl.unRegisteredJidList(jidList);
        loggingServiceImpl.updateEacUsersFile();
        return loggingServiceImpl.getRegisteredJidList();
    }

    public Map<String,List<String>> updateComponentHistory() {
        LoggingServiceImpl loggingServiceImpl = (LoggingServiceImpl) getLoggingService();
        return loggingServiceImpl.updateComponentHistory();
    }

    @Override
    public String sendHistroy(boolean send) {
        LoggingServiceImpl loggingServiceImpl = (LoggingServiceImpl) getLoggingService();
        loggingServiceImpl.setSendHistory(false);
        return null;
    }

    @Override
    public List<String> getAllJids() {
        LoggingServiceImpl loggingServiceImpl = (LoggingServiceImpl) getLoggingService();
        return loggingServiceImpl.getLocalCoreJids();
    }

    @Override
    public String unregisterLocalJid(String connectionJid, boolean remote) {
        LoggingServiceImpl loggingServiceImpl = (LoggingServiceImpl) getLoggingService();
        loggingServiceImpl.unRegisterLocalJid(connectionJid);
        if(remote || loggingServiceImpl.getLocalCoreJids().isEmpty())
            this.connectionInfo.clear();
        return null;
    }

}
