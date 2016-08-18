package com.saic.uicds.core.em.adminconsole.server.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.uicds.coreConfig.CoreConfigListType;
import org.uicds.coreConfig.CoreConfigType;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.resourceInstanceService.ResourceInstance;
import org.uicds.resourceInstanceService.ResourceInstanceListType;
import org.uicds.serviceConfig.ServiceConfigListType;
import org.uicds.serviceConfig.ServiceConfigType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.saic.uicds.core.em.adminconsole.client.model.CoreConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ExternalDataSourceConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ExternalToolConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ServiceConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.rpc.DirectoryServiceProxy;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.LoggingService;
import com.saic.uicds.core.infrastructure.service.ResourceInstanceService;
import com.saic.uicds.core.infrastructure.service.impl.LoggingServiceImpl;

public class DirectoryServiceProxyImpl extends RemoteServiceServlet implements
    DirectoryServiceProxy {

    private ResourceInstanceService resourceInstanceService = null;

    private LoggingService loggingService = null;

    private DirectoryService service = null;

    private String consoleResource;

    private String corePassword;

    private String boshServiceUrl;
    
    private String connectionTimeOut;

    public String getConnectionTimeOut() {
        return connectionTimeOut;
    }

    public void setConnectionTimeOut(String connectionTimeout) {
        this.connectionTimeOut = connectionTimeout;
    }

    public String getBoshServiceUrl() {
        return boshServiceUrl;
    }

    public void setBoshServiceUrl(String boshService) {
        this.boshServiceUrl = boshService;
    }

    public String getConsoleResource() {
        return consoleResource;
    }

    public void setConsoleResource(String consoleResource) {
        this.consoleResource = consoleResource;
        loadLoggingService();
        LoggingServiceImpl ls=(LoggingServiceImpl)loggingService;
        ls.setConsoleResource(consoleResource);
        
    }

    public void setCorePassword(String corePassword) {
        this.corePassword = corePassword;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -3776578278148899670L;

    public List<CoreConfigGWT> getCoreList() {

        CoreConfigListType list = getDirectoryService().getCoreList();
        CoreConfigType[] coreArray = list.getCoreArray();
        List<CoreConfigGWT> listGWT = new ArrayList<CoreConfigGWT>(coreArray.length);
        for (CoreConfigType value : coreArray) {
            CoreConfigGWT temp = new CoreConfigGWT();
            /*            if (value.getOnlineStatus() == CoreStatusType.ONLINE) {
                            temp.setCoreName(value.getName());
                        } else {
                            temp.setCoreName("[" + value.getName() + "]");
                        }*/
            temp.setCoreName(value.getName());
            temp.setOnlineStatus(value.getOnlineStatus().toString());
            listGWT.add(temp);
        }
        return listGWT;
    }

    public List<CoreConfigGWT> getCoreListChildren(String coreName, CoreConfigGWT coreGWT) {

        if (!coreGWT.isRoot()) {
            List<CoreConfigGWT> coreList = getCoreList();
            return coreList;
        }
        List<CoreConfigGWT> listGWT = new ArrayList<CoreConfigGWT>(1);
        coreGWT.setRoot(false);
        listGWT.add(coreGWT);
        return listGWT;
    }

    public String getCoreName() {

        return getDirectoryService().getCoreName();
    }

    public String getCorePassword() {
        
        return corePassword;
    }

    private DirectoryService getDirectoryService() {

        if (this.service == null) {
            loadService();
        }
        return this.service;
    }

    public List<ExternalDataSourceConfigGWT> getExternalDataSourceList(String coreName) {

        // TODO Auto-generated method stub
        return null;
    }

    public List<ExternalToolConfigGWT> getExternalToolList(String coreName) {

        // TODO Auto-generated method stub
        return null;
    }

    private String getIncidentIDFromWP(WorkProduct wp) {

        String id = null;
        Set<String> ids = wp.getAssociatedInterestGroupIDs();
        id = ids.iterator().next();
        return id;
    }

    public List<ProfileGWT> getProfileList(String coreName) {

        ResourceInstanceListType list;
        if (StringUtils.isEmpty(coreName)) {
            list = getResourceInstanceService().getResourceInstanceList("");
        } else {
            list = getResourceInstanceService().getResourceInstanceList("");
        }
        ResourceInstance[] users = list.getResourceInstanceArray();
        List<ProfileGWT> listGWT = new ArrayList<ProfileGWT>(users.length);
        for (ResourceInstance value : users) {
            ProfileGWT temp = new ProfileGWT();
            temp.setEntityID(value.getID().getStringValue()); // This is what you input to the
            // Profile Service for
            // the Profile
            listGWT.add(temp);
        }

        return listGWT;
    }

    public String[] getPublishedProductTypeList() {

        WorkProductTypeListType list = getDirectoryService().getPublishedProductTypeList();
        return list.getProductTypeArray();

    }

    public String getRequestUrl() {

        return this.getThreadLocalRequest().getScheme() + "://"
            + this.getThreadLocalRequest().getServerName() + ":"
            + this.getThreadLocalRequest().getServerPort()
            + this.getThreadLocalRequest().getContextPath();
    }

    private ResourceInstanceService getResourceInstanceService() {

        if (this.resourceInstanceService == null) {
            loadResourceInstanceService();
        }
        return this.resourceInstanceService;
    }

    public List<ServiceConfigGWT> getServiceList(String coreName) {

        ServiceConfigListType list;
        if (StringUtils.isEmpty(coreName)) {
            list = getDirectoryService().getServiceList(getCoreName());
        } else {
            list = getDirectoryService().getServiceList(coreName);
        }
        ServiceConfigType[] services = list.getServiceArray();
        List<ServiceConfigGWT> listGWT = new ArrayList<ServiceConfigGWT>(services.length);
        for (ServiceConfigType value : services) {
            ServiceConfigGWT temp = new ServiceConfigGWT();
            temp.setXML(Util.getPrettyXmlFromString(value.toString()));
            try {
                BeanUtils.copyProperties(temp, value);
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            listGWT.add(temp);
        }

        return listGWT;
    }

    // method is here just for the stupid tree thing
    public List<ServiceConfigGWT> getServiceListChildren(String coreName,
        ServiceConfigGWT serviceGWT) {

        if (!serviceGWT.isRoot()) {
            List<ServiceConfigGWT> serviceList = getServiceList(coreName);
            return serviceList;
        }
        List<ServiceConfigGWT> listGWT = new ArrayList<ServiceConfigGWT>(1);
        serviceGWT.setRoot(false);
        listGWT.add(serviceGWT);
        return listGWT;
    }

    private boolean loadResourceInstanceService() {

        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        this.resourceInstanceService = (ResourceInstanceService) springContext.getBean("resourceInstanceService");
        if (resourceInstanceService == null) {
            throw new RuntimeException("Unable to load ResourceInstanceService!");
        } else {
            return true;
        }
    }

    private boolean loadLoggingService() {
        
        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        this.loggingService = (LoggingService) springContext.getBean("loggingService");
        if (loggingService == null) {
            throw new RuntimeException("Unable to load loggingService!");
        } else {
            return true;
        }
    }

    private boolean loadService() {

        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        this.service = (DirectoryService) springContext.getBean("directoryService");
        if (service == null) {
            throw new RuntimeException("Unable to load DirectoryService!");
        } else {
            return true;
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String consoleResource = config.getInitParameter("consoleResource");
        setConsoleResource(consoleResource);
        String corePassword = config.getInitParameter("corePassword");
        setCorePassword(corePassword);
        String boshServiceUrl=config.getInitParameter("boshServiceUrl");
        setBoshServiceUrl(boshServiceUrl);
        String connectionTimeOut=config.getInitParameter("connectionTimeOut");
        setConnectionTimeOut(connectionTimeOut);
    }

    public Map<String, String> getConfigurationMap() {
        Map<String,String> configurationMap=new HashMap<String, String>();
        String coreName=getCoreName();
        String corePassword=getCorePassword();
        String resource=getConsoleResource();
        String boshServiceUrl=getBoshServiceUrl();
        String connectionTimeOut=getConnectionTimeOut();
        configurationMap.put("coreName", coreName);
        configurationMap.put("corePassword", corePassword);
        configurationMap.put("resource", resource);
        configurationMap.put("boshServiceUrl", boshServiceUrl);
        configurationMap.put("connectionTimeOut", connectionTimeOut);
        return configurationMap;
    }
}
