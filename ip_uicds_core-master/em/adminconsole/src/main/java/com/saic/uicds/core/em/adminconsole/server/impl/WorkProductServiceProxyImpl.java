package com.saic.uicds.core.em.adminconsole.server.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.uicds.incident.IncidentDocument;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.base.StateType;
import com.saic.precis.x2009.x06.payloads.link.LinkContentDocument;
import com.saic.uicds.core.em.adminconsole.client.model.WorkProductGWT;
import com.saic.uicds.core.em.adminconsole.client.rpc.WorkProductServiceProxy;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;
import com.saic.uicds.core.infrastructure.util.WorkProductXsltType;
import com.usersmarts.util.DirectoryWatcher;
import com.usersmarts.util.DirectoryWatcher.Change;

public class WorkProductServiceProxyImpl extends RemoteServiceServlet implements
        WorkProductServiceProxy, DirectoryWatcher.Listener {

    /**
     * Serializable
     */
    private static final long serialVersionUID = -7537045601575114028L;

    private WorkProductService service = null;
    private WorkProductXsltType wpXsltType = null;

    private Map<String, Map<String, String>> xsltMap = null;
    private Map<String,String> defaultXsltMap=null;

    Logger log = LoggerFactory.getLogger(this.getClass());

    private DirectoryWatcher directoryWatcher;
    private Timer watcherTimer = null;
    private long frequency = 20000;
    private long delay = 5000;
    private String xsltDirectory;
    private String xsltConfiguredDirectory;

    public String getXsltConfiguredDirectory() {
        return xsltConfiguredDirectory;
    }

    public void setXsltConfiguredDirectory(String configuredDirectory) {
        this.xsltConfiguredDirectory = configuredDirectory;
    }

    public String getXsltDirectory() {
        return xsltDirectory;
    }

    public void setXsltDirectory(String xsltDirectory) {
        this.xsltDirectory = xsltDirectory;
    }

    @Override
    public String archiveProduct(String productID) {

        String request = "Archive: [" + productID + "] ";
        WorkProduct wp = getWorkProductService().getProduct(productID);
        ProductPublicationStatus status = getWorkProductService().archiveProduct(
                WorkProductHelper.getWorkProductIdentification(wp));

        return request + status.getStatus();
    }

    @Override
    public String closeProduct(String productID) {

        String request = "Close: [" + productID + "] ";
        WorkProduct wp = getWorkProductService().getProduct(productID);
        ProductPublicationStatus status = getWorkProductService().closeProduct(
                WorkProductHelper.getWorkProductIdentification(wp));

        return request + status.getStatus();
    }

    public List<WorkProductGWT> findByIncidentAndType(String incidentID, String productType) {

        List<WorkProduct> list = getWorkProductService().findByInterestGroupAndType(incidentID,
                productType);
        return getList(list);
    }

    private String getIncidentID(WorkProduct wp) {

        String incidentID = null;
        Set<String> incidentIDs = wp.getAssociatedInterestGroupIDs();
        if (incidentIDs.size() > 0) {
            for (String id : incidentIDs) {
                incidentID = id;
            }
        }
        return incidentID;
    }

    private List<WorkProductGWT> getList(List<WorkProduct> wpList) {

        List<WorkProductGWT> listGWT = new ArrayList<WorkProductGWT>(wpList.size());
        for (WorkProduct wp : wpList) {

            WorkProductGWT temp = new WorkProductGWT();
            try {
                BeanUtils.copyProperties(temp, wp);
                temp.setIncidentID(getIncidentID(wp));
                if (wp.getDigest() != null) {
                    temp.setDigest(true);
                } else {
                    temp.setDigest(false);
                }
                if (wp.getActive() == false) {
                    temp.setClosed(true);
                }
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

    /**
     * get digest or full product with default xslt
     */
    public WorkProductGWT getProduct(String productID, boolean showDefaultWP) {

        WorkProduct wp = getWorkProductService().getProduct(productID);
        WorkProductGWT dataGWT = new WorkProductGWT();
        try {
            BeanUtils.copyProperties(dataGWT, wp);
            // dataGWT.setProduct(Util.getPrettyXmlFromBytes(wp.getProduct()));
            dataGWT.setProduct(WorkProductHelper.toWorkProductDocument(wp).xmlText(
                    new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(Util.IdentSize)));

            String xslt = null;
            if(showDefaultWP){
                xslt=defaultXsltMap.get("default");
            }else{
                String type = getXsltType(wp.getProductType());
                Map<String, String> xsltIdMap = getXsltMap(type);
                if (xsltIdMap == null) {
                    XmlObject xmlObject = wp.getProduct();
                    if (xmlObject == null)
                        return dataGWT;
                    if (xmlObject instanceof LinkContentDocument) {
                        type = getXsltType(LinkContentDocument.class.getName().replace("link", "links"));
                        xsltIdMap = getXsltMap(type);
                    } else {
                        type = getXsltType(IncidentDocument.class.getName());
                        xsltIdMap = getXsltMap(type);
                    }
                }
                if (xsltIdMap == null) {
                    xslt=defaultXsltMap.get("default");
                }else{
                    xslt = xsltIdMap.get("default:" + type);
                }
            }
            
            if (wp.getDigest() != null) {
                dataGWT.setDigest(true);
            } else {
                dataGWT.setDigest(false);
            }
            String wpXml = WorkProductHelper.toWorkProductDocument(wp).xmlText(
                    new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(Util.IdentSize));

            String productHtml = "";
            productHtml = transformation(wpXml, xslt);
            dataGWT.setProductHtml(productHtml);
        } catch (Exception e) {
            log.error("Error retrieving Product: " + e.toString());
        }
        return dataGWT;
    }

    /**
     * get digest or full product with specified xslt
     */
    public WorkProductGWT getProduct(String productID, String xsltId, boolean showDefaultWP) {

        WorkProduct wp = getWorkProductService().getProduct(productID);
        WorkProductGWT dataGWT = new WorkProductGWT();
        try {
            BeanUtils.copyProperties(dataGWT, wp);
            dataGWT.setProduct(WorkProductHelper.toWorkProductDocument(wp).xmlText(
                    new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(Util.IdentSize)));

            String xslt = null;
            if(showDefaultWP){
                xslt=defaultXsltMap.get("default");
            }else{
                Map<String, String> xsltIdMap = getXsltMap(wp.getProductType());
                if (xsltIdMap == null) {
                    XmlObject xmlObject = wp.getProduct();
                    if (xmlObject == null)
                        return dataGWT;
                    if (xmlObject instanceof LinkContentDocument) {
                        xsltIdMap = getXsltMap(LinkContentDocument.class.getName().replace("link", "links"));
                    } else {
                        xsltIdMap = getXsltMap(IncidentDocument.class.getName());
                    }
                }
                if (xsltIdMap == null) {
                    xslt=defaultXsltMap.get("default");
                }else{
                    xslt = xsltIdMap.get(xsltId);
                }
            }
            if (wp.getDigest() != null) {
                dataGWT.setDigest(true);
            } else {
                dataGWT.setDigest(false);
            }
            String wpXml = WorkProductHelper.toWorkProductDocument(wp).xmlText(
                    new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(Util.IdentSize));

            String productHtml = "";
            productHtml = transformation(wpXml, xslt);
            dataGWT.setProductHtml(productHtml);
        } catch (Exception e) {
            log.error("Error retrieving Product: " + e.toString());
        }
        return dataGWT;
    }

    @Deprecated
    public final IdentificationType getWorkProductIdentification(WorkProduct wp) {

        IdentificationType pkgId = IdentificationType.Factory.newInstance();

        if (wp.getChecksum() != null) {
            pkgId.addNewChecksum().setStringValue(wp.getChecksum());
        }
        if (wp.getProductID() != null) {
            pkgId.addNewIdentifier().setStringValue(wp.getProductID());
        }
        if (wp.getProductType() != null) {
            pkgId.addNewType().setStringValue(wp.getProductType());
        }
        if (wp.getProductVersion() != null) {
            pkgId.addNewVersion().setStringValue(wp.getProductVersion().toString());
        }

        pkgId.setState(wp.getActive() ? StateType.ACTIVE : StateType.INACTIVE);

        return pkgId;
    }

    public List<WorkProductGWT> getWorkProductListChildren(WorkProductGWT workProduct) {

        if (!workProduct.isRoot()) {
            return listAllWorkProducts();
        }
        List<WorkProductGWT> listGWT = new ArrayList<WorkProductGWT>(1);
        workProduct.setRoot(false);
        listGWT.add(workProduct);
        return listGWT;
    }

    @Deprecated
    private final PropertiesType getWorkProductProperties(WorkProduct wp) {

        PropertiesType properties = PropertiesType.Factory.newInstance();

        Set<String> igIdSet = wp.getAssociatedInterestGroupIDs();
        if (igIdSet.size() > 0) {
            String[] igIds = wp.getAssociatedInterestGroupIDs().toArray(new String[igIdSet.size()]);
            properties.addNewAssociatedGroups();
            for (String igId : igIds) {
                properties.getAssociatedGroups().addNewIdentifier().setStringValue(igId);
            }
        }
        if (wp.getCreatedDate() != null) {
            properties.addNewCreated().setDateValue(wp.getCreatedDate());
        }
        if (wp.getCreatedBy() != null) {
            properties.addNewCreatedBy().setStringValue(wp.getCreatedBy());
        }
        if (wp.getUpdatedDate() != null) {
            properties.addNewLastUpdated().setDateValue(wp.getUpdatedDate());
        }
        if (wp.getUpdatedBy() != null) {
            properties.addNewLastUpdatedBy().setStringValue(wp.getUpdatedBy());
        }
        if (wp.getMimeType() != null) {
            properties.addNewMimeType().setStringValue(wp.getMimeType());
        }
        if (wp.getSize() != null) {
            properties.addNewKilobytes().setBigIntegerValue(BigInteger.valueOf(wp.getSize()));
        }

        return properties;
    }

    /**
     * get list of xslts for the product type
     */
    public List<String> getProductXsltIds(String wpID) {
        List<String> xsltIds = new ArrayList<String>();
        try{
            WorkProduct wp = getWorkProductService().getProduct(wpID);
            String type = wp.getProductType();
            Map<String, String> xsltIdMap = getXsltMap(type);
            if (xsltIdMap == null) {
                XmlObject xmlObject = wp.getProduct();
                if (xmlObject == null)
                    return xsltIds;
                if (xmlObject instanceof LinkContentDocument) {
                    type = LinkContentDocument.class.getName().replace("link", "links");
                } else {
                    type = IncidentDocument.class.getName();
                }
            }
            xsltIds = getXsltIds(type);
            if(xsltIds.isEmpty()){
               xsltIds.addAll(defaultXsltMap.keySet()); 
            }
        }catch (Exception e) {
            log.error("Error retrieving xslt, using default one"+e.toString());
            xsltIds.clear();
            for(String key:defaultXsltMap.keySet()){
                xsltIds.add(key);
            }
        }
            
        return xsltIds;
    }

    // get xslts from map based on product type
    private List<String> getXsltIds(String type) {
        List<String> xsltIds = new ArrayList<String>();
        Map<String, String> map = getXsltMap(type);
        String defaultXslt = getDefaultXslt(map);
        if (defaultXslt != null)
            xsltIds.add(defaultXslt);
        for (String xsltId : map.keySet()) {
            if (!xsltId.contains("default")) {
                xsltIds.add(xsltId);
            }
        }
        return xsltIds;
    }

    private String getDefaultXslt(Map<String, String> map) {
        for (String key : map.keySet()) {
            if (key.contains("default")) {
                return key;
            }
        }
        return null;
    }

    private WorkProductService getWorkProductService() {

        if (this.service == null) {
            boolean loaded = loadService();
        }
        return this.service;
    }

    // Has XsltMaps with product type configuration
    private WorkProductXsltType getWorkProductXsltType() {

        if (this.wpXsltType == null) {
            loadXsltType();
        }
        return this.wpXsltType;
    }

    private String getXslString(URL url) {
        String xslString = null;
        if (url == null)
            return xslString;
        try {
            InputStream is = url.openStream();
            int size = is.available();
            byte[] content = new byte[size];
            for (int i = 0; i < size;) {
                i += is.read(content, i, size - i);
            }
            xslString = new String(content);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return xslString;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);
        String xsltInit = config.getInitParameter("XSLT_DIR");
        String xsltDir = config.getServletContext().getRealPath(xsltInit);
        setXsltDirectory(xsltDir);

        WorkProductXsltType wpXsltType = getWorkProductXsltType();
        xsltMap = wpXsltType.getXsltMap();

        File xsltDirectory = new File(xsltDir);
        setXsltConfiguredDirectory(xsltDirectory.getAbsolutePath());
        defaultXsltMap = new HashMap<String, String>();
        if (xsltDirectory.exists()) {
            File[] xsltFiles = xsltDirectory.listFiles();
            for (File xsltFile : xsltFiles) {
                if (xsltFile.getName().endsWith(".xsl")) {
                    Map<String, String> typeXsltMap = getXsltMap(xsltFile.getAbsolutePath());
                    if (typeXsltMap != null) {
                        storeXsltValueInTypeMap(typeXsltMap, xsltFile);
                    }
                    if(xsltFile.getName().contains("default.xsl")){
                        String xsltValue=null;
                        try {
                            xsltValue = getXslString(xsltFile.toURL());
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        if(xsltValue!=null)
                            defaultXsltMap.put("default", xsltValue);
                    }
                }
            }
        }
        directoryWatcher = new DirectoryWatcher(xsltDirectory, this, "^.*\\.xsl$", true);
        watcherTimer = new Timer();
        watcherTimer.schedule(directoryWatcher, delay, frequency);
    }

    private Map<String, String> getXsltMap(String key) {
        if(key==null)
            return null;
        Set<String> types = xsltMap.keySet();
        for (String type : types) {
            if (key.toLowerCase().contains(type)) {
                return xsltMap.get(type);
            }
        }
        return null;
    }

    private String getXsltType(String key) {
        Set<String> types = xsltMap.keySet();
        for (String type : types) {
            if (key.toLowerCase().contains(type)) {
                return type;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void storeXsltValueInTypeMap(Map<String, String> map, File xsltFile) {
        try {
            String file = xsltFile.getName();
            String key = file.replace(".xsl", "");
            key = getKeyFromXsltMap(key);
            String xsltValue = getXslString(xsltFile.toURL());
            if (xsltValue != null)
                map.put(key, xsltValue);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public List<WorkProductGWT> listAllWorkProducts() {

        List<WorkProduct> list = getWorkProductService().listAllWorkProducts();
        return getList(list);
    }

    public List<WorkProductGWT> listByProductType(String type) {

        List<WorkProduct> list = getWorkProductService().listByProductType(type);
        return getList(list);
    }

    private boolean loadService() {

        WebApplicationContext springContext = WebApplicationContextUtils
                .getWebApplicationContext(this.getServletContext());
        this.service = (WorkProductService) springContext.getBean("workProductService");
        if (service == null) {
            throw new RuntimeException("Unable to load WorkProductService!");
        } else {
            return true;
        }
    }

    private boolean loadXsltType() {
        WebApplicationContext springContext = WebApplicationContextUtils
                .getWebApplicationContext(this.getServletContext());
        this.wpXsltType = (WorkProductXsltType) springContext.getBean("workProductXsltType");
        if (wpXsltType == null) {
            throw new RuntimeException("Unable to load WorkProductXsltType!");
        } else {
            return true;
        }
    }

    private String transformation(String product, String xslt) {

        if (xslt == null)
            return new String();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Source xmlSource = new javax.xml.transform.stream.StreamSource(
                new StringReader(product));
        javax.xml.transform.Source xsltSource = new javax.xml.transform.stream.StreamSource(
                new StringReader(xslt));
        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);

        // create an instance of TransformerFactory
        javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory
                .newInstance();

        try {
            javax.xml.transform.Transformer transformer = transformerFactory
                    .newTransformer(xsltSource);
            transformer.transform(xmlSource, result);
            return new String(baos.toByteArray());
        } catch (Throwable e) {
            // log the error message
            // e.printStackTrace();
            return "<html><b>Transformation failed: " + e.getMessage() + ".</b></html>";
        }
    }


    @Override
    public void onChange(File file, Change change) {
        if (!file.getPath().endsWith(".xsl"))
            return;
        String fileName = file.getName();
        Map<String, String> xsltMap = getXsltMap(file.getAbsolutePath());
        if(fileName.contains("default.xsl"))
            xsltMap=defaultXsltMap;
        try {
            if (Change.DELETED.equals(change)) {
                if (xsltMap != null) {
                    String key = fileName.replace(".xsl", "");
                    key = getKeyFromXsltMap(key);
                    if(xsltMap.containsKey(key))
                        xsltMap.remove(key);
                }
            } else if (Change.ADDED.equals(change)
                    || Change.MODIFIED.equals(change)) {
                if (xsltMap != null) {
                    String key = fileName.replace(".xsl", "");
                    key = getKeyFromXsltMap(key);
                    if (file != null) {
                        String value;
                        value = getXslString(file.toURL());
                        if (value != null)
                            xsltMap.put(key, value);
                    }
                }
            }
        } catch (MalformedURLException e) {
            log.error("Error in Directory Watcher:" + e.toString());
        }
    }

    private String getKeyFromXsltMap(String key) {
        for (String type : xsltMap.keySet()) {
            if (key.equals(type)) {
                key = "default:" + type;
                break;
            }
        }
        return key;
    }

}

/*
 *     private String digestTransformation(String product, String xslt) {
        if (xslt == null)
            return new String();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Source xmlSource = new javax.xml.transform.stream.StreamSource(
                new StringReader(product));
        javax.xml.transform.Source xsltSource = new javax.xml.transform.stream.StreamSource(
                new StringReader(xslt));
        javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);

        javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory
                .newInstance();
        try {
            // replace all Applytemplates in forloop other than digest
            org.apache.xalan.transformer.TransformerImpl transformer = (TransformerImpl) transformerFactory
                    .newTransformer(xsltSource);
            StylesheetRoot styleSheet = transformer.getStylesheet();
            ElemTemplate rootTemplate = styleSheet.getTemplate(0);
            ElemTemplateElement forEachTemplate = rootTemplate.getFirstChildElem();
            ElemForEach forEachXsl = (ElemForEach) forEachTemplate.getLastChildElem();
            int len = forEachXsl.getLength();
            List<ElemApplyTemplates> applyTemplates = new ArrayList<ElemApplyTemplates>();
            for (int i = 0; i < len; i++) {
                ElemApplyTemplates applyTemp = (ElemApplyTemplates) forEachXsl.item(i);
                ChildTestIterator select = (ChildTestIterator) applyTemp.getSelect();
                String name = select.getLocalName();
                if (!name.equals("Digest")) {
                    applyTemplates.add(applyTemp);
                }
            }
            for (ElemApplyTemplates appltTemplate : applyTemplates) {
                forEachXsl.removeChild(appltTemplate);
            }
            transformer.transform(xmlSource, result);
            return new String(baos.toByteArray());

        } catch (Throwable e) {
            return "<html><b>Transformation failed: " + e.getMessage() + ".</b></html>";
        }
    }

 * 
 * 
 */
		
