package com.saic.uicds.core.infrastructure.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XQueryService;

import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.uicds.core.infrastructure.service.ConfigurationService;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;
import com.saic.uicds.core.xmldb.XMLDBConnection;

/**
 * The ConfigurationService Implementation
 * 
 * @ssdd
 */
public class ConfigurationServiceImpl implements ConfigurationService {

    /** The log. */
    Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private static XMLDBConnection xmldbConnection = null;
    
    /** The Constant serviceUrnPrefix. */
    static final String serviceUrnPrefix = "urn:uicds:service:";

    /** The base url. */
    private static String baseURL = "https://localhost/uicds/core/ws/services";

    /** The rest base url. */
    private static String restBaseURL = "https://localhost/uicds/api/";

    /** The configuration data */
    private XmlObject config;
    
    /** The core jid. */
    private String coreJID;

    // set to a default value for the JUnit test
    /** The core name. */
    private String coreName = "uicds@localhost";

    /**
     * Sets the core jid.
     * 
     * @param coreJID the new core jid
     * @ssdd
     */
    public void setCoreJID(String coreJID) {
        this.coreJID = coreJID;
    }

    /**
     * Sets the base url.
     * 
     * @param url the new base url
     * @ssdd
     */
    public void setBaseURL(String url) {
        baseURL = url;
    }

    /**
     * Inits the.
     */
    @PostConstruct
    public void init() {
        String fqn = getFullyQualifiedHostName();
        if (fqn != null && !fqn.isEmpty()) {
            baseURL = baseURL.replace("localhost", fqn);
            restBaseURL = restBaseURL.replace("localhost", fqn);
        }

        if (coreJID != null) {
            coreName = coreJID;
        }
        coreName = coreName.replace("localhost", fqn);
        // log.debug("=== > init() - fqn=" + fqn + " coreJID = " + coreJID + " coreName=" +
        // coreName);
    }

    /**
     * Gets the core name.
     * 
     * @return the core name
     * @ssdd
     */
    @Override
    public XmlObject getConfig() {
    	try {	
	    	XQueryService queryService = (XQueryService) xmldbConnection.getRootCollection().getService("XQueryService", "1.0");
	    	String query = "xquery version \"1.0\"; " +
		        "return " + "fn:doc(\"config.xml\")";
	    	CompiledExpression cquery = queryService.compile(query);
	    	ResourceSet result = queryService.execute(cquery);
            if (result.getSize() >= 1) {
                ResourceIterator it = result.getIterator();
                Resource resource = it.nextResource();
                return XmlObject.Factory.parse(resource.toString());
            } else {
                return null;
            }
    	} catch (XMLDBException xmldbe) {
    		log.error(xmldbe.getMessage());
    		return null;
    	} catch (XmlException xmle) {
    		log.error(xmle.getMessage());
    		return null;
		}
       
    }
    
    /**
     * Gets the core name.
     * 
     * @return the core name
     * @ssdd
     */
    @Override
    public String getCoreName() {
        return coreName;
    }

    /**
     * Gets the host name.
     * 
     * @return the host name
     * @ssdd
     */
    @Override
    public String getHostName() {
        /*
         * String hostname = null; try { hostname =
         * InetAddress.getLocalHost().getHostName().toLowerCase(); } catch (UnknownHostException e)
         * { e.printStackTrace(); hostname = "Unknown Host"; } return hostname;
         */

        return getFullyQualifiedHostName();
    }

    /**
     * Gets the fully qualified host name.
     * 
     * @return the fully qualified host name
     * @ssdd
     */
    @Override
    public String getFullyQualifiedHostName() {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            hostname = "Unknown Host";
        }
        return hostname;
    }

    /**
     * Gets the fully qualified service name urn.
     * 
     * @param serviceName the service name
     * 
     * @return the fully qualified service name urn
     * @ssdd
     */
    public String getFullyQualifiedServiceNameURN(String serviceName) {
        return getServiceUrnPrefix() + serviceName;
    }

    /**
     * Gets the service urn prefix.
     * 
     * @return the service urn prefix
     * @ssdd
     */
    public String getServiceUrnPrefix() {
        return serviceUrnPrefix + getFullyQualifiedHostName() + ".";
    }

    /**
     * Gets the web service base url.
     * 
     * @return the web service base url
     * @ssdd
     */
    @Override
    public String getWebServiceBaseURL() {
        return baseURL;
    }

    /**
     * Gets the service name urn.
     * 
     * @param serviceName the service name
     * 
     * @return the service name urn
     * @ssdd
     */
    @Override
    public String getServiceNameURN(String serviceName) {
        return serviceUrnPrefix + serviceName;
    }

    /**
     * Gets the rest base url.
     * 
     * @return the rest base url
     * @ssdd
     */
    @Override
    public String getRestBaseURL() {
        return restBaseURL;
    }

	public XMLDBConnection getXmldbConnection() {
		return xmldbConnection;
}

	public void setXmldbConnection(XMLDBConnection xmldbConnection) {
		ConfigurationServiceImpl.xmldbConnection = xmldbConnection;
	}
}
