package com.saic.uicds.core.infrastructure.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.coreConfig.CoreConfigListType;
import org.uicds.directoryService.GetCoreListRequestDocument;
import org.uicds.directoryService.GetCoreListResponseDocument;
import org.uicds.directoryService.GetExternalDataSourceListRequestDocument;
import org.uicds.directoryService.GetExternalDataSourceListResponseDocument;
import org.uicds.directoryService.GetExternalToolListRequestDocument;
import org.uicds.directoryService.GetExternalToolListResponseDocument;
import org.uicds.directoryService.GetIncidentListRequestDocument;
import org.uicds.directoryService.GetIncidentListResponseDocument;
import org.uicds.directoryService.GetIncidentListResponseDocument.GetIncidentListResponse;
import org.uicds.directoryService.GetSOSListRequestDocument;
import org.uicds.directoryService.GetSOSListResponseDocument;
import org.uicds.directoryService.GetServiceListRequestDocument;
import org.uicds.directoryService.GetServiceListResponseDocument;
import org.uicds.directoryService.RegisterExternalDataSourceRequestDocument;
import org.uicds.directoryService.RegisterExternalToolRequestDocument;
import org.uicds.directoryService.RegisterSOSRequestDocument;
import org.uicds.directoryService.RegisterUICDSServiceRequestDocument;
import org.uicds.directoryService.UnregisterExternalDataSourceRequestDocument;
import org.uicds.directoryService.UnregisterExternalToolRequestDocument;
import org.uicds.directoryService.UnregisterSOSRequestDocument;
import org.uicds.directoryService.UnregisterUICDSServiceRequestDocument;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.externalDataSourceConfig.ExternalDataSourceConfigListType;
import org.uicds.externalToolConfig.ExternalToolConfigListType;
import org.uicds.serviceConfig.ServiceConfigListType;
import org.uicds.sosConfig.SOSConfigListType;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

/**
 * The Directory Service allows UICDS clients to retrieve information on common data sources, common
 * tools, and services available on this core installation. The following information is available:
 * <ul>
 * <li>UICDS Cores that have Information Sharing Agreements
 * <li>Available services
 * <li>Available external tools
 * <li>Available external data sources
 * <li>Available sensor observation services (SOSs)
 * <li>Active incidents (to be deprecated, use Incident Management Service instead)
 * </ul>
 * <p>
 * 
 * @author Aruna Hau
 * @since 1.0
 * @see <a href="../../wsdl/DirectoryService.wsdl">Appendix: DirectoryService.wsdl</a>
 * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
 * @see <a href="../../services/Directory/0.1/CoreConfig.xsd">Appendix: CoreConfig.xsd</a>
 * @see <a href="../../services/Directory/0.1/ExternalDataSourceConfig.xsd">Appendix:
 *      ExternalDataSourceConfig.xsd</a>
 * @see <a href="../../services/Directory/0.1/ExternalToolConfig.xsd">Appendix: ExternalToolConfig.xsd</a>
 * @see <a href="../../services/Directory/0.1/ServiceConfig.xsd">Appendix: ServiceConfig.xsd</a>
 * @see <a href="../../services/Directory/0.1/SOSConfig.xsd">Appendix: SOSConfig.xsd</a>
 * @idd
 * 
 */
