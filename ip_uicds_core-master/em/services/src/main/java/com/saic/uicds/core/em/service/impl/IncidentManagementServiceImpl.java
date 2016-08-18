package com.saic.uicds.core.em.service.impl;

import gov.niem.niem.niemCore.x20.AddressFullTextDocument;
import gov.niem.niem.niemCore.x20.AreaType;
import gov.niem.niem.niemCore.x20.CircularRegionType;
import gov.niem.niem.niemCore.x20.LocationType;
import gov.niem.niem.niemCore.x20.OrganizationType;
import gov.niem.niem.niemCore.x20.TextType;
import gov.ucore.ucore.x20.DigestDocument;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlCursor;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.transaction.annotation.Transactional;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.IncidentInfoDocument;
import org.uicds.incidentManagementService.IncidentInfoType;
import org.uicds.incidentManagementService.IncidentListType;
import org.uicds.incidentManagementService.ShareIncidentRequestDocument.ShareIncidentRequest;

import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert.Info;
import x1.oasisNamesTcEmergencyCap1.AlertDocument.Alert.Info.Area;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.uicds.core.em.dao.IncidentDAO;
import com.saic.uicds.core.em.messages.IncidentStateNotificationMessage;
import com.saic.uicds.core.em.model.Incident;
import com.saic.uicds.core.em.service.IncidentManagementService;
import com.saic.uicds.core.em.util.DigestGenerator;
import com.saic.uicds.core.em.util.EMGeoUtil;
import com.saic.uicds.core.em.util.IncidentUtil;
import com.saic.uicds.core.infrastructure.exceptions.InvalidInterestGroupIDException;
import com.saic.uicds.core.infrastructure.exceptions.UICDSException;
import com.saic.uicds.core.infrastructure.messages.DeleteJoinedInterestGroupMessage;
import com.saic.uicds.core.infrastructure.messages.InterestGroupStateNotificationMessage;
import com.saic.uicds.core.infrastructure.messages.JoinedInterestGroupNotificationMessage;
import com.saic.uicds.core.infrastructure.messages.ProductChangeNotificationMessage;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.InterestGroupManagementComponent;
import com.saic.uicds.core.infrastructure.service.PubSubNotificationService;
import com.saic.uicds.core.infrastructure.service.PubSubService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.InterestGroupInfo;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.uicds.core.infrastructure.util.LogEntry;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.ServletUtil;
import com.saic.uicds.core.infrastructure.util.UUIDUtil;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;
import com.vividsolutions.jts.geom.Point;

