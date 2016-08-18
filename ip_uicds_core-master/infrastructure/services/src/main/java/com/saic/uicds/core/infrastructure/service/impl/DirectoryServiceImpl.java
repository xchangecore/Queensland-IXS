package com.saic.uicds.core.infrastructure.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.coreConfig.CoreConfigListType;
import org.uicds.coreConfig.CoreConfigType;
import org.uicds.coreConfig.CoreStatusType;
import org.uicds.coreConfig.CoreStatusType.Enum;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.externalDataSourceConfig.ExternalDataSourceConfigListType;
import org.uicds.externalDataSourceConfig.ExternalDataSourceConfigType;
import org.uicds.externalToolConfig.ExternalToolConfigListType;
import org.uicds.externalToolConfig.ExternalToolConfigType;
import org.uicds.serviceConfig.ServiceConfigListType;
import org.uicds.serviceConfig.ServiceConfigType;
import org.uicds.sosConfig.SOSConfigListType;
import org.uicds.sosConfig.SOSConfigType;

import com.saic.uicds.core.infrastructure.dao.ExternalDataSourceConfigDAO;
import com.saic.uicds.core.infrastructure.dao.PublishedProductDAO;
import com.saic.uicds.core.infrastructure.dao.RegisteredServiceDAO;
import com.saic.uicds.core.infrastructure.dao.SubscribedProductDAO;
import com.saic.uicds.core.infrastructure.exceptions.EmptySubscriberNameException;
import com.saic.uicds.core.infrastructure.exceptions.InvalidProductIDException;
import com.saic.uicds.core.infrastructure.exceptions.InvalidProductTypeException;
import com.saic.uicds.core.infrastructure.exceptions.InvalidXpathException;
import com.saic.uicds.core.infrastructure.exceptions.NullSubscriberException;
import com.saic.uicds.core.infrastructure.listener.NotificationListener;
import com.saic.uicds.core.infrastructure.messages.AgreementRosterMessage;
import com.saic.uicds.core.infrastructure.messages.CoreRosterMessage;
import com.saic.uicds.core.infrastructure.messages.CoreStatusUpdateMessage;
import com.saic.uicds.core.infrastructure.model.ExternalDataSourceConfig;
import com.saic.uicds.core.infrastructure.model.PublishedProduct;
import com.saic.uicds.core.infrastructure.model.RegisteredService;
import com.saic.uicds.core.infrastructure.model.SubscribedProduct;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.CommunicationsService;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.PubSubService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;

