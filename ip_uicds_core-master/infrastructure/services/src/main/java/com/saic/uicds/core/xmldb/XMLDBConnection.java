package com.saic.uicds.core.xmldb;

import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLDBConnection {

	public final String XMLDB_COMPONENT_NAME = "XMLDBComponent";
	
	private String driver = null;
	private String connectionURI = null;
	private String username = null;
	private String password = null;
	private int maxRetries = 5;
	
	private Collection rootCollection = null;
	
	Logger log = LoggerFactory.getLogger(XMLDBConnection.class);
		
	public Boolean isConnected() {
		
    	Boolean initialized = false;
    	try {
    		Database dbList[] = DatabaseManager.getDatabases();
    		initialized = true;
	    } catch (Exception e) {
	    	initialized = false;
	    }

        return initialized;
    }
	
    public Boolean updateConnection() {
    	
    	// check that all required fields are set
    	if (driver != null && connectionURI != null && username != null && password != null) {
	        try {
	        	// initialize database driver
	        	log.info("Initializing xml:db driver ...");
	        	Class cl = Class.forName(driver);
	        	Database database = (Database) cl.newInstance();
	        	DatabaseManager.registerDatabase(database);
		       	
	        	log.info("Using root collection: " + getConnectionURI());
		       	
	        	// Setup root collection and services
	        	log.info("Initializing DatabaseManager");
	        	rootCollection = DatabaseManager.getCollection(getConnectionURI(), username, password);
		
	        	if (rootCollection != null) {
	        		log.info("XML:DB System initialized using root collection: " + rootCollection.getName());
	        		return true;
	        	} else {
	        		log.error("System failed to initialize XML:DB connection to collection: " + getConnectionURI());
	        		return false;
	        	}
	           
	        } catch (Throwable e) {
	        	log.error("Error registering XML:DB driver: " + e.getMessage());
	        	e.printStackTrace();
	        	return false;
	        }
    	} else {
    		return false;
    	}
   }
	
	public void setDriver(String driverString) {
		driver = driverString;
		updateConnection();
	}

	public String getDriver() {
		return driver;
	}

	public void setConnectionURI(String connectionURIString) {
		connectionURI = connectionURIString;
		updateConnection();
	}

	public String getConnectionURI() {
		return connectionURI;
	}

	public void setUsername(String usernameString) {
		username = usernameString;
		updateConnection();
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String passwordString) {
		password = passwordString;
		updateConnection();
	}

	public void setMaxRetries(int maxRetriesInt) {
		maxRetries = maxRetriesInt;
		updateConnection();
	}

	public int getMaxRetries() {
		return maxRetries;
	}	
	
	public Collection getRootCollection() {
		return rootCollection;
	}

}
