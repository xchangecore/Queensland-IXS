package com.saic.uicds.core.xmldb;

import org.xmldb.api.base.Collection;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XQueryService;
import org.xmldb.api.modules.XUpdateQueryService;

public interface XMLDBDAOInterface {

	public abstract Collection getCollection();

	public abstract void setCollection(Collection collection);

	public abstract void setSubCollectionName(String name);

	public abstract String getSubCollectionName();

	public abstract void setMgtService(CollectionManagementService mgtService);

	public abstract CollectionManagementService getMgtService();

	public abstract void setXPathQueryService(
			XPathQueryService xPathQueryService);

	public abstract XPathQueryService getXPathQueryService();

	public abstract void setXQueryService(XQueryService xQueryService);

	public abstract XQueryService getXQueryService();

	public abstract void setXUpdateQueryService(
			XUpdateQueryService xUpdateQueryService);

	public abstract XUpdateQueryService getXUpdateQueryService();

	public abstract void setXmldbConnection(XMLDBConnection conn);

	public abstract XMLDBConnection getXmldbConnection();

}