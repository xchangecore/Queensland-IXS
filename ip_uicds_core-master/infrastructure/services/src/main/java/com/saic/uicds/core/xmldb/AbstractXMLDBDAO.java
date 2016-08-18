package com.saic.uicds.core.xmldb;

import org.xmldb.api.base.Collection;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XQueryService;
import org.xmldb.api.modules.XUpdateQueryService;

abstract class AbstractXMLDBDAO implements XMLDBDAOInterface {

	public final String XMLDB_COMPONENT_NAME = "XMLDBComponent";
	
	private String subCollectionName = "";

	private Collection collection;
	private CollectionManagementService mgtService;
	private XPathQueryService xPathQueryService;
	private XQueryService xQueryService;
	private XUpdateQueryService xUpdateQueryService;

	private XMLDBConnection xmldbConnection;
	
	AbstractXMLDBDAO () {
		this("/unknown");
	}
	
	AbstractXMLDBDAO(String collectionName) {
		this.setSubCollectionName(collectionName);
	}

	abstract protected void refreshServices();
	
	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#getCollection()
	 */
	public Collection getCollection() {
		return collection;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#setCollection(org.xmldb.api.base.Collection)
	 */
	public void setCollection(Collection collection) {
		this.collection = collection;
	}
	

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#setSubCollectionName(java.lang.String)
	 */
	public void setSubCollectionName(String name) {
		this.subCollectionName = name;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#getSubCollectionName()
	 */
	public String getSubCollectionName() {
		return subCollectionName;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#setMgtService(org.xmldb.api.modules.CollectionManagementService)
	 */
	public void setMgtService(CollectionManagementService mgtService) {
		this.mgtService = mgtService;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#getMgtService()
	 */
	public CollectionManagementService getMgtService() {
		return mgtService;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#setXPathQueryService(org.xmldb.api.modules.XPathQueryService)
	 */
	public void setXPathQueryService(XPathQueryService xPathQueryService) {
		this.xPathQueryService = xPathQueryService;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#getXPathQueryService()
	 */
	public XPathQueryService getXPathQueryService() {
		return xPathQueryService;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#setXQueryService(org.xmldb.api.modules.XQueryService)
	 */
	public void setXQueryService(XQueryService xQueryService) {
		this.xQueryService = xQueryService;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#getXQueryService()
	 */
	public XQueryService getXQueryService() {
		return xQueryService;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#setXUpdateQueryService(org.xmldb.api.modules.XUpdateQueryService)
	 */
	public void setXUpdateQueryService(XUpdateQueryService xUpdateQueryService) {
		this.xUpdateQueryService = xUpdateQueryService;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#getXUpdateQueryService()
	 */
	public XUpdateQueryService getXUpdateQueryService() {
		return xUpdateQueryService;
	}

	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#setXmldbConnection(com.saic.uicds.core.xmldb.XMLDBConnection)
	 */
	public void setXmldbConnection(XMLDBConnection conn) {
		this.xmldbConnection = conn;
		this.refreshServices();
	}
	
	/* (non-Javadoc)
	 * @see com.saic.uicds.core.xmldb.XMLDBDAOInterface#getXmldbConnection()
	 */
	public XMLDBConnection getXmldbConnection() {
		return this.xmldbConnection;
	}
	
}