/**
 * The IncidentManagementService implementation.
 * 
 * @author Daphne Hurrell
 * @author Daniel Huang
 * @since 1.0
 * @see com.saic.uicds.core.em.model.Incident Incident Data Model
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
@Transactional
public class IncidentManagementServiceImpl
    implements IncidentManagementService, PubSubNotificationService, ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(IncidentManagementServiceImpl.class);

    private IncidentDAO incidentDAO;

    private WorkProductService workProductService;

    private DirectoryService directoryService;

    private PubSubService pubSubService;

    private ConfigurationService configurationService;

    private InterestGroupManagementComponent interestGroupManagementComponent;

    // TODO: temporary - this channel shall be removed once the messages
    // sent on it have been replaced with simple method invocations to other domain services.
    private MessageChannel incidentStateNotificationChannel;

    // remotely-owned work products pending update
    // key: productID
    // value: incident
    private final Map<String, String> pendingRemoteUpdateRequests = new HashMap<String, String>();

    private String xsltFilePath;

    private String iconConfigXmlFilePath;

    private javax.xml.transform.Source xsltSource;

    private ClassPathResource xsltResource;

    private javax.xml.transform.TransformerFactory transformerFactory;

    private javax.xml.transform.Transformer transformer;

    //private DigestGenerator digestGenerator;

    private UICDSIncidentType alertToIncident(Alert alert) {

        // System.out.println("alertToIncident: alert=[" + alert.toString() + "]");
        UICDSIncidentType theIncident = UICDSIncidentType.Factory.newInstance();

        // set activity name
        String incidentName = "No incident description.";
        if (alert.sizeOfInfoArray() > 0) {
            incidentName = alert.getInfoArray()[0].getEvent();
        } else {
            incidentName = alert.getIdentifier();
        }

        TextType[] activityNameArray = new TextType[1];
        activityNameArray[0] = TextType.Factory.newInstance();
        activityNameArray[0].setStringValue(incidentName);
        theIncident.setActivityNameArray(activityNameArray);

        TextType[] activityCategoryText = new TextType[1];
        activityCategoryText[0] = TextType.Factory.newInstance();

        TextType[] activityDescriptionText = new TextType[1];
        activityDescriptionText[0] = TextType.Factory.newInstance();

        // process the FIRST alert.info
        int sizeOfInfo = alert.sizeOfInfoArray();
        if (sizeOfInfo > 0) {
            Info[] infos = alert.getInfoArray();
            // set activity type
            activityCategoryText[0].setStringValue(infos[0].getCategoryArray(0).toString());

            activityDescriptionText[0].setStringValue(infos[0].getDescription());

            if (infos[0].sizeOfAreaArray() > 0) {
                // set the area data into IncidentType.IncidentLocation.LocationArea
                Area[] areas = infos[0].getAreaArray();
                for (Area area : areas) {

                    LocationType incidentLocation = theIncident.addNewIncidentLocation();
                    String[] stringValues = area.getPolygonArray();
                    // if there are polygon points then save them into
                    // IncidentLocation.LocationArea.AreaPolygonGeographicCoordinate
                    if (stringValues != null) {
                        for (String polygonString : stringValues) {
                            // assign the polygon into
                            // IncidentType.IncidentLocation.LocationArea.Polygon
                            AreaType polygonArea = EMGeoUtil.getPoloygon(polygonString);
                            incidentLocation.addNewLocationArea().set(polygonArea);
                        }
                    }
                    // if there are circle then save them into
                    // IncidentLocation.LocationArea.AreaCircleRegion
                    stringValues = area.getCircleArray();
                    if (stringValues != null && stringValues.length > 0) {
                        for (String circleString : stringValues) {
                            // each point is a circle with center coordinate and radius
                            CircularRegionType theCircle = EMGeoUtil.getCircle(circleString);
                            if (theCircle != null
                                && theCircle.getCircularRegionCenterCoordinateArray().length > 0) {
                                incidentLocation.addNewLocationArea().addNewAreaCircularRegion().set(
                                    theCircle);
                            }
                        }
                    }
                }
            }
        } else {
            activityCategoryText[0].setStringValue(Alert.Info.Category.OTHER.toString());
        }

        theIncident.setActivityCategoryTextArray(activityCategoryText);

        theIncident.setActivityDescriptionTextArray(activityDescriptionText);

        // set the IncidentType.IncidentLocation.LocationAddress
        LocationType[] incidentLocationArray;
        if (theIncident.sizeOfIncidentLocationArray() == 0) {
            incidentLocationArray = new LocationType[1];
            incidentLocationArray[0] = LocationType.Factory.newInstance();
        } else {
            incidentLocationArray = theIncident.getIncidentLocationArray();
        }
        AddressFullTextDocument ad = AddressFullTextDocument.Factory.newInstance();
        ad.addNewAddressFullText().setStringValue(alert.getAddresses());
        incidentLocationArray[0].addNewLocationAddress().set(ad);
        theIncident.setIncidentLocationArray(incidentLocationArray);

        // TRAC #272
        XmlCursor cursor = theIncident.addNewActivityDateRepresentation().newCursor();
        cursor.setName(new QName("http://niem.gov/niem/niem-core/2.0", "ActivityDate"));
        cursor.setTextValue(alert.getSent().toString());
        cursor.dispose();

        OrganizationType[] incidentJurisdictionalOrganizationArray = new OrganizationType[1];
        incidentJurisdictionalOrganizationArray[0] = OrganizationType.Factory.newInstance();

        TextType[] organizationNameArray = new TextType[1];
        organizationNameArray[0] = TextType.Factory.newInstance();
        organizationNameArray[0].setStringValue("Organization Name");

        incidentJurisdictionalOrganizationArray[0].setOrganizationNameArray(organizationNameArray);
        theIncident.setIncidentJurisdictionalOrganizationArray(incidentJurisdictionalOrganizationArray);

        return theIncident;
    }

    /**
     * Archive incident. find the work products and deleted them. delete the interest group. find
     * the incident model and deleted it.
     * 
     * @param incidentID the incident id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus archiveIncident(String incidentID) {

        ProductPublicationStatus status = validateIncident(incidentID);
        if (status != null) {
            return status;
        }

        status = new ProductPublicationStatus();
        status.setStatus(ProductPublicationStatus.SuccessStatus);

        // find the work products, mark them to be deleted
        WorkProduct[] products = getWorkProductService().getAssociatedWorkProductList(incidentID);
        for (WorkProduct product : products) {
            if (product.isActive() == true) {
                return new ProductPublicationStatus(incidentID + " contains "
                    + product.getProductID() + " which needs to be closed first");
            }
            log.debug("delete " + product.getProductID() + ", Ver.: " + product.getProductVersion());
            getWorkProductService().deleteWorkProduct(product.getProductID());
        }

        // we will delete the interest group now.
        try {
            log.debug("ask InterestGroupManagementComponent to delete InterestGroup: " + incidentID);
            getInterestGroupManagementComponent().deleteInterestGroup(incidentID);
        } catch (InvalidInterestGroupIDException e) {
            e.printStackTrace();
            return new ProductPublicationStatus("delete interest group: " + incidentID + ": "
                + e.getMessage());
        }

        // find the incident model and mark it to be deleted
        log.debug("DELETE " + incidentID + " ...");
        try {
            incidentDAO.delete(incidentID, true);
            log.debug("DELETED " + incidentID + " ...");
        } catch (HibernateException e) {
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(e.getMessage());
            log.error("archiveIncident: HibernateException deleting incidentDAO: " + e.getMessage()
                + " from " + e.toString());
        } catch (Exception e) {
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(e.getMessage());
            log.error("archiveIncident: Exception deleting incidentDAO: " + e.getMessage()
                + " from " + e.toString());
        }

        // make a performance log entry
        LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
        logEntry.setAction(LogEntry.ACTION_INCIDENT_ARCHIVE);
        logEntry.setIncidentId(incidentID);
        logEntry.setUpdatedBy(ServletUtil.getPrincipalName());
        log.info(logEntry.getLogEntry());

        return status;
    }

    /**
     * Close incident. Find the work products, mark them to be deleted. Find the incident model and
     * mark it to be deleted.
     * 
     * @param incidentID the incident id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus closeIncident(String incidentID) {

        ProductPublicationStatus status = validateIncident(incidentID);
        if (status != null) {
            return status;
        }

        status = new ProductPublicationStatus();
        status.setStatus(ProductPublicationStatus.SuccessStatus);

        // find the work products, mark them to be deleted
        WorkProduct[] products = getWorkProductService().getAssociatedWorkProductList(incidentID);
        for (WorkProduct product : products) {
            if (product != null && product.isActive()) {
                log.debug("mark " + product.getProductID() + ", Ver.: "
                    + product.getProductVersion() + " as Deleted/InActive");
                getWorkProductService().closeProduct(
                    WorkProductHelper.getWorkProductIdentification(product));
            }
        }

        // find the incident model and mark it to be deleted
        log.debug("mark " + incidentID + " as Deleted/InActive");
        try {
            incidentDAO.delete(incidentID, false);
        } catch (HibernateException e) {
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(e.getMessage());
            log.error("closeIncident: HibernateException deleting incidentDAO: " + e.getMessage()
                + " from " + e.toString());
        } catch (Exception e) {
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(e.getMessage());
            log.error("closeIncident: Exception deleting incidentDAO: " + e.getMessage() + " from "
                + e.toString());
        }

        // make a performance log entry
        LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
        logEntry.setAction(LogEntry.ACTION_INCIDENT_CLOSE);
        logEntry.setIncidentId(incidentID);
        logEntry.setUpdatedBy(ServletUtil.getPrincipalName());
        log.info(logEntry.getLogEntry());

        return status;
    }

    /**
     * Creates the incident. Create interest group for the incident. Set the digest for the work
     * product. Persist the incident.
     * 
     * @param incident the incident
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createIncident(UICDSIncidentType incident) {

        // set the incident ID first
        String owningCore = getConfigurationService().getCoreName();

        // create interest group for the incident
        InterestGroupInfo igInfo = new InterestGroupInfo();
        igInfo.setInterestGroupID(null);
        igInfo.setInterestGroupType(IncidentManagementService.InterestGroupType);
        igInfo.setName(getIncidentName(incident));
        igInfo.setDescription(getIncidentDescription(incident));
        igInfo.setOwningCore(owningCore);
        igInfo.setInterestGroupSubType(getIncidentActivityCategory(incident));
        String interestGroupID = interestGroupManagementComponent.createInterestGroup(igInfo);
        setIncidentID(incident, interestGroupID);

        // we will setup these administrative information, not user
        incident.setOwningCore(owningCore);
        incident.setSharedCoreNameArray(null);

        IncidentDocument incidentDoc = IncidentDocument.Factory.newInstance();
        incidentDoc.addNewIncident().set(incident);

        WorkProduct newWp = newWorkProduct(incidentDoc, null);

        // Create the digest if we have an XSLT .
        // test env doesn't set this via Spring:
        if (xsltFilePath == null)
            xsltFilePath = "xslt/IncidentDigest.xsl";
        if (iconConfigXmlFilePath == null)
            iconConfigXmlFilePath = "xml/types_icons.xml";
        DigestGenerator digestGenerator = new DigestGenerator(xsltFilePath, iconConfigXmlFilePath);
        DigestDocument digestDoc = digestGenerator.createDigest(incidentDoc);
        // log.info("digestDoc="+digestDoc);
        newWp.setDigest(digestDoc);

        // set the digest for the work product
        // newWp.setDigest(new EMDigestHelper(incidentDoc.getIncident()).getDigest());
        // log.info("EMDigestHelper Digest="+new
        // EMDigestHelper(incidentDoc.getIncident()).getDigest().toString());

        ProductPublicationStatus status = getWorkProductService().publishProduct(newWp);

        WorkProduct wp = null;
        if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
            wp = status.getProduct();
            // set the incident work product ID to the incidentDoc
            // setIncidentWPID(incidentDoc.getIncident(), wp.getProductID());

            // persist the Incident model again to persist the work product ID

            persistIncident(incidentDoc.getIncident(), wp.getProductID());

            // send the IncidentStateNotificationMessage.State.NEW message
            sendIncidentStateChangeMessages(InterestGroupStateNotificationMessage.State.NEW,
                getIncidentDAO().findByIncidentID(getIncidentID(incidentDoc.getIncident())), igInfo);
        }

        // make a performance log entry
        LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
        logEntry.setAction(LogEntry.ACTION_INCIDENT_CREATE);
        logEntry.setCoreName(owningCore);
        logEntry.setIncidentId(interestGroupID);
        logEntry.setCreatedBy(ServletUtil.getPrincipalName());
        if (incident.sizeOfActivityCategoryTextArray() > 0) {
            logEntry.setIncidentType(incident.getActivityCategoryTextArray(0).getStringValue());
        } else {
            logEntry.setIncidentType(wp.getProductType());
        }
        log.info(logEntry.getLogEntry());

        return status;
    }

    /**
     * Creates the incident from cap. Convert the CAP alert to a UICDSIncidentType and then create
     * the incident.
     * 
     * @param alert the alert
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createIncidentFromCap(Alert alert) {

        if (alert == null)
            return null;

        return createIncident(alertToIncident(alert));
    }

    /**
     * Delete incident.
     * 
     * @param incidentID the incident id
     * @return true, if successful
     */
    @Override
    public boolean deleteIncident(String incidentID) {

        // TODO
        log.debug("delete incident: " + incidentID + " ...");
        return false;
    }

    /** {@inheritDoc} */
    public ConfigurationService getConfigurationService() {

        return this.configurationService;
    }

    /** {@inheritDoc} */
    public DirectoryService getDirectoryService() {

        return this.directoryService;
    }

    /**
     * Gets the incident using the workProduct Id String.
     * 
     * @param incidentWPID the incident wpid
     * @return the incident
     * @ssdd
     */
    @Override
    public WorkProduct getIncident(String incidentWPID) {

        return incidentWPID != null ? getWorkProductService().getProduct(incidentWPID) : null;
    }

    /** {@inheritDoc} */
    public IncidentDAO getIncidentDAO() {

        return incidentDAO;
    }

    // private ArrayList<IncidentStateObserver> observers = new ArrayList<IncidentStateObserver>();

    private String getIncidentDateRepresentation(UICDSIncidentType incident) {

        String dateRepresentation = null;
        if (incident.sizeOfActivityDateRepresentationArray() > 0) {
            dateRepresentation = incident.getActivityDateRepresentationArray(0).toString();
        }
        return dateRepresentation;
    }

    private String getIncidentDescription(UICDSIncidentType incident) {

        String incidentDescription = null;
        if (incident.sizeOfActivityDescriptionTextArray() > 0) {
            incidentDescription = incident.getActivityDescriptionTextArray(0).getStringValue();
        }
        return incidentDescription;
    }

    private String getIncidentActivityCategory(UICDSIncidentType incident) {

        String incidentActivityCategory = null;
        if (incident.sizeOfActivityCategoryTextArray() > 0) {
            incidentActivityCategory = incident.getActivityCategoryTextArray(0).getStringValue();
        }
        return incidentActivityCategory;
    }

    private String getIncidentID(UICDSIncidentType incident) {

        String incidentID = null;
        if (incident.sizeOfActivityIdentificationArray() > 0) {
            if (incident.getActivityIdentificationArray(0).sizeOfIdentificationIDArray() > 0) {
                incidentID = incident.getActivityIdentificationArray(0).getIdentificationIDArray(0).getStringValue();
            }
        }
        return incidentID;
    }

    /**
     * Gets the incident description info from the incident identifier.
     *
     * NOT synchronized
     *
     * @param incidentID the incident id
     * @return the incident info
     * @ssdd
     */
    public IncidentInfoType getIncidentInfo(String incidentID) {

        InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(incidentID);
        Incident incident = incidentDAO.findByIncidentID(incidentID);
        if (incident != null) {
            return toIncidentInfoType(incident, igInfo);
        }
        return null;
    }

    private String getIncidentName(UICDSIncidentType incident) {

        String incidentName = null;
        if (incident.sizeOfActivityNameArray() > 0) {
            incidentName = incident.getActivityNameArray(0).getStringValue();
        }
        return incidentName;
    }

    private String getIncidentWPID(UICDSIncidentType incident) {

        return incident.getId();
    }

    public InterestGroupManagementComponent getInterestGroupManagementComponent() {

        return interestGroupManagementComponent;
    }

    /**
     * Gets the list of closed incident.
     *
     * Not: synchronized
     *
     * @return the list of closed incident
     * @ssdd
     */
    @Override
    public String[] getListOfClosedIncident() {

        List<Incident> closedIncidents = incidentDAO.findAllClosedIncident();
        String[] incidentIDList = null;
        if (closedIncidents.size() > 0) {
            incidentIDList = new String[closedIncidents.size()];
            int i = 0;
            for (Incident incident : closedIncidents) {
                incidentIDList[i++] = incident.getIncidentId();
            }
        }

        return incidentIDList;
    }

    /**
     * Gets the list of incidents.
     *
     * Not: synchronized
     *
     * @return the list of incidents
     * @ssdd
     */
    @Override
    public IncidentListType getListOfIncidents() {

        IncidentListType response = IncidentListType.Factory.newInstance();
        List<Incident> incidents = incidentDAO.findAll();
        if (incidents != null && incidents.size() > 0) {
            List<IncidentInfoType> infoList = new ArrayList<IncidentInfoType>();
            for (Incident incident : incidents) {
                InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(incident.getIncidentId());
                if (igInfo != null) {
                    IncidentInfoType incidentInfo = toIncidentInfoType(incident, igInfo);
                    if (incidentInfo != null)
                        infoList.add(incidentInfo);
                }
            }
            if (infoList.size() > 0) {
                IncidentInfoType[] infos = new IncidentInfoType[infoList.size()];
                infos = infoList.toArray(infos);
                response.setIncidentInfoArray(infos);
            }
        }

        return response;
    }

    /**
     * Gets the list of incident work products.
     *
     * Not: synchronized
     *
     * @return the list of incident work products
     * @ssdd
     */
    @Override
    public ArrayList<WorkProduct> getIncidentList() {

        ArrayList<WorkProduct> workProducts = new ArrayList<WorkProduct>();

        List<Incident> incidents = incidentDAO.findAll();
        if (incidents != null && incidents.size() > 0) {
            for (Incident incident : incidents) {
                WorkProduct wp = workProductService.getProduct(incident.getWorkProductID());
                if (wp != null) {
                    workProducts.add(wp);
                }
            }
        }

        return workProducts;
    }

    /**
     * Gets the service name.
     * 
     * @return the service name
     * @ssdd
     */
    @Override
    public String getServiceName() {

        return IncidentManagementService.IMS_SERVICE_NAME;
    }

    // end of temporary code
    /** {@inheritDoc} */
    public WorkProductService getWorkProductService() {

        return workProductService;
    }

    /*
     * Initialize the cache and send UPDATE message to Directory Service for each incidents
     */
    private void init() {

        List<Incident> incidents = incidentDAO.findAll();
        if (incidents != null) {
            for (Incident incident : incidents) {
                InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(incident.getIncidentId());
                sendIncidentStateChangeMessages(
                    InterestGroupStateNotificationMessage.State.RESTORE, incident, igInfo);
            }
        }
    }

    /**
     * Invalid xpath notification.
     * 
     * @param subscriptionId the subscription id
     * @param errorMessage the error message
     */
    @Override
    public void InvalidXpathNotification(Integer subscriptionId, String errorMessage) {

        log.error(errorMessage + " for Subscriber: " + subscriptionId);
    }

    public synchronized void deleteJoinedInterestGroupHandler(
        DeleteJoinedInterestGroupMessage message) {

        // the interest group has been deleted, what do i do next ???
        String incidentID = message.getInterestGroupID();

        // find the work products, mark them to be deleted
        WorkProduct[] products = getWorkProductService().getAssociatedWorkProductList(incidentID);
        for (WorkProduct product : products) {
            log.debug("deleteJoinedInterestGroupHandler: purge product: " + product.getProductID());
            getWorkProductService().purgeWorkProduct(product.getProductID());
        }

        // find the incident model and mark it to be deleted
        log.debug("Joined Core: delete incident: " + incidentID + " ...");
        try {
            incidentDAO.delete(incidentID, true);
        } catch (HibernateException e) {
            log.error("deleteJoinedInterestGroupHandler: HibernateException deleting incidentDAO: "
                + e.getMessage() + " from " + e.toString());
        } catch (Exception e) {
            log.error("deleteJoinedInterestGroupHandler: Exception deleting incidentDAO: "
                + e.getMessage() + " from " + e.toString());
        }
    }

    /**
     * Not: synchronized
     *
     * @param message
     */
    public void newJoinedInterestGroupHandler(
        JoinedInterestGroupNotificationMessage message) {

        log.info("newJoinedInterestGroupHandler: receive new joined incident notification incidentID="
            + message.interestGroupID
            + " incidentType="
            + message.interestGroupType
            + " owner="
            + message.owner);

        if (message.interestGroupType.equals(IncidentManagementService.InterestGroupType)) {

            Incident incident = getIncidentDAO().findByIncidentID(message.interestGroupID);

            if (incident == null) {
                try {
                    IncidentInfoDocument incidentInfoDocument = IncidentInfoDocument.Factory.parse(message.interestGroupInfo);

                    IncidentInfoType incidentInfo = incidentInfoDocument.getIncidentInfo();

                    incident = new Incident();
                    incident.setIncidentId(incidentInfo.getId());
                    incident.setWorkProductID(incidentInfo.getWorkProductIdentification().getIdentifier().getStringValue());
                    incident.setLatitude(incidentInfo.getLatitude());
                    incident.setLongitude(incidentInfo.getLongitude());
                    try {
                        getIncidentDAO().makePersistent(incident);
                    } catch (HibernateException e) {
                        log.error("newJoinedInterestGroupHandler: HibernateException makePersistent incidentDAO: "
                            + e.getMessage() + " from " + e.toString());
                    } catch (Exception e) {
                        log.error("newJoinedInterestGroupHandler: Exception makePersistent incidentDAO: "
                            + e.getMessage() + " from " + e.toString());
                    }

                    InterestGroupInfo igInfo = interestGroupManagementComponent.getInterestGroup(incident.getIncidentId());
                    if (igInfo != null) {
                        sendIncidentStateChangeMessages(
                            InterestGroupStateNotificationMessage.State.JOIN, incident, igInfo);

                        // make a performance log entry
                        LogEntry logEntry = new LogEntry();
                        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
                        logEntry.setAction(LogEntry.ACTION_INCIDENT_JOIN);
                        logEntry.setCoreName(getDirectoryService().getCoreName());
                        logEntry.setIncidentId(incident.getIncidentId());
                        logEntry.setJoinCoreName(message.getOwner());
                        log.info(logEntry.getLogEntry());

                    } else {
                        log.error("newJoinedInterestGroupHandler: Failed to retrieve interest group info for  "
                            + incident.getIncidentId() + "from interestGroupManagementComponent");
                    }
                } catch (Throwable e) {
                    log.error("newJoinedInterestGroupHandler: error parsing received incident info");
                    e.printStackTrace();
                }
            }
        }
    }

    private WorkProduct newWorkProduct(IncidentDocument incidentDoc, String productID) {

        WorkProduct wp = new WorkProduct();
        String incidentID = getIncidentID(incidentDoc.getIncident());
        if (incidentID == null) {
            log.error("Missing incident ID from [" + incidentDoc.toString() + "]");
        }
        wp.getAssociatedInterestGroupIDs().add(incidentID);
        wp.setProductType(Type);

        if (productID == null) {
            productID = UUIDUtil.getID(IncidentManagementService.Type);
            // addWPIDToIncident(incidentDoc.getIncident(), productID);
        }

        incidentDoc.getIncident().setId(productID);
        wp.setProductID(productID);
        // log.debug("newWorkProduct: incidentDoc's wp=[" + incidentDoc.toString() + "]");
        wp.setProduct(incidentDoc);
        return wp;
    }

    /**
     * New work product version. Checks if a subscription was made for a pending update and if so,
     * cancels the subscription, sends incident state change notifications and remove pending update
     * requests.
     * 
     * When there is new version of Incident document, we need to update the Incident and Interest
     * Group Models.
     * 
     * @param workProductID the work product id
     * @param subscriptionId the subscription id
     * @ssdd
     */
    @Override
    public void newWorkProductVersion(String workProductID, Integer subscriptionId) {

        // Verify the subscription was made for a pending update
        log.debug("newWorkProductVersion: incident wpID=" + workProductID + " subscriptionId="
            + subscriptionId);

        // When the Incident document has updated on the remote core successfully, we need to
        // update the Incident & InterestGroup model
        WorkProduct product = workProductService.getProduct(workProductID);
        if (product != null) {
            IncidentDocument theIncident = null;
            try {
                theIncident = (IncidentDocument) product.getProduct();
            } catch (Exception e) {
                log.error("the work product is not a valid Incident Document\n" + e.getMessage()
                    + "\n" + product.getProduct().xmlText());
                return;
            }
            // if owning core is not itself and there is no interest group created
            // then it's the first Incident document after the sharing, no update needed
            String owningCore = theIncident.getIncident().getOwningCore();
            String interestGroupId = getIncidentID(theIncident.getIncident());
            if (interestGroupManagementComponent.getInterestGroup(interestGroupId) == null) {
                if (owningCore.equals(directoryService.getCoreName())) {
                    log.error(directoryService.getCoreName()
                        + " is the owner, but no Interest Group exists");
                } else {
                    log.debug("this is the first Incident Document update on the joined core");
                }
                return;
            }
            // we know this interest group so only update the models if we do not own this
            // incident.
            else {
                if (!owningCore.equals(directoryService.getCoreName())) {
                    log.debug("newWorkProductVersion: Update: Incident:" + product.getProductID()
                        + ", Version: " + product.getProductVersion() + ", owned by core: "
                        + owningCore);
                    updateIncidentModelAndInterestGroupInfo(theIncident.getIncident(), owningCore,
                        product.getProductID());
                }
            }
        }
    }

    /**
     * Notify of incident state change.
     * 
     * @param notification the notification
     * @ssdd
     */
    public void notifyOfIncidentStateChange(IncidentStateNotificationMessage notification) {

        log.info("#### notifyOfIncidentStateChange() called with incident:"
            + notification.getIncidentInfo().getId() + " state:" + notification.getState()
            + " thead:" + Thread.currentThread().getName());
        Message<IncidentStateNotificationMessage> message = new GenericMessage<IncidentStateNotificationMessage>(
            notification);
        incidentStateNotificationChannel.send(message);

    }

    /**
     * public void owningCoreProductNotificationHandler(PublishEDXLProductMessage message) {
     * log.debug("owningCoreProductNotificationHandler: receive product notification"); if
     * (message.getProductType().equals(Type)) { log .info(
     * "owningCoreProductNotificationHandler: receive notification of an incident work product published by core:"
     * + message.getOwningCore()); EDXLDistributionDocument doc = message.getEdxlProduct(); String
     * owningCore = message.getOwningCore(); ContentObjectType content =
     * doc.getEDXLDistribution().getContentObjectArray(0); if (content == null) {
     * log.error("No content: " + doc.toString()); return; } String productType =
     * EDXLDistributionHelper.getProductType(content); String productID =
     * EDXLDistributionHelper.getProductID(content); String incidentID = content.getIncidentID();
     * IncidentDocument incidentDoc = null; try { incidentDoc =
     * IncidentDocument.Factory.parse(content.getXmlContent()
     * .getEmbeddedXMLContentArray(0).toString()); } catch (Exception e) {
     * log.error("publishIncidentWorkProductHandler: cannot parse incident: " + e.getMessage());
     * return; } Incident incident = getIncidentDAO().findByIncidentID(incidentID); if (incident ==
     * null) { // persist the incident model incident = new Incident();
     * incident.setIncidentId(incidentID);
     * incident.setName(getIncidentName(incidentDoc.getIncident()));
     * incident.setOwningCore(owningCore); incident.setWorkProductID(productID);
     * getIncidentDAO().makePersistent(incident); } // persist the work product
     * getWorkProductService().publishProduct(doc.getEDXLDistribution());
     * sendIncidentStateChangeMessages(InterestGroupStateNotificationMessage.State.JOIN, incident);
     * } }
     */

    // persist the incident model

    /**
     * Not: synchronized
     *
     * @param incident
     * @param incidentWPID
     */
    private void persistIncident(UICDSIncidentType incident, String incidentWPID) {

        String incidentID = getIncidentID(incident);

        Incident i = getIncidentDAO().findByIncidentID(incidentID);
        if (i == null) {
            i = new Incident();
            i.setIncidentId(incidentID);
            i.setWorkProductID(incidentWPID);
        }
        // After published the work product, we will do another persist and the work product Id will
        // be needed to persisted.
        String incidentWPId = getIncidentWPID(incident);
        if (incidentWPId != null)
            i.setWorkProductID(incidentWPId);

        // Point point = EMGeoUtil.parsePoint(incident);
        Point point = IncidentUtil.getIncidentLocation(incident);
        if (point != null) {
            i.setLatitude(point.getY());
            i.setLongitude(point.getX());
        } else {
            log.debug("persistIncident: Location not found in incident");
        }

        // TODO: what is the DateRepresentation ?
        // setDate(getIncidentDateRepresentation(incident));

        try {
            // r is here only to make EasyMock happy in the unit tests
            Incident r = getIncidentDAO().makePersistent(i);
            r = null;
        } catch (HibernateException e) {
            log.error("persistIncident: HibernateException makePersistent incidentDAO: "
                + e.getMessage() + " from " + e.toString());
        } catch (Exception e) {
            log.error("persistIncident: Exception makePersistent incidentDAO: " + e.getMessage()
                + " from " + e.toString());
        }
    }

    private void sendIncidentStateChangeMessages(InterestGroupStateNotificationMessage.State state,
        Incident incident, InterestGroupInfo igInfo) {

        if (incident == null) {
            log.error("sendIncidentStateChangeMessages: incident is null");
            return;
        }

        if (igInfo == null) {
            log.error("sendIncidentStateChangeMessages: interest group info is null");
            return;
        }

        IncidentInfoType incidentInfo = toIncidentInfoType(incident, igInfo);

        if (incidentInfo == null) {
            log.error("sendIncidentStateChangeMessages: unable to retrieve incidentInfo");
            return;
        }

        // TODO: the sending of this message, IncidentStateChangeMessage, should be changed
        // to a simple method invocation
        IncidentStateNotificationMessage mesg = new IncidentStateNotificationMessage();
        mesg.setState(state);
        mesg.setIncidentInfo(incidentInfo);

        log.debug("sendIncidentStateChangeMessage: \nmessage= IncidentStateNotificationMessage \nstate= "
            + mesg.getState()
            + "\nwpID="
            + mesg.getIncidentInfo().getWorkProductIdentification().getIdentifier().getStringValue()
            + "\nID="
            + mesg.getIncidentInfo().getId()
            + "\ndesc:"
            + mesg.getIncidentInfo().getDescription()
            + "\nlat="
            + mesg.getIncidentInfo().getLatitude()
            + "\nlong="
            + mesg.getIncidentInfo().getLongitude()
            + "\nname:"
            + mesg.getIncidentInfo().getName()
            + " \nowningCore=" + mesg.getIncidentInfo().getOwningCore());
        notifyOfIncidentStateChange(mesg);

    }

	/** {@inheritDoc} */
    public void setConfigurationService(ConfigurationService configurationService) {

        this.configurationService = configurationService;
    }

    /** {@inheritDoc} */
    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    /** {@inheritDoc} */
    public void setIncidentDAO(IncidentDAO incidentDAO) {

        this.incidentDAO = incidentDAO;
    }

    // make the ActivityIdentification to be immutable
    private void setIncidentID(UICDSIncidentType incident, String incidentID) {

        if (incident.sizeOfActivityIdentificationArray() > 0)
            incident.removeActivityIdentification(0);

        if (incidentID != null && incidentID.length() > 0)
            incident.addNewActivityIdentification().addNewIdentificationID().setStringValue(
                incidentID);
    }

    /**
     * @param channel
     */
    public void setIncidentStateNotificationChannel(MessageChannel channel) {

        incidentStateNotificationChannel = channel;
    }

    public void setInterestGroupManagementComponent(
        InterestGroupManagementComponent interestGroupManagementComponent) {

        this.interestGroupManagementComponent = interestGroupManagementComponent;
    }

    public void setInterestGroupManager(
        InterestGroupManagementComponent interestGroupManagementComponent) {

        this.interestGroupManagementComponent = interestGroupManagementComponent;
    }

    public void setPubSubService(PubSubService pubSubService) {

        this.pubSubService = pubSubService;
    }

    /**
     * @param workProductService
     */
    public void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }

    private boolean doShareIncident(ShareIncidentRequest shareIncidentRequest,
        boolean agreementChecked) throws UICDSException {

        String incidentID = shareIncidentRequest.getIncidentID();
        String coreName = shareIncidentRequest.getCoreName();

        IncidentInfoType incidentInfo = getIncidentInfo(incidentID);
        IncidentInfoDocument incidentInfoDoc = IncidentInfoDocument.Factory.newInstance();
        incidentInfoDoc.setIncidentInfo(incidentInfo);

        log.debug("doShareIncident: incidentDoc's string=[" + incidentInfoDoc.toString() + "]");
        try {
            interestGroupManagementComponent.shareInterestGroup(incidentID, coreName,
                incidentInfoDoc.toString(), agreementChecked);

            // TODO: RDW - The shareInterestGroup call sends an asycn message to the joining core
            // We can't wait synchronously for this response so we should put the request in a
            // queue and return. handleJoinRequestResponse will be called when we get a
            // response from the core.

            handleJoinRequestResponse(incidentInfo, coreName);
        } catch (UICDSException e) {
            log.error("Error sharing incident: " + e.getMessage());
            throw e;
        }

        return true;
    }

    private void handleJoinRequestResponse(IncidentInfoType incidentInfo, String coreName)
        throws UICDSException {

        String incidentID = incidentInfo.getId();

        // Notify listeners of a state change to the incident.
        IncidentStateNotificationMessage mesg = new IncidentStateNotificationMessage();
        mesg.setState(InterestGroupStateNotificationMessage.State.SHARE);
        mesg.setIncidentInfo(incidentInfo);

        try {
            notifyOfIncidentStateChange(mesg);
        } catch (Exception e) {
            log.error("Share Incident: " + incidentID + " with " + coreName
                + " incident state listener failure: " + e.getMessage());
            e.printStackTrace();
        }

        // update the incident work product to include the name of the "joined" core
        Incident theIncident = getIncidentDAO().findByIncidentID(incidentID);
        WorkProduct wp = getWorkProductService().getProduct(theIncident.getWorkProductID());
        IncidentDocument incidentWPDoc = null;
        try {
            incidentWPDoc = (IncidentDocument) wp.getProduct();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Incident WP: " + wp.getProductID() + " not a valied incident document");
            throw new UICDSException("Internal Error: Incident WP: " + wp.getProductID()
                + " not a valied incident document");

        }

        String[] sharedCore = incidentWPDoc.getIncident().getSharedCoreNameArray();

        HashSet<String> sharedCoreSet = new HashSet<String>(Arrays.asList(sharedCore));

        if (!sharedCoreSet.contains(coreName)) {
            sharedCoreSet.add(coreName);
            sharedCore = sharedCoreSet.toArray(new String[0]);
            incidentWPDoc.getIncident().setSharedCoreNameArray(sharedCore);
            WorkProduct newWP = new WorkProduct(wp);
            newWP.setProduct(incidentWPDoc);
            ProductPublicationStatus status = getWorkProductService().publishProduct(newWP);
        }

        // make a performance log entry
        LogEntry logEntry = new LogEntry();
        logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
        logEntry.setAction(LogEntry.ACTION_INCIDENT_SHARE);
        logEntry.setCoreName(incidentInfo.getOwningCore());
        logEntry.setIncidentId(incidentID);
        logEntry.setShareCoreName(coreName);
        log.info(logEntry.getLogEntry());
    }

    public void systemInitializedHandler(String message) {

        // register with the directory service
        WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(Type);
        getDirectoryService().registerUICDSService(NS_IncidentManagementService, IMS_SERVICE_NAME,
            typeList, typeList);

        // remove any existing subscriptions for this service (trac#602)
        List<Integer> subscriptionIDs = pubSubService.getSubscriptionsByServiceName(getServiceName());
        for (Integer subscriptionID : subscriptionIDs) {
            pubSubService.unsubscribeBySubscriptionID(subscriptionID);
        }

        // resubscribe to Incident work product updates
        try {
            // pubSubService.
            pubSubService.subscribeWorkProductType(IncidentManagementService.Type, null,
                new HashMap<String, String>(), this);
        } catch (Exception e) {
            log.error("Unable to subscribe to own product type");
            e.printStackTrace();
        }

        //IP DISABLED - NO NEED FOR THESE NOTIFICATION MESSAGES
        //init();

        // log.info("calling DigestGenerator(xsltFilePath)");
        // digestGenerator = new DigestGenerator(xsltFilePath);

    }

    private static String convertStreamToString(InputStream is) throws Exception {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line + "\n");
        }
        is.close();
        return sb.toString();
    }

    /**
     * @return the xsltFilePath
     */
    public String getXsltFilePath() {

        return xsltFilePath;
    }

    /**
     * @param xsltFilePath the xsltFilePath to set
     */
    public void setXsltFilePath(String xsltFilePath) {

        this.xsltFilePath = xsltFilePath;
    }

    /**
     * @return the getIconConfigXmlFilePath
     */
    public String getIconConfigXmlFilePath() {

        return iconConfigXmlFilePath;
    }

    /**
     * @param getIconConfigXmlFilePath to set
     */
    public void setIconConfigXmlFilePath(String iconConfigXmlFilePath) {

        this.iconConfigXmlFilePath = iconConfigXmlFilePath;
    }

    private IncidentInfoType toIncidentInfoType(Incident incident, InterestGroupInfo igInfo) {

        if (incident == null) {
            log.error("toIncidentInfoType - incident is null");
            return null;
        }

        IdentificationType identification = workProductService.getProductIdentification(incident.getWorkProductID());

        if (identification == null) {
            log.error("toIncidentInfoType - unable to retrieve product identification for incident:"
                + incident.getIncidentId() + ", wp: " + incident.getWorkProductID());
            return null;
        }

        IncidentInfoType info = IncidentInfoType.Factory.newInstance();
        info.setId(incident.getIncidentId());
        info.setWorkProductIdentification(identification);
        info.setLatitude(incident.getLatitude());
        info.setLongitude(incident.getLongitude());
        info.setDate((new GregorianCalendar()).getTime().toString());
        info.setName(igInfo.getName());
        info.setDescription(igInfo.getDescription());
        info.setOwningCore(igInfo.getOwningCore());

        return info;
    }

    private InterestGroupInfo updateIncidentModelAndInterestGroupInfo(UICDSIncidentType incident,
        String owningCore, String wpID) {

        // persist the updated incident model after the work product is published
        persistIncident(incident, wpID);

        // Determine who owns the work product being updated
        InterestGroupInfo igInfo = new InterestGroupInfo();
        igInfo.setInterestGroupID(getIncidentID(incident));
        igInfo.setInterestGroupType(IncidentManagementService.InterestGroupType);
        igInfo.setInterestGroupSubType(getIncidentActivityCategory(incident));
        igInfo.setName(getIncidentName(incident));
        igInfo.setDescription(getIncidentDescription(incident));
        igInfo.setOwningCore(owningCore);
        try {
            interestGroupManagementComponent.updateInterestGroup(igInfo);
        } catch (InvalidInterestGroupIDException e) {
            log.error("Caught InvalidInterestGroupIDException while attempting to update interestGroup with ID="
                + getIncidentID(incident));
            return null;
        }
        return igInfo;
    }

    /**
     * Update incident.
     * 
     * @param incident the incident
     * @param pkgId the pkg id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateIncident(UICDSIncidentType incident,
        IdentificationType pkgId) {

        String productID = pkgId.getIdentifier().getStringValue();

        WorkProduct wp = getWorkProductService().getProduct(productID);
        if (incident.sizeOfActivityCategoryTextArray() > 0) {
            log.debug("updateIncident: wpID=" + productID + "category:"
                + incident.getActivityCategoryTextArray(0).toString());
        }

        if (incident.sizeOfIncidentLocationArray() > 0) {
            Point point = IncidentUtil.getIncidentLocation(incident);
            if (point != null) {
                log.debug("===> updateIncidet: lat=" + IncidentUtil.getLatitude(point) + " long="
                    + IncidentUtil.getLongititude(point));
            } else {
                log.debug("===> updateIncidet: - location not specified in the update");
            }
        }

        if (wp == null) {
            log.error("updateIncident: unable to locate incident wp for productID=" + productID);
            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            if (workProductService.isDeleted(productID) == true) {
                status.setReasonForFailure(productID + " is inactive");
            } else {
                status.setReasonForFailure(productID + " cannot be located");
            }
            return status;
        } else {
            IncidentDocument persistedIncidentDoc = null;
            try {
                persistedIncidentDoc = (IncidentDocument) wp.getProduct();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("updateIncident for " + productID + " failed: " + e.getMessage());
                return null;
            }

            // this may need to change when we allow ownership transfer
            incident.setOwningCore(persistedIncidentDoc.getIncident().getOwningCore());

            incident.setSharedCoreNameArray(persistedIncidentDoc.getIncident().getSharedCoreNameArray());
            // enforce the incident Id is immutable
            setIncidentID(incident, getIncidentID(persistedIncidentDoc.getIncident()));

            IncidentDocument incidentDoc = IncidentDocument.Factory.newInstance();
            incidentDoc.setIncident(incident);

            WorkProduct newWp = newWorkProduct(incidentDoc, wp.getProductID());
            newWp = WorkProductHelper.setWorkProductIdentification(newWp, pkgId);

            // Create the digest if we have an XSLT
            DigestGenerator digestGenerator = new DigestGenerator(xsltFilePath, iconConfigXmlFilePath);
            DigestDocument digestDoc = digestGenerator.createDigest(incidentDoc);
            // log.info("digestDoc="+digestDoc);
            newWp.setDigest(digestDoc);

            // set the digest for the work product
            // newWp.setDigest(new EMDigestHelper(incidentDoc.getIncident()).getDigest());

            log.debug("The new work product is \n"+newWp);
            
            // publish the work product
            ProductPublicationStatus status = getWorkProductService().publishProduct(newWp);
            String owningCore = persistedIncidentDoc.getIncident().getOwningCore();

            if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
                InterestGroupInfo igInfo = updateIncidentModelAndInterestGroupInfo(incident,
                    owningCore, status.getProduct().getProductID());

                if (owningCore != null && directoryService.getCoreName().equals(owningCore)
                    && igInfo != null) {
                    // Only send notifications if it's an update by owning core
                    // create interest group for the incident
                    sendIncidentStateChangeMessages(
                        InterestGroupStateNotificationMessage.State.UPDATE,
                        getIncidentDAO().findByIncidentID(getIncidentID(incident)), igInfo);
                }
                // make a performance log entry
                LogEntry logEntry = new LogEntry();
                logEntry.setCategory(LogEntry.CATEGORY_INCIDENT);
                logEntry.setAction(LogEntry.ACTION_INCIDENT_UPDATE);
                logEntry.setCoreName(owningCore);
                if (newWp != null) {
                    logEntry.setIncidentId(newWp.getProductID());
                    logEntry.setIncidentType(newWp.getProductType());
                }
                logEntry.setUpdatedBy(ServletUtil.getPrincipalName());
                log.info(logEntry.getLogEntry());
            }
            // even we put subscription into pending hash, we cannot tell when the
            // newWorkProductVersion is invoked whether it's from original core or not
            /*
            else if (status.getStatus().equals(ProductPublicationStatus.PendingStatus)) {
                try {
                    // subscribe to the work product ID so we can send notifications when the
                    // update is completed. Add the pending update request to map to be use to
                    // check against when receiving new product version notifications.
                    log.debug("updateIncident: workProduct: " + productID + " owned by core ["
                        + owningCore + "]");
                    Integer subscriptionID = pubSubService.subscribeWorkProductIDNewVersions(
                        productID, this);
                    log.debug("updateIncident: subcriptionID=" + subscriptionID);
                    pendingRemoteUpdateRequests.put(subscriptionID.toString(), productID);

                } catch (Exception e) {
                    log.error("Error - updateIncident:  Unable to subscribe to a pending update worproduct; ID="
                        + productID);
                    e.printStackTrace();
                }
            }
            */

            return status;
        }
    }

    private ProductPublicationStatus validateIncident(String incidentID) {

        if (incidentID == null) {

            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure("Empty Incident ID");
            return status;
        }

        if (getIncidentDAO().findByIncidentID(incidentID) == null) {
            return new ProductPublicationStatus("Incident: " + incidentID + " does not exist");
        }

        // if the core is not the owning core then return failure
        if (interestGroupManagementComponent.interestGroupOwnedByCore(incidentID) == false) {

            ProductPublicationStatus status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(getDirectoryService().getCoreName()
                + " doesn't own incident: " + incidentID);
            return status;
        }
        return null;
    }

    /**
     * Work product deleted. Not yet implemented
     * 
     * @param workProductID the work product id
     * @param workProductType the work product type
     * @param subscriptionId the subscription id
     */
    @Override
    public void workProductDeleted(ProductChangeNotificationMessage changedMessage,
        Integer subscriptionId) {

        // Nothing to do for now
    }

    /**
     * Share incident. Adds the core to be shared with to the list of shared cores and re-publish
     * the incident work product This method does not set the agreement checked parameter so
     * interest group state notifications are not triggered.
     * 
     * @param shareIncidentRequest the share incident request
     * @return true, if successful
     * @throws UICDSException the UICDS exception
     * @ssdd
     */
    @Override
    public boolean shareIncident(ShareIncidentRequest shareIncidentRequest) throws UICDSException {

        return doShareIncident(shareIncidentRequest, false);
    }

    /**
     * Share incident agreement checked. This method sets the agreement checked parameter which
     * triggers interest group state notification.
     * 
     * @param shareIncidentRequest the share incident request
     * @return true, if successful
     * @throws UICDSException the UICDS exception
     * @ssdd
     */
    @Override
    public boolean shareIncidentAgreementChecked(ShareIncidentRequest shareIncidentRequest)
        throws UICDSException {

        return doShareIncident(shareIncidentRequest, true);
    }

}