@Endpoint
public class DirectoryServiceEndpoint
    implements ServiceNamespaces {
    @Autowired
    DirectoryService ds;

    Logger log = LoggerFactory.getLogger(DirectoryServiceEndpoint.class);

    public void setDirectoryService(DirectoryService directoryService) {

        ds = directoryService;
    }

    /**
     * Retrieves a list of incidents that are active in the UICDS core.
     * 
     * @param GetIncidentListRequestDocument
     * 
     * @return GetIncidentListResponseDocument
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/IncidentManagement/0.1/IncidentManagementServiceData.xsd">Appendix:
     *      IncidentManagementServiceData.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "GetIncidentListRequest")
    public GetIncidentListResponseDocument GetIncidentList(GetIncidentListRequestDocument requestDoc) {

        log.debug("GetIncidentList");

        GetIncidentListResponseDocument responseDoc = GetIncidentListResponseDocument.Factory.newInstance();

        WorkProduct workProducts[] = ds.getIncidentList();

        GetIncidentListResponse res = responseDoc.addNewGetIncidentListResponse();
        WorkProductList productList = populateSummary(workProducts);

        res.setWorkProductList(productList);

        return responseDoc;
    }

    private WorkProductList populateSummary(WorkProduct[] products) {

        WorkProductList productList = WorkProductList.Factory.newInstance();

        if (products != null && products.length > 0) {
            for (WorkProduct product : products) {
                if (product != null) {
                    productList.addNewWorkProduct().set(
                        WorkProductHelper.toWorkProductSummary(product));
                }
            }
        }

        return productList;
    }

    /**
     * Retrieves a list of cores, their addresses, and online status . The list of UICDS cores
     * contains entries for each core with which this core has agreements along with status
     * information.<BR>
     * 
     * @param GetCoreListRequestDocument
     * 
     * @return GetCoreListResponseDocument
     * 
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/CoreConfig.xsd">Appendix: CoreConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "GetCoreListRequest")
    public GetCoreListResponseDocument GetCoreList(GetCoreListRequestDocument requestDoc) {

        log.debug("GetCoreList: ");

        CoreConfigListType coreList = ds.getCoreList();

        GetCoreListResponseDocument responseDoc = GetCoreListResponseDocument.Factory.newInstance();
        responseDoc.addNewGetCoreListResponse().setCoreList(coreList);
        return responseDoc;
    }

    /**
     * Retrieves a list of registered external data sources that have registered with the UICDS
     * core.
     * 
     * @param GetExternalDataSourceListRequestDocument
     * 
     * @return GetExternalDataSourceListResponseDocument
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ExternalDataSourceConfig.xsd">Appendix:
     *      ExternalDataSourceConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "GetExternalDataSourceListRequest")
    public GetExternalDataSourceListResponseDocument GetExternalDataSourceList(
        GetExternalDataSourceListRequestDocument requestDoc) {

        String coreName = requestDoc.getGetExternalDataSourceListRequest().getCoreName();
        log.debug("GetExternalToolList (coreName: " + coreName + ")");

        ExternalDataSourceConfigListType externalDataSourceList = ds.getExternalDataSourceList(coreName);
        GetExternalDataSourceListResponseDocument responseDoc = GetExternalDataSourceListResponseDocument.Factory.newInstance();

        responseDoc.addNewGetExternalDataSourceListResponse().setExternalDataSourceList(
            externalDataSourceList);
        return responseDoc;
    }

    /**
     * Retrieves a list of external tools and their WPS interface URLs that are registered with the
     * UICDS core.
     * 
     * @param GetExternalToolListRequestDocument
     * 
     * @return GetExternalToolListResponseDocument
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ExternalToolConfig.xsd">Appendix:
     *      ExternalToolConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "GetExternalToolListRequest")
    public GetExternalToolListResponseDocument GetExternalToolList(
        GetExternalToolListRequestDocument requestDoc) {

        String coreName = requestDoc.getGetExternalToolListRequest().getCoreName();
        log.debug("GetExternalToolList (coreName: " + coreName + ")");

        ExternalToolConfigListType externalToolList = ds.getExternalToolList(coreName);
        GetExternalToolListResponseDocument responseDoc = GetExternalToolListResponseDocument.Factory.newInstance();

        responseDoc.addNewGetExternalToolListResponse().setExternalToolList(externalToolList);
        return responseDoc;
    }

    /**
     * Retrieves a list of sensors that are registered with the UICDS core.
     * 
     * @param GetSensorListRequestDocument
     * 
     * @return GetSensorListResponseDocument
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/SOSConfig.xsd">Appendix: SOSConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "GetSOSListRequest")
    public GetSOSListResponseDocument GetSOSList(GetSOSListRequestDocument requestDoc) {

        log.debug("GetSOSList");

        SOSConfigListType sosList = ds.getSOSList();

        GetSOSListResponseDocument responseDoc = GetSOSListResponseDocument.Factory.newInstance();
        responseDoc.addNewGetSOSListResponse().setSosList(sosList);
        return responseDoc;
    }

    /**
     * Retrieves a list of registered services that are registered with the UICDS core.
     * 
     * @param GetServiceListRequestDocument
     * 
     * @return GetServiceListResponseDocument
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ServiceConfig.xsd">Appendix: ServiceConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "GetServiceListRequest")
    public GetServiceListResponseDocument GetServiceList(GetServiceListRequestDocument requestDoc) {

        String coreName = requestDoc.getGetServiceListRequest().getCoreName();
        log.debug("GetServiceList (coreName=" + coreName + ")");

        ServiceConfigListType serviceList = ds.getServiceList(coreName);

        GetServiceListResponseDocument responseDoc = GetServiceListResponseDocument.Factory.newInstance();
        responseDoc.addNewGetServiceListResponse().setServiceList(serviceList);
        return responseDoc;
    }

    /**
     * Registers an external data source that implements the Open Search interface with the UICDS
     * core.
     * 
     * @param RegisterExternalDataSourceRequestDocument
     * 
     * @return None
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ExternalDataSourceConfig.xsd">Appendix:
     *      ExternalDataSourceConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "RegisterExternalDataSourceRequest")
    public void RegisterExternalDataSource(RegisterExternalDataSourceRequestDocument requestDoc) {

        String urn = requestDoc.getRegisterExternalDataSourceRequest().getURN();
        log.debug("RegisterExternalDataSource (urn=" + urn + ")");

        ds.registerExternalDataSource(urn);
    }

    /**
     * Registers an external tool that implements the WPS interface with the UICDS core.
     * 
     * @param RegisterExternalToolRequestDocument
     * 
     * @return None
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ExternalToolConfig.xsd">Appendix:
     *      ExternalToolConfig.xsd</a>
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "RegisterExternalToolRequest")
    public void RegisterExternalTool(RegisterExternalToolRequestDocument requestDoc) {

        String urn = requestDoc.getRegisterExternalToolRequest().getExternalTool().getURN();
        String toolName = requestDoc.getRegisterExternalToolRequest().getExternalTool().getToolName();
        WorkProductTypeListType publishedProducts = requestDoc.getRegisterExternalToolRequest().getExternalTool().getPublishedProducts();
        WorkProductTypeListType subscribedProducts = requestDoc.getRegisterExternalToolRequest().getExternalTool().getSubscribedProducts();

        log.debug("RegisterExternalTool (urn: " + urn + " publishedProducts=" + publishedProducts
            + ")");

        ds.registerExternalTool(urn, toolName, publishedProducts, subscribedProducts);
    }

    /**
     * Registers a UICDS service with the UICDS core.
     * 
     * @param RegisterUICDSServiceRequestDocument
     * 
     * @return None
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ServiceConfig.xsd">Appendix: ServiceConfig.xsd</a>
     * 
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "RegisterUICDSServiceRequest")
    public void RegisterUICDSService(RegisterUICDSServiceRequestDocument requestDoc) {

        String urn = requestDoc.getRegisterUICDSServiceRequest().getService().getURN();
        String serviceName = requestDoc.getRegisterUICDSServiceRequest().getService().getServiceName();
        WorkProductTypeListType publishedProducts = requestDoc.getRegisterUICDSServiceRequest().getService().getPublishedProducts();
        WorkProductTypeListType subscribedProducts = requestDoc.getRegisterUICDSServiceRequest().getService().getSubscribedProducts();

        log.debug("RegisterUICDSService (serviceName: " + serviceName);
        ds.registerUICDSService(urn, serviceName, publishedProducts, subscribedProducts);
    }

    /**
     * Registers an OGC Sensor Observation Service platform with the UICDS core.
     * 
     * @param RegisterSOSRequestDocument
     * 
     * @return None
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/SOSConfig.xsd">Appendix: SOSConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "RegisterSOSRequest")
    public void RegisterSOS(RegisterSOSRequestDocument requestDoc) {

        String sosURN = requestDoc.getRegisterSOSRequest().getSos().getURN();
        String sosID = requestDoc.getRegisterSOSRequest().getSos().getServiceID();

        log.debug("RegisterSOS [sosID: " + sosID + " ,sosURN:" + sosURN + "]");
        ds.registerSOS(sosID, sosURN);
    }

    /**
     * Unregisters an external data source.
     * 
     * @param UnregisterExternalDataSourceRequestDocument
     * 
     * @return None
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ExternalDataSourceConfig.xsd">Appendix:
     *      ExternalDataSourceConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "UnregisterExternalDataSourceRequest")
    public void UnregisterExternalDataSource(UnregisterExternalDataSourceRequestDocument request) {

        String urn = request.getUnregisterExternalDataSourceRequest().getURN();
        log.debug("UnregisterDataSource (urn: " + urn);
        ds.unregisterExternalDataSource(urn);
    }

    /**
     * Unregisters an external tool.
     * 
     * @param UnregisterExternalToolRequestDocument
     * 
     * @return None
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ExternalToolConfig.xsd">Appendix:
     *      ExternalToolConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "UnregisterExternalToolRequest")
    public void UnregisterExternalTool(UnregisterExternalToolRequestDocument request) {

        String urn = request.getUnregisterExternalToolRequest().getURN();
        log.debug("UnregisterExternalTool (urn: " + urn);
        ds.unregisterExternalTool(urn);
    }

    /**
     * Unregisters a UICDS service.
     * 
     * @param UnregisterUICDSServiceRequestDocument
     * 
     * @return None
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/ServiceConfig.xsd">Appendix: ServiceConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "UnregisterUICDSServiceRequest")
    public void UnregisterUICDSService(UnregisterUICDSServiceRequestDocument request) {

        String serviceName = request.getUnregisterUICDSServiceRequest().getServiceName();
        log.debug("UnregisterUICDSService (serviceName: " + serviceName);
        ds.unregisterUICDSService(serviceName);
    }

    /**
     * Unregisters a OGC Sensor Observation Service.
     * 
     * @param UnregisterSOSRequestRocument
     * 
     * @return None
     * @see <a href="../../services/Directory/0.1/DirectoryService.xsd">Appendix: DirectoryService.xsd</a>
     * @see <a href="../../services/Directory/0.1/SOSConfig.xsd">Appendix: SOSConfig.xsd</a>
     * @idd
     */
    @PayloadRoot(namespace = NS_DirectoryService, localPart = "UnregisterSOSRequest")
    public void UnregisterSOS(UnregisterSOSRequestDocument request) {

        String sosID = request.getUnregisterSOSRequest().getSosID();
        log.debug("UnregisterSOS (sosURN: " + sosID);
        ds.unregisterSOS(sosID);
    }
}