/**
 * The DirectoryService implementation
 * 
 * @author UICDS team
 * @since 1.0
 * @see com.saic.uicds.core.infrastructure.model.ExternalDataSourceConfig ExternalDataSourceConfig
 *      Data Model
 * @see com.saic.uicds.core.infrastructure.model.PublishedProduct PublishedProduct Data Model
 * @see com.saic.uicds.core.infrastructure.model.RegisteredService RegisteredService Data Model
 * @see com.saic.uicds.core.infrastructure.model.SubscribedProduct SubscribedProduct Data Model
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class DirectoryServiceImpl implements DirectoryService {

    Logger log = LoggerFactory.getLogger(DirectoryServiceImpl.class);

    private RegisteredServiceDAO registeredServiceDAO;

    private CommunicationsService communicationsService;

    private String resource;

    private String overallCoreStatus = "NORMAL";

    public CommunicationsService getCommunicationsService() {

        return communicationsService;
    }

    public void setCommunicationsService(CommunicationsService communicationsService) {

        this.communicationsService = communicationsService;
    }

    private List<String> localCoreJids = new ArrayList<String>();

    @Override
    public String getLocalCoreJid() {

        return null;
    }

    @Override
    public void setLocalCoreJid(String localCoreJid) {

        this.localCoreJids.add(localCoreJid);
    }

    public void setRegisteredServiceDAO(RegisteredServiceDAO registeredServiceDAO) {

        this.registeredServiceDAO = registeredServiceDAO;
    }

    private PublishedProductDAO publishedProductDAO;

    public void setPublishedProductDAO(PublishedProductDAO publishedProductDAO) {

        this.publishedProductDAO = publishedProductDAO;
    }

    private SubscribedProductDAO subscribedProductDAO;

    public void setSubscribedProductDAO(SubscribedProductDAO subscribedDAO) {

        this.subscribedProductDAO = subscribedDAO;
    }

    private ExternalDataSourceConfigDAO externalDataSourceConfigDAO;

    public void setExternalDataSourceConfigDAO(
            ExternalDataSourceConfigDAO externalDataSourceConfigDAO) {

        this.externalDataSourceConfigDAO = externalDataSourceConfigDAO;
    }

    private ConfigurationService configurationService;

    public void setConfigurationService(ConfigurationService ds) {

        configurationService = ds;
    }

    // private MessageChannel systemInitializedChannel;
    //
    // public MessageChannel getSystemInitializedChannel() {
    //
    // return systemInitializedChannel;
    // }
    //
    // public void setSystemInitializedChannel(MessageChannel systemInitializedChannel) {
    //
    // this.systemInitializedChannel = systemInitializedChannel;
    // }

    private WorkProductService workProductService;

    public void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }

    private PubSubService pubSubService;

    public void setPubSubService(PubSubService pubSubService) {

        this.pubSubService = pubSubService;
    }

    class RegisterUICDSServiceRequestData {
        public String urn;
        public String serviceName;
        public WorkProductTypeListType publshiedProducts;
        public WorkProductTypeListType subscribedProducts;

        public RegisterUICDSServiceRequestData(String urn, String serviceName,
                WorkProductTypeListType publshiedProducts,
                WorkProductTypeListType subscribedProducts) {

            this.urn = urn;
            this.serviceName = serviceName;
            this.publshiedProducts = publshiedProducts;
            this.subscribedProducts = subscribedProducts;
        }
    }

    // Need to cache UICDS service requests since they can be made before the bean's session is
    // fully initialized
    List<RegisterUICDSServiceRequestData> cachedUICDSServiceRequests = new ArrayList<RegisterUICDSServiceRequestData>();

    // Key: Core Name
    // Value: Core Status (Online/Offline)
    private HashMap<String, CoreStatusType.Enum> coreStatusMap = new HashMap<String, CoreStatusType.Enum>();

    // Key: sosID
    // Value: sosURN
    private HashMap<String, String> sosMap = new HashMap<String, String>();

    private class AgreementListener implements NotificationListener<AgreementRosterMessage> {

        @Override
        public void onChange(AgreementRosterMessage notificationMessage) {

            log.info("recived change in agreement: " + notificationMessage.getAgreementID());
            for (String coreName : notificationMessage.getCores().keySet()) {
                log.info("   " + coreName + ":" + notificationMessage.getCores().get(coreName));
                // If we know about this core then only worry about recind
                if (coreStatusMap.containsKey(coreName)) {
                    switch (notificationMessage.getCores().get(coreName)) {
                    case RESCIND: {
                        removeCoreFromStatusMap(coreName);
                        sendMessageToConsole(coreName, "remove");
                    }
                    }
                }
                // else only worry about the create, set to offline and
                // the presence updates will change the status through
                // the core roster status update handlers
                else {
                    switch (notificationMessage.getCores().get(coreName)) {
                    case CREATE: {
                        addCoreToStatusMap(coreName, CoreStatusType.OFFLINE);
                        sendMessageToConsole(coreName, "create");
                    }
                    }
                }
            }
        }

    };

    @PostConstruct
    public void init() {

    }

    /** {@inheritDoc} */
    public void systemInitializedHandler(String messgae) {

        if (log.isDebugEnabled()) {
            log.debug("DirectoryServiceImpl:systemInitializedHandler - started");
        }

        String coreName = configurationService.getCoreName();

        try {
            // call getServiceList so any cached registration requests get processed
            ServiceConfigListType serviceList = getServiceList(coreName);
            if (log.isDebugEnabled()) {
                log.debug("number of registered services=" + serviceList.sizeOfServiceArray());
                log.debug("DirectoryServiceImpl:systemInitializedHandler - completed");
            }
        } catch (Throwable e) {
            log.error("Exception caught while getting service list.   exception=" + e.getMessage());
            e.printStackTrace();
        }

        // Subscribe for notifications of changes in agreements
        try {
            pubSubService.addAgreementListener("*", new AgreementListener());
        } catch (InvalidProductIDException e) {
            log.error("Agreement subscription has invalid product id");
        } catch (NullSubscriberException e) {
            log.error("Agreement subscription has null subscriber");
        } catch (EmptySubscriberNameException e) {
            log.error("Agreement subscription has empty subscriber name");
        }
    }

    private synchronized void removeCoreFromStatusMap(String coreName) {

        coreStatusMap.remove(coreName);
    }

    private synchronized void addCoreToStatusMap(String coreName, Enum updatedStatus) {

        coreStatusMap.put(coreName, updatedStatus);
    }

    /**
     * Gets the incident list.
     * 
     * @return the incident list
     * @ssdd
     */
    @Override
    public WorkProduct[] getIncidentList() {

        ArrayList<WorkProduct> workProducts = new ArrayList<WorkProduct>();
        List<String> incidentWPIDs;
        try {
            incidentWPIDs = workProductService.getProductIDListByTypeAndXQuery("Incident", null,
                    null);
            for (String incidentWPID : incidentWPIDs) {
                WorkProduct wp = workProductService.getProduct(incidentWPID);
                if (wp != null) {
                    workProducts.add(wp);
                }
            }
        } catch (InvalidXpathException e) {
            log.error("invalid xpath getting product id by list and XQuery");
        }

        WorkProduct[] products = new WorkProduct[workProducts.size()];
        return workProducts.toArray(products);

    }

    /**
     * Gets the core name.
     * 
     * @return the core name
     * @ssdd
     */
    @Override
    public String getCoreName() {

        return configurationService.getCoreName();
    }

    /**
     * Gets the core list.
     * 
     * @return the core list
     * @ssdd
     */
    @Override
    public /*synchronized*/ CoreConfigListType getCoreList() {

        // Get list of cores from Communications Service

        CoreConfigListType coreList = CoreConfigListType.Factory.newInstance();

        Set<String> coreNames = coreStatusMap.keySet();

        String localCoreName = "localhost";
        try {
            localCoreName = InetAddress.getLocalHost().getHostName().toLowerCase();
        } catch (UnknownHostException e) {
            log.error("Cannot get host name: " + e.getMessage());
        }

        for (String coreName : coreNames) {
            CoreConfigType coreConfig = coreList.addNewCore();
            coreConfig.setName(coreName);
            coreConfig.setLocalCore((coreName.indexOf(localCoreName) != -1) ? true : false);
            coreConfig.setURL(coreName);
            coreConfig.setOnlineStatus(coreStatusMap.get(coreName));
        }

        return coreList;
    }

    /**
     * Gets the core config.
     * 
     * @param coreName the core name
     * 
     * @return the core config
     * @ssdd
     */
    @Override
    public /*synchronized*/ CoreConfigType getCoreConfig(String coreName) {

        // Get list of cores from Communications Service

        CoreConfigType coreConfig = null;

        if (coreStatusMap.containsKey(coreName)) {
            coreConfig = CoreConfigType.Factory.newInstance();
            coreConfig.setName(coreName);
            coreConfig.setOnlineStatus(coreStatusMap.get(coreName));
        }

        return coreConfig;
    }

    /**
     * Gets the external tool list.
     * 
     * @param coreName the core name
     * 
     * @return the external tool list
     * @ssdd
     */
    @Override
    public ExternalToolConfigListType getExternalToolList(String coreName) {

        // Get list of external tools from database
        Set<RegisteredService> externalTools = registeredServiceDAO.findByServiceTypeAndCoreName(
                RegisteredService.SERVICE_TYPE.EXTERNAL, coreName);

        ExternalToolConfigListType externalToolList = ExternalToolConfigListType.Factory
                .newInstance();

        // Construct the return list from the items retrieved from the database
        for (RegisteredService externalTool : externalTools) {
            ExternalToolConfigType externalToolConfig = externalToolList.addNewExternalTool();
            externalToolConfig.setURN(externalTool.getURN());
            externalToolConfig.setCoreName(externalTool.getCoreName());
            externalToolConfig.setToolName(externalTool.getServiceName());

            WorkProductTypeListType publishedProducts = WorkProductTypeListType.Factory
                    .newInstance();
            for (PublishedProduct publishedProduct : externalTool.getPublishedProducts()) {
                publishedProducts.addProductType(publishedProduct.getProductType());
            }
            externalToolConfig.setPublishedProducts(publishedProducts);

            WorkProductTypeListType subscribedProducts = WorkProductTypeListType.Factory
                    .newInstance();
            for (SubscribedProduct subscribedProduct : externalTool.getSubscribedProducts()) {
                subscribedProducts.addProductType(subscribedProduct.getProductType());
            }
            externalToolConfig.setSubscribedProducts(subscribedProducts);
        }
        // }

        return externalToolList;
    }

    /**
     * Gets the external data source list.
     * 
     * @param coreName the core name
     * 
     * @return the external data source list
     * @ssdd
     */
    @Override
    public ExternalDataSourceConfigListType getExternalDataSourceList(String coreName) {

        // Get list of data sources from database
        List<ExternalDataSourceConfig> externalDataSources = externalDataSourceConfigDAO
                .findByCoreName(coreName);

        ExternalDataSourceConfigListType externalDataSourceList = ExternalDataSourceConfigListType.Factory
                .newInstance();

        // Construct the return list from the items retrieved from the database
        for (ExternalDataSourceConfig externalDataSource : externalDataSources) {
            ExternalDataSourceConfigType externalDataSourceConfig = externalDataSourceList
                    .addNewExternalDataSource();
            externalDataSourceConfig.setURN(externalDataSource.getUrn());
            externalDataSourceConfig.setCoreName(externalDataSource.getCoreName());
        }
        // }

        return externalDataSourceList;
    }

    /**
     * Gets the list of sensors.
     * 
     * @return the sensor list
     * @ssdd
     */
    @Override
    public SOSConfigListType getSOSList() {

        Set<String> sosIDs = sosMap.keySet();

        SOSConfigListType sosList = SOSConfigListType.Factory.newInstance();
        for (String sosID : sosIDs) {
            SOSConfigType sos = sosList.addNewSos();
            sos.setServiceID(sosID);
            sos.setURN(sosMap.get(sosID));
        }
        return sosList;
    }

    /**
     * Gets the service list.
     * 
     * @param coreName the core name
     * 
     * @return the service list
     * @ssdd
     */
    @Override
    public ServiceConfigListType getServiceList(String coreName) {

        // if (log.isDebugEnabled()) {
        // log.debug("getServiceList (coreName: " + coreName + ")");
        // }

        // process cache requests
        for (RegisterUICDSServiceRequestData request : cachedUICDSServiceRequests) {
            log.info("process cached registration request fro service:" + request.serviceName);
            registerUICDSService(request.urn, request.serviceName, request.publshiedProducts,
                    request.subscribedProducts);
        }
        cachedUICDSServiceRequests.clear();

        // Get a list of UICDS services from database
        Set<RegisteredService> services = registeredServiceDAO.findByServiceTypeAndCoreName(
                RegisteredService.SERVICE_TYPE.UICDS, coreName);

        ServiceConfigListType serviceList = ServiceConfigListType.Factory.newInstance();

        // Need to put result in hash map first to get rid of duplicates created by the many-to-many
        // associations
        HashSet<RegisteredService> hashList = new HashSet<RegisteredService>();
        for (RegisteredService service : services) {
            hashList.add(service);
        }

        // Construct the return list from the items retrieved from the database
        for (RegisteredService service : hashList) {
            // if (log.isDebugEnabled()) {
            // log.debug("getServiceList: found service: " + service.getServiceName() + ")");
            // }

            ServiceConfigType serviceConfig = serviceList.addNewService();
            serviceConfig.setServiceName(service.getServiceName());
            serviceConfig.setCoreName(service.getCoreName());
            serviceConfig.setURN(service.getURN());

            WorkProductTypeListType publishedProducts = WorkProductTypeListType.Factory
                    .newInstance();
            for (PublishedProduct publishedProduct : service.getPublishedProducts()) {
                publishedProducts.addProductType(publishedProduct.getProductType());
            }
            serviceConfig.setPublishedProducts(publishedProducts);

            WorkProductTypeListType subscribedProducts = WorkProductTypeListType.Factory
                    .newInstance();
            for (SubscribedProduct subscribedProduct : service.getSubscribedProducts()) {
                subscribedProducts.addProductType(subscribedProduct.getProductType());
            }
            serviceConfig.setSubscribedProducts(subscribedProducts);
        }

        return serviceList;
    }

    /**
     * Register external data source.
     * 
     * @param urn the urn
     * @ssdd
     */
    @Override
    public void registerExternalDataSource(String urn) {

        if (log.isDebugEnabled()) {
            log.debug("registerExternalDataSource (urn: " + urn + ")");
        }

        // Persist external data source in the database, overriding any existing one
        ExternalDataSourceConfig externalDataSourceConfig = new ExternalDataSourceConfig(urn,
                configurationService.getCoreName());

        // Delete previous registration (should be just one) if one exists
        List<ExternalDataSourceConfig> dataSources = externalDataSourceConfigDAO.findByUrn(urn);
        for (ExternalDataSourceConfig dataSource : dataSources) {
            externalDataSourceConfigDAO.makeTransient(dataSource);
        }
        externalDataSourceConfigDAO.makePersistent(externalDataSourceConfig);
    }

    /**
     * Register external tool.
     * 
     * @param urn the urn
     * @param toolName the tool name
     * @param publishedProducts the published products
     * @param subscribedProducts the subscribed products
     * @ssdd
     */
    @Override
    public void registerExternalTool(String urn, String toolName,
            WorkProductTypeListType publishedProducts, WorkProductTypeListType subscribedProducts) {

        if (log.isDebugEnabled()) {
            log.debug("registerExternalTool (urn: " + urn + ")");
        }

        // Persist the external tool in the database, overriding any existing one
        Set<PublishedProduct> publishedList = new HashSet<PublishedProduct>();
        for (Integer i = 0; i < publishedProducts.sizeOfProductTypeArray(); i++) {
            PublishedProduct publishedProduct = new PublishedProduct(
                    publishedProducts.getProductTypeArray(i));
            publishedList.add(publishedProduct);
        }

        Set<SubscribedProduct> subscribedList = new HashSet<SubscribedProduct>();
        for (Integer i = 0; i < subscribedProducts.sizeOfProductTypeArray(); i++) {
            SubscribedProduct subscribedProduct = new SubscribedProduct(
                    subscribedProducts.getProductTypeArray(i));
            subscribedList.add(subscribedProduct);
        }

        String coreName = configurationService.getCoreName();
        RegisteredService externalTool = new RegisteredService(urn, toolName,
                RegisteredService.SERVICE_TYPE.EXTERNAL, coreName, publishedList, subscribedList);

        // Delete previous registration (should be just one) if one exists
        Set<RegisteredService> tools = registeredServiceDAO.findByServiceNameAndCoreName(toolName,
                coreName);
        for (RegisteredService tool : tools) {
            registeredServiceDAO.makeTransient(tool);
        }
        registeredServiceDAO.makePersistent(externalTool);
    }

    /**
     * Register uicds service.
     * 
     * @param urn the urn
     * @param serviceName the service name
     * @param publishedProducts the published products
     * @param subscribedProducts the subscribed products
     * @ssdd
     */
    @Override
    public void registerUICDSService(String urn, String serviceName,
            WorkProductTypeListType publishedProducts, WorkProductTypeListType subscribedProducts) {

        // @Transactional(propagation = Propagation.REQUIRES_NEW)
        if (!registeredServiceDAO.isSessionInitialized()) {
            if (log.isInfoEnabled()) {
                log.info("registerUICDSService - session not yer initialized - cache requrest for (serviceName: "
                        + serviceName + ")");
            }

            // buffer up this request for later
            this.cachedUICDSServiceRequests.add(new RegisterUICDSServiceRequestData(urn,
                    serviceName, publishedProducts, subscribedProducts));
        } else {

            if (log.isInfoEnabled()) {
                log.info("===> registerUICDSService (serviceName: " + serviceName + ")");
            }

            // Persist the external tool in the database if one doesn't already exist
            Set<PublishedProduct> publishedList = new HashSet<PublishedProduct>();
            for (Integer i = 0; i < publishedProducts.sizeOfProductTypeArray(); i++) {
                if (log.isInfoEnabled()) {
                    log.info("=====> registerUICDSService (publishedProductType: "
                            + publishedProducts.getProductTypeArray(i) + ")");
                }
                PublishedProduct publishedProduct = new PublishedProduct(
                        publishedProducts.getProductTypeArray(i));
                publishedList.add(publishedProduct);
            }

            Set<SubscribedProduct> subscribedList = new HashSet<SubscribedProduct>();
            for (Integer i = 0; i < subscribedProducts.sizeOfProductTypeArray(); i++) {
                log.info("=====> registerUICDSService (subscribedproductType: "
                        + subscribedProducts.getProductTypeArray(i) + ")");
                SubscribedProduct subscribedProduct = new SubscribedProduct(
                        subscribedProducts.getProductTypeArray(i));
                subscribedList.add(subscribedProduct);
            }

            // Persist the UICDS service in the database overriding any existing one
            String coreName = configurationService.getCoreName();
            RegisteredService service = new RegisteredService(urn, serviceName,
                    RegisteredService.SERVICE_TYPE.UICDS, coreName, publishedList, subscribedList);

            // Delete previous registration (should be just one) if one exists
            Set<RegisteredService> services = registeredServiceDAO.findByServiceNameAndCoreName(
                    serviceName, coreName);

            for (RegisteredService svc : services) {
                if (log.isDebugEnabled()) {
                    log.debug("Remove existing registration for " + svc.getServiceName().toString());
                }
                registeredServiceDAO.makeTransient(svc);
            }

            if (log.isDebugEnabled()) {
                log.debug("adding new registration for " + serviceName);
            }
            registeredServiceDAO.makePersistent(service);

        }
    }

    /**
     * Unregister external data source.
     * 
     * @param urn the urn
     * @ssdd
     */
    @Override
    public void unregisterExternalDataSource(String urn) {

        if (log.isDebugEnabled()) {
            log.debug("unregisterExternalDataSource (urn: " + urn + ")");
        }

        // Delete previous registration (should be just one) if one exists
        List<ExternalDataSourceConfig> dataSources = externalDataSourceConfigDAO.findByUrn(urn);
        for (ExternalDataSourceConfig dataSource : dataSources) {
            externalDataSourceConfigDAO.makeTransient(dataSource);
        }
    }

    /**
     * Unregister external tool.
     * 
     * @param urn the urn
     * @ssdd
     */
    @Override
    public void unregisterExternalTool(String urn) {

        if (log.isDebugEnabled()) {
            log.debug("unregisterExternalTool (urn: " + urn + ")");
        }

        String coreName = configurationService.getCoreName();

        // Delete previous registration (should be just one) if one exists
        Set<RegisteredService> tools = registeredServiceDAO.findByUrnAndCoreName(urn, coreName);
        for (RegisteredService tool : tools) {
            registeredServiceDAO.makeTransient(tool);
        }
    }

    /**
     * Unregister uicds service.
     * 
     * @param serviceName the service name
     * @ssdd
     */
    @Override
    public void unregisterUICDSService(String serviceName) {

        if (log.isDebugEnabled()) {
            log.debug("unregisterUICDSService (serviceName: " + serviceName + ")");
        }

        String coreName = configurationService.getCoreName();

        // Delete previous registration (should be just one) if one exists
        Set<RegisteredService> services = registeredServiceDAO.findByServiceNameAndCoreName(
                serviceName, coreName);
        for (RegisteredService svc : services) {
            registeredServiceDAO.makeTransient(svc);
        }

    }

    /**
     * Gets the subscribed product type list.
     * 
     * @return the subscribed product type list
     * @ssdd
     */
    @Override
    public WorkProductTypeListType getSubscribedProductTypeList() {

        WorkProductTypeListType productList = WorkProductTypeListType.Factory.newInstance();

        Set<SubscribedProduct> subscribedProducts = subscribedProductDAO
                .findAllSubscribedProducts();
        for (SubscribedProduct prod : subscribedProducts) {
            productList.addProductType(prod.getProductType());
        }

        return productList;
    }

    /**
     * Gets the published product type list.
     * 
     * @return the published product type list
     * @ssdd
     */
    @Override
    public WorkProductTypeListType getPublishedProductTypeList() {

        WorkProductTypeListType productList = WorkProductTypeListType.Factory.newInstance();

        Set<PublishedProduct> publishedProducts = publishedProductDAO.findAllPublishedProducts();
        for (PublishedProduct prod : publishedProducts) {
            productList.addProductType(prod.getProductType());
        }

        return productList;
    }

    /**
     * Core roster handler.
     * 
     * @param message the message
     * @ssdd
     */
    @Override
    public void coreRosterHandler(CoreRosterMessage message) {

        if (log.isDebugEnabled()) {
            log.debug("=====> *** DirectoryServiceImpl.coreRosterHandler **** <=====");
        }

        Map<String, String> coreStatusUpdateMap = message.getCoreStatusMap();

        Set<String> coreNames = coreStatusUpdateMap.keySet();

        for (String coreName : coreNames) {
            CoreStatusType.Enum updatedStatus = (coreStatusUpdateMap.get(coreName)
                    .equals("available")) ? CoreStatusType.ONLINE : CoreStatusType.OFFLINE;

            if (log.isInfoEnabled()) {
                log.info("===> coreRosterHandler [" + coreName + "," + updatedStatus.toString()
                        + "]");
            }

            addCoreToStatusMap(coreName, updatedStatus);

            sendMessageToConsole(coreName, "update");
            sendCoreStatus(coreName, updatedStatus.toString());
        }
        if (log.isDebugEnabled()) {
            log.debug("=====> *** DirectoryServiceImpl.coreRosterHandler ends **** <=====");
        }
    }

    private void sendMessageToConsole(String coreName, String operation) {

        for (String jid : localCoreJids) {
            String updateMessage = "DirectoryService-CoreStatus:[" + coreName + "]\n";
            updateMessage += "Operation:[" + operation + "]";
            this.communicationsService.sendXMPPMessage(updateMessage, "message", "message", jid);
        }
    }

    /**
     * Core status update handler.
     * 
     * @param message the message
     * @ssdd
     */
    @Override
    public void coreStatusUpdateHandler(CoreStatusUpdateMessage message) {

        if (log.isDebugEnabled()) {
            log.debug("=====> *** DirectoryServiceImpl.coreStatusUpdateHandler **** <=====");
        }

        String coreName = message.getCoreName();
        String coreStatus = message.getCoreStatus();

        if (coreName.endsWith("/CoreConnection")) {

            coreName = coreName.substring(0, coreName.lastIndexOf('/'));

            if (log.isDebugEnabled()) {
                log.debug("=====> *** coreName=" + coreName + " coreStatus=" + coreStatus);
            }

            if (coreStatus.equals("unsubscribed")) {
                removeCoreFromStatusMap(coreName);
            } else {
                CoreStatusType.Enum updatedStatus = (coreStatus.equals("available")) ? CoreStatusType.ONLINE
                        : CoreStatusType.OFFLINE;

                if (log.isInfoEnabled()) {
                    log.info("===> *** coreStatusUpdate [" + coreName + ","
                            + updatedStatus.toString() + "]");
                }
                addCoreToStatusMap(coreName, updatedStatus);
            }
            sendMessageToConsole(coreName, "update");
            sendCoreStatus(coreName, coreStatus);
        } else {
            log.debug("=====> *** Not intended for 'CoreConnection' [" + message.getCoreName()
                    + "]");
        }
        if(coreStatus.contains("available"))
            sendCoreStatus(coreName, coreStatus);
    }

    private void sendCoreStatus(String coreName, String coreStatus) {

        if (coreStatus.equals("available") || coreStatus.toLowerCase().contains("online")) {
            String jid = getCoreName();
            String message = "Remote-CoreStatus: [" + jid + "]\n";
            message += "Status: [" + getOverallCoreStatus() + "]";
            String resource = getConsoleResource();
            String remoteJid=coreName;
            if(!remoteJid.contains("/"))
                remoteJid=coreName + "/" + resource;
            if (resource != null) {
                getCommunicationsService().sendXMPPMessage(message, "message", "message",
                        remoteJid);
            }
        }
    }

    /**
     * Register sos.
     * 
     * @param sosID the sos id
     * @param sosURN the sos urn
     * @ssdd
     */
    @Override
    public void registerSOS(String sosID, String sosURN) {

        sosMap.put(sosID, sosURN);
    }

    /**
     * Unregister sos.
     * 
     * @param sosID the sos id
     * @ssdd
     */
    @Override
    public void unregisterSOS(String sosID) {

        sosMap.remove(sosID);
    }

    /**
     * Gets the service name by published product type.
     * 
     * @param publishedProductType the published product type
     * 
     * @return the service name by published product type
     * 
     * @throws InvalidProductTypeException the invalid product type exception
     * @ssdd
     */
    @Override
    public String getServiceNameByPublishedProductType(String publishedProductType)
            throws InvalidProductTypeException {

        if (log.isInfoEnabled()) {
            log.info("getServiceNameByPublishedProductType: productType=" + publishedProductType);
        }
        String serviceName = null;

        Set<PublishedProduct> publishedProductList = publishedProductDAO
                .findByProductType(publishedProductType);
        if (publishedProductList.size() == 0) {
            throw new InvalidProductTypeException();
        } else {
            for (PublishedProduct product : publishedProductList) {
                serviceName = product.getPublisher().getServiceName();
                break;
            }
        }
        if (log.isInfoEnabled()) {
            log.info("getServiceNameByPublishedProductType: serviceName=" + serviceName);
        }
        return serviceName;
    }

    @Override
    public void setConsoleResource(String resource) {

        this.resource = resource;
    }

    @Override
    public String getConsoleResource() {

        return resource;
    }

    @Override
    public void setOverallCoreStatus(String category) {

        this.overallCoreStatus = category;
    }

    @Override
    public String getOverallCoreStatus() {

        return overallCoreStatus;
    }

}
