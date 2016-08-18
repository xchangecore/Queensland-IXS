/*
 * -------------------------- HEADER ------------------------
 */
package com.saic.uicds.core.em.service.impl;

import gov.niem.niem.niemCore.x20.AreaType;
import gov.niem.niem.niemCore.x20.LocationType;
import gov.ucore.ucore.x20.CollectionType;
import gov.ucore.ucore.x20.DigestDocument;
import gov.ucore.ucore.x20.IdentifierType;
import gov.ucore.ucore.x20.StringType;
import gov.ucore.ucore.x20.ThingRefType;
import gov.ucore.ucore.x20.ThingType;
import gov.ucore.ucore.x20.WhatType;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.uicds.core.em.messages.IncidentStateNotificationMessage;
import com.saic.uicds.core.em.service.MapService;
import com.saic.uicds.core.em.util.AtomModule;
import com.saic.uicds.core.em.util.ShapefileIngester;
import com.saic.uicds.core.infrastructure.messages.InterestGroupStateNotificationMessage;
import com.saic.uicds.core.infrastructure.messages.ProductChangeNotificationMessage;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.uicds.core.infrastructure.util.DigestConstant;
import com.saic.uicds.core.infrastructure.util.InfrastructureNamespaces;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.UUIDUtil;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;
import com.saic.uicds.core.infrastructure.util.XmlUtil;
import com.usersmarts.cx.wms.WMC;
import com.usersmarts.cx.wms.api.Layer;
import com.usersmarts.cx.wms.api.Server;
import com.usersmarts.cx.wms.api.ViewContext;
import com.usersmarts.cx.wms.xml.WmsModule;
import com.usersmarts.jts.GeoGeometryFactory;
import com.usersmarts.pub.atom.Content;
import com.usersmarts.pub.atom.Entry;
import com.usersmarts.util.Coerce;
import com.usersmarts.util.DOMUtils;
import com.usersmarts.xmf2.MarshalContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * The MapService implementation.
 * 
 * @author Patrick Neal - Image Matters, LLC
 * @created: Nov 7, 2008
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class MapServiceImpl
    implements MapService, ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(MapServiceImpl.class);

    private WorkProductService workProductService;
    private DirectoryService directoryService;
    private ConfigurationService configurationService;
    private ShapefileIngester shapefileIngester;

    private GeoGeometryFactory geometryFactory = new GeoGeometryFactory();

    private static final String PRECISB_NS = "http://www.saic.com/precis/2009/06/base";

    /** {@inheritDoc} */
    public DirectoryService getDirectoryService() {

        return directoryService;
    }

    /** {@inheritDoc} */
    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    /** {@inheritDoc} */
    public WorkProductService getWorkProductService() {

        return workProductService;
    }

    /** {@inheritDoc} */
    public void setWorkProductService(WorkProductService workProductService) {

        this.workProductService = workProductService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {

        this.configurationService = configurationService;
    }

    public ConfigurationService getConfigurationService() {

        return configurationService;
    }

    /** {@inheritDoc} */
    @Override
    public void systemInitializedHandler(String messgae) {

        // log.debug("systemInitializedHandler - started");
        WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(MapType);
        // log.info("systemInitializedHandler - register with DirectoryService");
        getDirectoryService().registerUICDSService(NS_MapService, MAP_SERVICE_NAME, typeList,
            typeList);
        log.debug("systemInitializedHandler - started");
    }

    /**
     * Gets the layer.
     * 
     * @param packageId the package id
     * @return the layer
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public Node getLayer(Node packageId) throws Exception {

        Node workProductIdNode = DOMUtils.getChild(packageId, "Identifier");
        String workProductId = workProductIdNode.getTextContent();

        return getLayer(workProductId);

    }

    /**
     * Gets the layer.
     * 
     * @param incidentId the incident id
     * @param layerId the layer id
     * @return the layer
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public Node getLayer(String workProductId) throws Exception {

        WorkProduct layer = getWorkProductService().getProduct(workProductId);
        if (layer != null) {
            WorkProductDocument.WorkProduct wpd = WorkProductHelper.toWorkProduct(layer);
            if (wpd != null) {
                return wpd.getDomNode();
            }
        }
        return null;
    }

    /**
     * Gets the layer.
     * 
     * @param packageId the package id
     * @return the layer
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public WorkProduct getLayerWP(Node packageId) throws Exception {

        Node workProductIdNode = DOMUtils.getChild(packageId, "Identifier");
        String workProductId = workProductIdNode.getTextContent();

        return getLayerWP(workProductId);

    }

    /**
     * Gets the layer.
     * 
     * @param incidentId the incident id
     * @param layerId the layer id
     * @return the layer
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public WorkProduct getLayerWP(String workProductId) throws Exception {

        return getWorkProductService().getProduct(workProductId);
    }

    /**
     * Gets the map.
     * 
     * @param packageId the package id
     * @return the map
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public Node getMap(Node packageId) throws Exception {

        // log.debug("Retrieving map '" + mapId + "' for incident '" + incidentId + "'");
        Node workProductIdNode = DOMUtils.getChild(packageId, "Identifier");
        String workProductId = workProductIdNode.getTextContent();

        return getMap(workProductId);
        // WorkProduct map = getWorkProductService().getProduct(workProductId);
        // WorkProductDocument.WorkProduct wp = WorkProductHelper.toDocument(map);
        // return wp.getDomNode();
        // if (map != null) {
        // return coerce(map.getProduct());
        // }
        // return null;
    }

    /**
     * Gets the map.
     * 
     * @param workProductId the work product id
     * @return the map
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public Node getMap(String workProductId) throws Exception {

        WorkProduct map = getWorkProductService().getProduct(workProductId);
        if (map != null) {
            WorkProductDocument.WorkProduct wpd = WorkProductHelper.toWorkProduct(map);
            if (wpd != null) {
                return wpd.getDomNode();
            }
        }
        return null;
    }

    /**
     * Gets the map.
     * 
     * @param packageId the package id
     * @return the map
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public WorkProduct getMapWP(Node packageId) throws Exception {

        // log.debug("Retrieving map '" + mapId + "' for incident '" + incidentId + "'");
        Node workProductIdNode = DOMUtils.getChild(packageId, "Identifier");
        String workProductId = workProductIdNode.getTextContent();

        return getMapWP(workProductId);
    }

    /**
     * Gets the map.
     * 
     * @param workProductId the work product id
     * @return the map
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public WorkProduct getMapWP(String workProductId) throws Exception {

        return getWorkProductService().getProduct(workProductId);
    }

    /**
     * Gets the layers.
     * 
     * @param incidentId the incident id
     * @return the layers
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public Collection<WorkProduct> getLayers(String incidentId) throws Exception {

        // log.debug("Retrieving layers for incident '" + incidentId + "'");
        List<WorkProduct> products = getWorkProductService().findByInterestGroupAndType(incidentId,
            LayerType);
        return products;
    }

    /**
     * Gets the maps.
     * 
     * @param incidentId the incident id
     * @return the maps
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public Collection<WorkProduct> getMaps(String incidentId) throws Exception {

        // log.debug("Retrieving maps for incident '" + incidentId + "'");
        List<WorkProduct> products = getWorkProductService().findByInterestGroupAndType(incidentId,
            MapType);
        return products;
    }

    /**
     * Creates the layer.
     * 
     * @param incidentId the incident id
     * @param layer the layer
     * @return the product publication status
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createLayer(String incidentId, Node layer) throws Exception {

        layer = fixNamespace(layer);

        WorkProduct product = new WorkProduct();
        if (incidentId != null) {
            product.getAssociatedInterestGroupIDs().add(incidentId);
        }

        product.setProduct(coerce(layer));
        product.setProductType(LayerType);
        Date date = new Date();
        product.setCreatedDate(date);
        product.setUpdatedDate(date);

        ProductPublicationStatus status = workProductService.publishProduct(product);
        return status;
    }

    /**
     * Creates the map.
     * 
     * @param incidentId the incident id
     * @param map the map
     * @return the product publication status
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createMap(String incidentId, Node map) throws Exception {

        map = fixNamespace(map);

        WorkProduct product = new WorkProduct();
        // Get a work product id
        String productID = UUIDUtil.getID(MapService.MapType);
        ((Element) map).setAttribute("id", productID); // update id to point to work product
        product.setProductID(productID);

        if (incidentId != null) {
            product.getAssociatedInterestGroupIDs().add(incidentId);
        }
        product.setProduct(coerce(map));
        product.setProductType(MapType);
        Date date = new Date();
        product.setCreatedDate(date);
        product.setUpdatedDate(date);

        ProductPublicationStatus status = workProductService.publishProduct(product);
        // if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
        // String id = status.getProduct().getProductID();
        // if (StringUtils.isNotEmpty(id)) {
        // ((Element) map).setAttribute("id", id); // update id to point to work product
        // updateMap(incidentId, id, map);
        // }
        // }
        // log.debug("Created map '" + id + "' for incident '" + incidentId + "'");
        return status;
    }

    /**
     * Delete layer.
     * 
     * @param incidentId the incident id
     * @param layerId the layer id
     * @return the product publication status
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public ProductPublicationStatus deleteLayer(String incidentId, String layerId) throws Exception {

        WorkProduct layer = getWorkProductService().getProduct(layerId);
        ProductPublicationStatus status;

        if (layer == null) {
            status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(layerId + " doesn't existed");
            return status;
        }

        // if it's still not closed, we need to close it first
        if (layer.isActive() == true) {
            status = getWorkProductService().closeProduct(
                WorkProductHelper.getWorkProductIdentification(layer));
            if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
                return status;
        }

        return getWorkProductService().archiveProduct(
            WorkProductHelper.getWorkProductIdentification(layer));
    }

    /**
     * Delete map.
     * 
     * @param incidentId the incident id
     * @param mapId the map id
     * @return the product publication status
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public ProductPublicationStatus deleteMap(String incidentId, String mapId) throws Exception {

        WorkProduct map = getWorkProductService().getProduct(mapId);
        ProductPublicationStatus status;

        if (map == null) {
            status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(mapId + " doesn't existed");
            return status;
        }

        // if it's still not closed, we need to close it first
        if (map.isActive() == true) {
            status = getWorkProductService().closeProduct(
                WorkProductHelper.getWorkProductIdentification(map));
            if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
                return status;
        }

        return getWorkProductService().archiveProduct(
            WorkProductHelper.getWorkProductIdentification(map));
    }

    /**
     * Update layer.
     * 
     * @param pkgId the layer's work product identification
     * @param layer the layer
     * @return the product publication status
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateLayer(Node packageId, Node layer) throws Exception {

        layer = fixNamespace(layer);

        String layerId = getWorkProductId(packageId);

        WorkProduct product = getWorkProductService().getProduct(layerId);
        if (product != null) {
            // creating new version of the existing wp for tracking purposes
            WorkProduct newWP = new WorkProduct(product);
            newWP.setProduct(coerce(layer));
            ProductPublicationStatus status = workProductService.publishProduct(newWP);
            // log.debug("Updated layer '" + layerId + "' for incident '" + incidentId + "'");
            return status;
        } else {
            throw new NullPointerException("Layer with id '" + layerId + "' is null");
        }
    }

    /**
     * Update map.
     * 
     * @param packageIdNode the package id node
     * @param map the map
     * @return the product publication status
     * @throws Exception the exception
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateMap(Node packageIdNode, Node map) throws Exception {

        map = fixNamespace(map);

        String mapId = getWorkProductId(packageIdNode);

        WorkProduct product = getWorkProductService().getProduct(mapId);
        if (product != null) {
            // creating new version of the existing wp for tracking purposes
            WorkProduct newWP = new WorkProduct(product);
            newWP.setProduct(coerce(map));
            ProductPublicationStatus status = workProductService.publishProduct(newWP);
            // log.debug("Updated map '" + mapId + "' for incident '" + incidentId + "'");

            return status;
        } else {
            throw new NullPointerException("Map with id '" + mapId + "' is null");
        }
    }

    private String getWorkProductId(Node node) {

        Node incidentIdNode = DOMUtils.getChild(node, new QName(PRECISB_NS, "Identifier"));
        if (incidentIdNode != null)
            return incidentIdNode.getTextContent();
        else
            return null;
    }

    /**
     * Converts a W3C DOM node into a byte array for storage within a WorkProduct
     * 
     * @param node Node
     * @return byte[]
     * @throws Exception
     */
    protected XmlObject coerce(Node node) throws Exception {

        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer, true);
        final DOMSource input = new DOMSource(node);
        Result output = new StreamResult(printWriter);
        try {
            TransformerFactory xsltTransformerFactory = TransformerFactory.newInstance();
            Transformer transformer = xsltTransformerFactory.newTransformer(); // id
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
            transformer.transform(input, output);
        } catch (Exception e) {
            log.error("coerce: " + e.getMessage());
            return null;
        }
        printWriter.flush();
        return XmlObject.Factory.parse(writer.toString());
    }

    /**
     * Converts a byte array representation of XML into a w3c DOM node
     * 
     * @param bytes byte[]
     * @return Node
     * @throws Exception
     */
    protected Node coerce(byte[] bytes) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new ByteArrayInputStream(new String(bytes).getBytes("UTF-8")));
        return document.getDocumentElement();
    }

    /**
     * Handles notifications when Work Products are changed. We're only interested in those of type
     * "Feature"
     * 
     * @param message ProductChangeNotificationMessage
     * @ssdd
     */
    public void productChangeNotificationHandler(ProductChangeNotificationMessage message) {

        log.debug("Receive productChangeNotification for wp ID: " + message.getProductID());
    }

    /**
     * Handles notifications when new Incidents have been created in the system.
     * 
     * @param notification IncidentStateNotificationMessage
     * @ssdd
     */
    public void handleIncidentState(IncidentStateNotificationMessage message) {

        if (message.getState().equals(InterestGroupStateNotificationMessage.State.NEW)
            || message.getState().equals(InterestGroupStateNotificationMessage.State.JOIN)) {
            String incidentId = message.getIncidentInfo().getId();

            // locate the work product for the new incident
            String wpId = message.getIncidentInfo().getWorkProductIdentification().getIdentifier().getStringValue();
            WorkProduct product = getWorkProductService().getProduct(wpId);
            if (product == null) {
                log.warn("Failed to retrieve work product for newly created incident");
                return;
            }

            // parse the incident from the work product
            IncidentDocument incidentDoc = null;
            try {
                incidentDoc = (IncidentDocument) product.getProduct();
            } catch (Exception e) {
                log.error("Error parsing Incident work product", e);
                return;
            }

            if (incidentDoc == null)
                return;
            UICDSIncidentType incident = incidentDoc.getIncident();
            Geometry geometry = null;
            try {
                geometry = parseGeometry(incident);
            } catch (Throwable e) {
                log.info("No Geometry information specified in Incident document");
            }
            if (message.getState().equals(InterestGroupStateNotificationMessage.State.NEW))
                submitDefaultMap(incidentId, incident, geometry);
        } else if (message.getState().equals(InterestGroupStateNotificationMessage.State.RESIGN)) {
            // remove incident
            // String url = ef.getUrl(getConfigurationService());
            // Workspace ws = new Workspace();
            // ws.setGuid(notification.getIncidentID());
            // try {
            // new WorkspaceClient(url).delete(ws);
            // } catch (Exception e) {
            // log.error("Failed to remove incident from COP Summary Service", e);
            // }
        }
    }

    /**
     * Creates a default Map for the specified Incident and stores it as a WorkProduct
     * 
     * @param incidentId String incident identifier (not work product identifier)
     * @param incident UICDSIncidentType
     * @param geometry Geometry spatial component of the incident
     * @return status of the submit request
     * @ssdd
     */
    protected ProductPublicationStatus submitDefaultMap(String incidentId,
        UICDSIncidentType incident, Geometry geometry) {

        ProductPublicationStatus status = null;
        try {
            if (geometry != null) {
                Node map = createDefaultMap(incidentId, geometry); // build the view context
                status = createMap(incidentId, map); // persist it as a work product

                // if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
                // ((Element) map).setAttribute("id", status.getProduct().getProductID());
                // // update the xml to include with the new product ID
                // updateMap(incidentId, status.getProduct().getProductID(), map);
                // log.debug("Created default map for new incident " + incidentId);
                // }

            } else {
                log.warn("No area defined for incident " + incidentId
                    + ", not creating default map");
            }
        } catch (Throwable t) {
            log.error("Error creating default map for new incident with id " + incidentId, t);
        }
        return status;
    }

    /**
     * @param incident UICDSIncidentType
     * @return Geometry or null
     */
    protected Geometry parseGeometry(UICDSIncidentType incident) {

        // System.out.println("parseGeometry: incident=[" + incident.toString() + "]");
        LocationType location = incident.getIncidentLocationArray(0);
        AreaType[] areas = location.getLocationAreaArray();
        if (areas.length > 0) {
            AreaType area = location.getLocationAreaArray(0);
            try {
                return parseGeometryUsingDOM(area);
            } catch (Throwable t) {
                log.error("Error parsing spatial extent from Incident work product", t);
            }
        } else {
            log.warn("Unable to find LocationArea within Incident, cannot create default map");
        }
        return null;
    }

    /**
     * Parses the geometry of the incident by traversing it's xml representation
     * 
     * @param area AreaType
     * @return Geometry or null
     * @throws Exception
     */
    protected Geometry parseGeometryUsingDOM(AreaType area) {

        /*
         * because XMLBeans does not support DOM level 3, we must do this the hard way the "Node"
         * returned by 'area.getDomNode()' above is in reality an XMLBeans impl of a Node. We must
         * write it out as a string and then parse it back into a true w3c DOM Node. We have to go
         * up the chain, however, to make sure any and all XML schema declarations are accounted for
         * or else we could fail parsing the serialized DOM <incident> <IncidentLocation>
         * <LocationArea> </LocationArea> </IncidentLocation> </incident>
         */

        Node root = area.getDomNode().getParentNode().getParentNode(); // incident node
        if (root == null)
            return null;
        String domStr = DOMUtils.toString(root);
        StringReader reader = new StringReader(domStr);
        try {
            root = DOMUtils.parseDocument(reader);
        } catch (Exception e) {
            log.error("Error parsing xml from incident while determining spatial extent", e);
            return null;
        }
        if (root == null)
            return null;
        root = ((Document) root).getDocumentElement();
        Node locNode = DOMUtils.getChild(root, area.getDomNode().getParentNode().getLocalName());
        if (locNode == null)
            return null;
        Node areaNode = DOMUtils.getChild(locNode, area.getDomNode().getLocalName());
        if (areaNode == null)
            return null;

        List<Element> polyCoords = DOMUtils.getChildren(areaNode, "AreaPolygonGeographicCoordinate");
        if (!polyCoords.isEmpty()) {

            int i = 0;
            Coordinate[] carr = new Coordinate[polyCoords.size()];
            for (Element polyCoord : polyCoords) {
                carr[i++] = parseCoordinateUsingDOM(polyCoord);
            }

            // make sure last point is same as first point for valid polygon
            if (!carr[carr.length - 1].equals(carr[0])) {
                Coordinate[] temp = new Coordinate[carr.length + 1];
                for (i = 0; i < carr.length; ++i)
                    temp[i] = carr[i];
                temp[i] = carr[0];
                carr = temp;
            }

            // create polygon so we can get the bounding box for the area
            LinearRing shell = geometryFactory.createLinearRing(carr);
            Polygon poly = geometryFactory.createPolygon(shell, null);
            return poly;

        } else {
            // look for circle

            Element circle = DOMUtils.getChild(areaNode, "AreaCircularRegion");
            if (circle != null) {
                Element center = DOMUtils.getChild(circle, "CircularRegionCenterCoordinate");
                Element rad = DOMUtils.getChild(circle, "CircularRegionRadiusLengthMeasure");
                String r = DOMUtils.getChildTextNS(rad, "http://niem.gov/niem/niem-core/2.0",
                    "MeasurePointValue", "1.0");
                Double radius = Coerce.toDouble(r, 1.0);
                Coordinate coord = parseCoordinateUsingDOM(center);
                if (coord != null) {
                    Point point = geometryFactory.createPoint(coord);
                    if (radius > 0.0) {
                        Geometry geometry = geometryFactory.createCircle(point, radius);
                        return geometry;
                    } else if (radius == 0.0) {
                        return point;
                    } else {
                        log.warn("Invalid radius specified for incident circle.  Must be non-negative");
                    }
                }
            }
        }

        return null;
    }

    /**
     * Parses a coordinate from an incident's XML representation
     * 
     * @param parent Element
     * @return Coordinate
     */
    private Coordinate parseCoordinateUsingDOM(Element parent) {

        Element coord = DOMUtils.getChild(parent, "LocationTwoDimensionalGeographicCoordinate");
        if (coord == null)
            coord = parent;

        Element lon = DOMUtils.getChild(coord, "GeographicCoordinateLongitude");
        Double lonDegree = Coerce.toDouble(DOMUtils.getChildText(lon, "LongitudeDegreeValue"), null);
        Double lonMinute = Coerce.toDouble(DOMUtils.getChildText(lon, "LongitudeMinuteValue"), 0.0);
        Double lonSecond = Coerce.toDouble(DOMUtils.getChildText(lon, "LongitudeSecondValue"), 0.0);

        Element lat = DOMUtils.getChild(coord, "GeographicCoordinateLatitude");
        Double latDegree = Coerce.toDouble(DOMUtils.getChildText(lat, "LatitudeDegreeValue"), null);
        Double latMinute = Coerce.toDouble(DOMUtils.getChildText(lat, "LatitudeMinuteValue"), 0.0);
        Double latSecond = Coerce.toDouble(DOMUtils.getChildText(lat, "LatitudeSecondValue"), 0.0);

        // didn't specify coordinates properly
        if (lonDegree == null || latDegree == null) {
            log.warn("Coordinates were not specified properly in Incident work product, "
                + "unable to determine location for default map");
            return null;
        }

        // convert to decimal degrees
        int sign = (int) (lonDegree / Math.abs(lonDegree));
        lonDegree = sign * (Math.abs(lonDegree) + (lonMinute / 60.0) + (lonSecond / 3600.0));

        sign = (int) (latDegree / Math.abs(latDegree));
        latDegree = sign * (Math.abs(latDegree) + (latMinute / 60.0) + (latSecond / 3600.0));

        return new Coordinate(lonDegree, latDegree);
    }

    /**
     * Creates a new Map for the Incident with the specified incident identifier
     * 
     * @param incidentId String
     * @param geometry Envelope
     * @return Node w3c DOM Node representing the XML-encoded map
     * @throws Exception
     */
    protected Node createDefaultMap(String incidentId, Geometry geometry) throws Exception {

        ViewContext context = new ViewContext();
        context.setVersion("1.1.0");
        context.setTitle("Default Map for Incident '" + incidentId + "'");
        context.setAbstract("Default map for incident with id '" + incidentId + "'");
        context.getKeywords().add("Default Map");
        context.getKeywords().add(incidentId);
        context.getKeywords().add("UICDS");
        context.setWindow(new Rectangle(600, 500));

        Envelope extent = (geometry == null)
            ? new Envelope(-125.0, -66.0, 20.0, 50.0)
            : geometry.getEnvelopeInternal();
        context.setBoundingBox(extent);

        // determine url to access features via wms controller
        String url = configurationService.getRestBaseURL() + incidentId + "/features?";

        // base map layer
        Server server = new Server(new URI("http://labs.metacarta.com/wms/vmap0"), "OGC:WMS",
            "1.1.0", "Metacarta");
        context.getLayers().add(
            new Layer(server, "basic", "Base Map", "", null, null, "EPSG:4326", false, false, null));

        // features layer
        server = new Server(new URI(url), "OGC:WMS", "1.1.0", "UICDS Core Map Service");
        context.getLayers().add(
            new Layer(server, "", "Incident Features", "", null, null, "EPSG:4326", false, false,
                null));

        // marshal map out to bytes
        MarshalContext ctx = new MarshalContext(WmsModule.class);
        ctx.setProperty("strictMode", true); // only schema-compliant output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ctx.marshal(context, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toString().getBytes("UTF-8"));
        return DOMUtils.parseDocument(in).getDocumentElement();
    }

    /**
     * @param incidentId String identifier
     * @param bytes byte array containing the shapefile
     * @return collection of ProductPublicationStatus for the new work products
     * @see com.saic.uicds.core.em.service.MapService#submitShapefile(java.lang.String, byte[])
     * @ssdd
     */
    @SuppressWarnings("unchecked")
    public List<ProductPublicationStatus> submitShapefile(String incidentId, byte[] bytes)
        throws Exception {

        if (getShapefileIngester() == null) {
            log.warn("No Shapefile Ingester configured with MapService. Ignoring request");
            return Collections.EMPTY_LIST;
        }

        List<ProductPublicationStatus> results = new ArrayList<ProductPublicationStatus>();
        List<String> ids = new ArrayList<String>();

        // create a new WorkProduct for the shapefile
        // needs to be XML instead of just the binary content of the shapefile
        // so we'll use an ATOM entry with the binary shapefile as "content"
        Entry entry = new Entry();
        entry.setTitle("shapefile");
        if (incidentId == null) {
            entry.setSummary("a shapefile");
        } else {
            entry.setSummary("a shapefile for Incident: " + incidentId);
        }
        entry.setContent(new Content("application/shz", bytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new MarshalContext(AtomModule.class).marshal(entry, out);
        byte[] content = out.toByteArray();
        out.close();
        WorkProduct product = new WorkProduct();
        if (incidentId != null) {
            product.getAssociatedInterestGroupIDs().add(incidentId);
        }
        // TODO do we have better way to do this ?
        product.setProduct(XmlObject.Factory.parse(new String(content)));

        product.setProductType("Shapefile");
        ProductPublicationStatus status = getWorkProductService().publishProduct(product);
        if (status.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
            ids.add(status.getProduct().getProductID());
            results.add(status);
        }

        // create new WorkProducts for each feature parsed from the shapefile
        // TODO - Do we need to make sure the bytes is enforced to be UTF-8 ???
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        List<WorkProduct> features = getShapefileIngester().parseFeatures(incidentId, in);
        
        DigestDocument productDigest = DigestDocument.Factory.newInstance();
		productDigest.addNewDigest();
		List<String> locationIds=new ArrayList<String>();
        for (WorkProduct feature : features) {
        	if(feature.getDigest()!=null){
        		ThingType[] locationArray = feature.getDigest().getDigest().getThingAbstractArray();
        		for(ThingType location:locationArray){
        			locationIds.add(location.getId());
        			gov.ucore.ucore.x20.LocationType l=(gov.ucore.ucore.x20.LocationType)location;
        			XmlUtil.substitute(productDigest.getDigest().addNewThingAbstract(), InfrastructureNamespaces.NS_UCORE,
        					"Location", gov.ucore.ucore.x20.LocationType.type, l);
        		}
        	}
            WorkProductService wps = getWorkProductService();
            ProductPublicationStatus featureStat = wps.publishProduct(feature);
            if (featureStat.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
                ids.add(status.getProduct().getProductID());
                results.add(featureStat);
            }
            feature.setDigest(null);
        }
        addCollectionTypeToDigest(productDigest,locationIds);
        product.setDigest(productDigest);

        // create a new Layer WorkProduct for all the features
        // parsed from the shapefile
        {
            String layerName = StringUtils.join(ids.toArray(new String[ids.size()]), ",");
            layerName = layerName.substring(layerName.indexOf(",") + 1, layerName.length());

            Document doc = DOMUtils.newDocument();
            Element root = doc.createElementNS(WMC.NAMESPACE, "Layer");
            root.setAttribute("queryable", "0");
            root.setAttribute("hidden", "0");
            doc.appendChild(root);

            Element server = doc.createElementNS(WMC.NAMESPACE, "Server");
            server.setAttribute("service", "OGC:WMS");
            server.setAttribute("version", "1.1.0");
            server.setAttribute("title", "UICDS Feature WMS");
            root.appendChild(server);

            String url = configurationService.getRestBaseURL() + incidentId + "/features?";

            Element or = doc.createElementNS(WMC.NAMESPACE, "OnlineResource");
            or.setAttributeNS(WMC.XLINK_NAMESPACE, "href", url);
            server.appendChild(or);

            DOMUtils.appendElementWithText(root, WMC.NAME, layerName);
            if (incidentId != null) {
                DOMUtils.appendElementWithText(root, WMC.TITLE, "Shapefile Features in Incident '"
                    + incidentId + "'");
                DOMUtils.appendElementWithText(root, WMC.ABSTRACT,
                    "Shapefile Features for Incident '" + incidentId + "'");
            } else {
                DOMUtils.appendElementWithText(root, WMC.TITLE, "Shapefile Features");
                DOMUtils.appendElementWithText(root, WMC.ABSTRACT,
                    "Shapefile Features for Incident");
            }
            DOMUtils.appendElementWithText(root, WMC.SRS, "EPSG:4326");
            ProductPublicationStatus layerStat = createLayer(incidentId, root);
            if (layerStat.getStatus().equals(ProductPublicationStatus.SuccessStatus)) {
                results.add(layerStat);
            }
        }

        return results;
    }

    @SuppressWarnings("deprecation")
	private void addCollectionTypeToDigest(DigestDocument digest,
			List<String> locationIds) {
		SchemaType type = CollectionType.type;
		CollectionType colType = CollectionType.Factory.newInstance();
		colType.setId(UUIDUtil.getID("Collection"));
		
		IdentifierType identifier = colType.addNewIdentifier();
		identifier.addNewLabel().set("Label");
		identifier.setCode("label");
		identifier.setCodespace("http://uicds.us/identifier");
		identifier.set("Collection (geo)");

		StringType description = colType.addNewDescriptor();
		description.set("Collection of LocationInfo of all features");

		WhatType what = colType.addNewWhat();
		what.setCode("GroupOfLocations");
		what.setCodespace(InfrastructureNamespaces.NS_UCORE_CODESPACE);

		for (String locationId : locationIds) {
			ThingRefType ref = colType.addNewThingRef();
			List<String> arg0 = new ArrayList<String>();
			arg0.add(locationId);
			ref.setRef(arg0);
		}

		XmlUtil.substitute(digest.getDigest().addNewThingAbstract(),
				InfrastructureNamespaces.NS_UCORE, DigestConstant.S_Collection,
				type, colType);
	}
    
    /**
     * Sets the shapefile ingester.
     * 
     * @param shapefileIngester the new shapefile ingester
     * @ssdd
     */
    public void setShapefileIngester(ShapefileIngester shapefileIngester) {

        this.shapefileIngester = shapefileIngester;
    }

    /**
     * Gets the shapefile ingester.
     * 
     * @return the shapefile ingester
     * @ssdd
     */
    public ShapefileIngester getShapefileIngester() {

        return shapefileIngester;
    }

    private Node fixNamespace(Node el) {

        if (WMC.NAMESPACE.equals(el.getNamespaceURI()))
            return el;
        try {
            Document document = el.getOwnerDocument();
            // create new version of the node
            Element newEl = document.createElementNS(WMC.NAMESPACE, el.getLocalName());
            newEl.setPrefix("wmc");
            // insert the new node before the old one
            el.getParentNode().insertBefore(newEl, el);
            // walk the children and re-parent them
            Node child = el.getFirstChild();
            while (child != null) {
                Node nextChild = child.getNextSibling();
                el.removeChild(child);
                newEl.appendChild(child);
                child = nextChild;
            }
            el.getParentNode().removeChild(el);
            return newEl;
        } catch (Throwable t) {
            log.error("Error fixing namespace", t);
            return el;
        }
    }
}
