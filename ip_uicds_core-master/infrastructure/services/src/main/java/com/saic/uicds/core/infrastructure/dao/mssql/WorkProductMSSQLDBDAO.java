package com.saic.uicds.core.infrastructure.dao.mssql;

import gov.ucore.ucore.x20.DigestDocument;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.uicds.core.infrastructure.dao.WorkProductDAO;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

public class WorkProductMSSQLDBDAO
    extends AbstractMSSQLDBDAO
    implements WorkProductDAO {

    private Logger log = LoggerFactory.getLogger(WorkProductMSSQLDBDAO.class);

    // do the sql way for now
    private Statement stmt = null;
    private String driver = null;
    private String connectionURI = null;
    private String username = null;
    private String password = null;
    private Connection connection = null;

    private static final String tablerecords = "WorkProductList";
    private static final String sqlrow = "WorkProduct";

    public WorkProductMSSQLDBDAO() {

        super("/WorkProducts");
        stmt = null;
    }

    @Override
    public WorkProduct findBySerachCritia(String searchSQL) {

        log.debug("calling findBySerachCritia to get single WorkProduct... ");

        if (isConnected()) {
            return doWPWork(searchSQL);
        } else {
            doConnection();
            return doWPWork(searchSQL);
        }
    }

    private WorkProduct doWPWork(String SQL) {

    	log.debug("doWPWork - start - SQL="+SQL);
        try {
            if (stmt != null) {
                ResultSet rs = stmt.executeQuery(SQL);
                if (rs.getRow() == 0)
                	log.debug("No record is found.");
                
                while (rs.next()) {
                    WorkProduct obj = createWorkProduct(rs);
                    // only one object
                    return obj;
                }

                rs.close();
                // stmt.close();
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
        	log.error("doWPWork - error:"+e.getMessage());
            e.printStackTrace();
        }

        log.debug("doWPWork - end");
        
        return null;
    }

    @Override
    public List<WorkProduct> findWPListBySearch(String searchSQL) {

        log.debug("calling findWPListBySearch to get single WorkProduct... ");

        if (isConnected()) {
            return doWPListWork(searchSQL);
        } else {
            doConnection();
            return doWPListWork(searchSQL);
        }

    }

    private List<WorkProduct> doWPListWork(String SQL) {

    	log.debug("doWPListWork - start");
        List<WorkProduct> wplist = new ArrayList<WorkProduct>();
        try {

            if (stmt != null) {
                ResultSet rs = stmt.executeQuery(SQL);
                while (rs.next()) {
                    WorkProduct obj = createWorkProduct(rs);
                    // add this object onto the object list
                    wplist.add(obj);
                }
                rs.close();
                // stmt.close();
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.debug("doWPListWork - complete");
        return wplist;
    }

    public Document findDocsBySearchCriteria(String searchSQL) throws ParserConfigurationException,
        SQLException {

        log.debug("calling findDocsBySearchCriteria to get single WorkProduct... ");
        if (isConnected()) {
            return doFindDom(searchSQL);
        } else {
            doConnection();
            return doFindDom(searchSQL);
        }
    }

    @SuppressWarnings("unused")
    private Document doFindDom(String searchSQL) throws ParserConfigurationException, SQLException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element results = doc.createElement(tablerecords);
        doc.appendChild(results);

        ResultSet rs = connection.createStatement().executeQuery(searchSQL);
        ResultSetMetaData rsmd = rs.getMetaData();

        int colCount = rsmd.getColumnCount();

        while (rs.next()) {

            Node wpNode = WorkProductHelper.toWorkProductSummary(createWorkProduct(rs)).getDomNode();
            Node importedWPNode = doc.importNode(wpNode, true);
            results.appendChild(importedWPNode);

        }

        if (doc != null) {
            return doc;
        } else {
            return null;
        }
    }

    // -------------------WorkProductDAO calls ------------------
    @Override
    public boolean exists(Integer id) {

        String sql = "select * from workproducts where ID=" + id;
        log.debug("exists. SQL-"+sql);
        WorkProduct obj = findBySerachCritia(sql);

        if (obj != null) {
            return true;
        } else {
            return false;
        }

    }

    public WorkProduct findByID(Integer id) {

        String sql = "select * from workproducts where ID=" + id;
        log.debug("findByID. SQL - "+sql);
        
        WorkProduct obj = findBySerachCritia(sql);

        if (obj != null) {
            return obj;
        } else {
            return null;
        }

    }

    public boolean existsWithProductID(String productID) {

    	log.debug("existsWithProductID. productID="+productID);
        WorkProduct obj = findByProductID(productID);

        if (obj != null) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public synchronized List<WorkProduct> findAll() {

        String sql = "select * from workproducts";
        log.debug("findAll. sql-"+sql);
        
        return findWPListBySearch(sql);
    }

    @Override
    public List<WorkProduct> findByExample(WorkProduct exampleInstance, String... excludeProperty) {

        log.error("!!! findByExample(WorkProduct exampleInstance,	String... excludeProperty)");
        return null;
    }

    @Override
    public WorkProduct findById(Integer id) {

        String searchSQL = "select * from workproducts where ID=" + id;
        
        log.debug("findById. SQL-"+searchSQL);
        return findBySerachCritia(searchSQL);
    }

    @Override
    public WorkProduct findById(Integer id, boolean lock) {

        log.error("!!! findById(Integer id, boolean lock)");
        return null;
    }

    /**
     * Make the entity persistent. If the entity is already persistent, then ensure that it is
     * saved.
     */
    // @SuppressWarnings("static-access")
    @Override
    public WorkProduct makePersistent(WorkProduct entity) {

        boolean bISNew = false;
        if (entity != null) {
            // Reorganize this entity parameters to do the sql process
            String xmlStr1 = "";
            if (entity.getProduct() != null) {
                xmlStr1 = entity.getProduct().toString();
            }

            // do not set null, because ms sql will treat it differently.
            String digestxmlStr1 = "";
            if (entity.getDigest() != null) {
                digestxmlStr1 = entity.getDigest().toString();
            }

            String asgListStr = null;
            Set<String> sList = entity.getAssociatedInterestGroupIDs();
            List<String> list = new ArrayList<String>(sList);

            // as said, for now, we only have one string in the list.
            if (list.size() > 0) {
                asgListStr = list.get(0);
            } else {
                asgListStr = "";
            }

            java.util.Date utilDate = null;

            if (entity.getCreatedDate() != null) {
                utilDate = entity.getCreatedDate();
            } else {
                utilDate = new java.util.Date();
            }

            java.sql.Timestamp createdDate = new java.sql.Timestamp(utilDate.getTime());

            if (entity.getUpdatedDate() != null) {
                utilDate = entity.getUpdatedDate();
            } else {
                utilDate = new java.util.Date();
            }

            java.sql.Timestamp lastupdatedDate = new java.sql.Timestamp(utilDate.getTime());

            String state = "active";
            boolean bState = entity.getActive();
            if (bState != false) {
                state = "active";
            } else {
                state = "Inactive";
            }

            // some strings have single quote, need to replace them with double quote.
            String xmlStr = xmlStr1.replace("'", "''");
            String digestxmlStr = digestxmlStr1.replace("'", "''");

            // create the sql statement
            String sql = null;

            if (entity.getId() != null) {
                bISNew = false;
                boolean bCheck = false;

                // 1: make sure there is one entity with this id in the db.
                int id = entity.getId();

                String incomingWPProductID = entity.getProductID();
                WorkProduct obj = findByID(id);
                String existingWPProductID = null;
                if (obj != null) {
                    existingWPProductID = obj.getProductID();
                }

                if (incomingWPProductID.equalsIgnoreCase(existingWPProductID)) {
                    bCheck = true;
                }

                // if (exists(id) != false)
                // no matter single core or multi core communication, as long as that id/workproduct
                // has the same productID as the incoming work product's, then update it.
                if (bCheck == true) {
                    // there is one entity with the same id in the database.
                    // update this entity but keep the same id as the incoming entity's id
                    sql = null;
                    sql = "update workproducts " + "set ProductTypeVersion='"
                        + entity.getProductTypeVersion() + "'" + "," + "ProductVersion="
                        + entity.getProductVersion() + "," + "WPType='" + entity.getProductType()
                        + "'" + "," + "WPChecksum='" + entity.getChecksum() + "'" + ","
                        + "Created='" + createdDate + "'" + "," + "CreatedBy='"
                        + entity.getCreatedBy() + "'" + "," + "LastUpdated='" + lastupdatedDate
                        + "'" + "," + "LastUpdatedBy='" + entity.getUpdatedBy() + "'" + ","
                        + "Kilobytes='" + entity.getSize() + "'" + "," + "Mimetype='"
                        + entity.getMimeType() + "'" + "," + "AssociatedGroups='" + asgListStr
                        + "'" + "," + "RawXML='" + xmlStr + "'" + "," + "State='" + state + "'"
                        + "," + "ProductID='" + entity.getProductID() + "'" + "," + "DigestXML='"
                        + digestxmlStr + "'" + "where ID=" + id;

                }
                // for multi core transaction, id will not be good to use because they are most
                // likely
                // not the same, we should use ProductID.
                else if (existsWithProductID(entity.getProductID()) != false) {

                    sql = null;
                    sql = "update workproducts " + "set ProductTypeVersion='"
                        + entity.getProductTypeVersion() + "'" + "," + "ProductVersion="
                        + entity.getProductVersion() + "," + "WPType='" + entity.getProductType()
                        + "'" + "," + "WPChecksum='" + entity.getChecksum() + "'" + ","
                        + "Created='" + createdDate + "'" + "," + "CreatedBy='"
                        + entity.getCreatedBy() + "'" + "," + "LastUpdated='" + lastupdatedDate
                        + "'" + "," + "LastUpdatedBy='" + entity.getUpdatedBy() + "'" + ","
                        + "Kilobytes='" + entity.getSize() + "'" + "," + "Mimetype='"
                        + entity.getMimeType() + "'" + "," + "AssociatedGroups='" + asgListStr
                        + "'" + "," + "RawXML='" + xmlStr + "'" + "," + "State='" + state + "'"
                        + ","
                        // + "ProductID='" + entity.getProductID() + "'" + ","
                        + "DigestXML='" + digestxmlStr + "'" + "where ProductID='"
                        + entity.getProductID() + "'";
                } else {
                    // insert it into the db? NO!! Just return null
                    return null;
                }
            } else {

                if (existsWithProductID(entity.getProductID()) != false) {
                    bISNew = false;
                    sql = null;
                    sql = "update workproducts " + "set ProductTypeVersion='"
                        + entity.getProductTypeVersion() + "'" + "," + "ProductVersion="
                        + entity.getProductVersion() + "," + "WPType='" + entity.getProductType()
                        + "'" + "," + "WPChecksum='" + entity.getChecksum() + "'" + ","
                        + "Created='" + createdDate + "'" + "," + "CreatedBy='"
                        + entity.getCreatedBy() + "'" + "," + "LastUpdated='" + lastupdatedDate
                        + "'" + "," + "LastUpdatedBy='" + entity.getUpdatedBy() + "'" + ","
                        + "Kilobytes='" + entity.getSize() + "'" + "," + "Mimetype='"
                        + entity.getMimeType() + "'" + "," + "AssociatedGroups='" + asgListStr
                        + "'" + "," + "RawXML='" + xmlStr + "'" + "," + "State='" + state + "'"
                        + ","
                        // + "ProductID='" + entity.getProductID() + "'" + ","
                        + "DigestXML='" + digestxmlStr + "'" + "where ProductID='"
                        + entity.getProductID() + "'";

                } else {
                    bISNew = true;
                    sql = null;
                    sql = "INSERT INTO workproducts  (ProductTypeVersion, WPType, WPChecksum, "
                        + "State, Created, CreatedBy, LastUpdated, LastUpdatedBy, Kilobytes, Mimetype, AssociatedGroups, RawXML, "
                        + "ProductVersion, ProductID, DigestXML) VALUES (" + "'"
                        + entity.getProductTypeVersion()
                        + "'"
                        + ","
                        + "'"
                        + entity.getProductType()
                        + "'"
                        + ","
                        + "'"
                        + entity.getChecksum()
                        + "'"
                        + ","
                        + "'"
                        + state
                        + "'"
                        + ","
                        + "'"
                        + createdDate
                        + "'"
                        + ","
                        + "'"
                        + entity.getCreatedBy()
                        + "'"
                        + ","
                        + "'"
                        + lastupdatedDate
                        + "'"
                        + ","
                        + "'"
                        + entity.getUpdatedBy()
                        + "'"
                        + ","
                        + "'"
                        + entity.getSize()
                        + "'"
                        + ","
                        + "'"
                        + entity.getMimeType()
                        + "'"
                        + ","
                        + "'"
                        + asgListStr
                        + "'"
                        + ","
                        + "'"
                        + xmlStr
                        + "'"
                        + ","
                        + entity.getProductVersion()
                        + ","
                        + "'"
                        + entity.getProductID() + "'" + "," + "'" + digestxmlStr + "'" + ")";
                }

            }

            // do the sql wwith the ms sql database
            try {
                if ((connection != null) && (stmt != null)) {
                	log.debug("makePersistent. sql - "+sql);
                    stmt.executeUpdate(sql);
                    connection.commit();

                    if (bISNew == true) {
                        // System.out.println("Transaction succeeded. the records were written to the database.");
                    } else {
                        // System.out.println("Transaction succeeded. the records were updated.");

                    }
                }

            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // This commits the transaction and starts a new one.
        }

        if (bISNew == true) {
            // for this case, we need to get the id which the mssql db gave to the new insert row,
            // and
            // reset the incoming entity's id, before the db operation, the entity's id is null.
            // now, need to set it with the db given id.
            int newID = getMaxID();
            entity.setId(newID);
        }

        return entity;
    }

    /**
     * Make the entity transient. This will cause the entity to be removed from the database.
     * 
     * @param entity
     * @throws SQLException
     */
    @Override
    public void makeTransient(WorkProduct entity) {

        // remove them after done the dao test
        archiveProduct(entity);
    }

    private void archiveProduct(WorkProduct entity) {

        if (entity != null) {
            String delSql = "delete from workproducts where ProductID ='"
                + entity.getProductID() + "'";
            if (connection != null) {
                if (stmt != null) {
                    try {
                    	log.debug("archiveProduct. sql - "+delSql);
                    	
                        stmt.executeUpdate(delSql);
                        // This commits the transaction and starts a new one.
                        connection.commit();
                        // System.out.println("Transaction succeeded. the records was deleted.");
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        }

    }

    private int getMaxID() {

        log.debug("calling getMaxID... ");
        String SQL = "select id from workproducts";

        if (isConnected()) {
            return doGetMaxID(SQL);
        } else {
            doConnection();
            return doGetMaxID(SQL);
        }
    }

    // the newest id is always the biggest integer in the database table because it is incremented
    // automatically.
    private int doGetMaxID(String searchSQL) {

        int maxID = 0;
        int tempID = 0;
        try {
            if (stmt != null) {
                ResultSet rs = stmt.executeQuery(searchSQL);
                while (rs.next()) {
                    tempID = rs.getInt("ID");
                    if (tempID > maxID) {
                        maxID = tempID;
                    }
                }

                rs.close();

            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return maxID;
    }

    @Override
    public boolean isSessionInitialized() {

        return this.isConnected();
    }

    @Override
    public List<WorkProduct> findAllClosedVersionOfProduct(String productID) {

        String searchSQL = "select * from workproducts where ProductID='" + productID + "'";
        log.debug("findAllClosedVersionOfProduct. SQL - "+searchSQL);
        return findWPListBySearch(searchSQL);
    }

    @Override
    public List<WorkProduct> findAllVersionOfProduct(String productID) {

        String searchSQL = "select * from workproducts where ProductID='" + productID + "'";
        log.debug("findAllVersionOfProduct. SQL - "+searchSQL);
        return findWPListBySearch(searchSQL);
    }

    @Override
    public List<WorkProduct> findByInterestGroup(String interestGroupID) {

        String searchSQL = "select * from workproducts where AssociatedGroups='"
            + interestGroupID + "'";
       log.debug("findByInterestGroup. SQL - "+searchSQL);
       return findWPListBySearch(searchSQL);

    }

    @Override
    public List<WorkProduct> findByInterestGroupAndType(String interestGroupID, String type) {

        String searchSQL = "select * from workproducts where AssociatedGroups='"
            + interestGroupID + "'" + " and WPType='" + type + "'";
        log.debug("findByInterestGroupAndType. SQL - "+searchSQL);
        return findWPListBySearch(searchSQL);
    }

    @Override
    public WorkProduct findByProductID(String productID) {

        String searchSQL = "select * from workproducts where ProductID='" + productID + "'";
        log.debug("findByProductID. SQL - "+searchSQL);
        return findBySerachCritia(searchSQL);
    }

    @Override
    public WorkProduct findByProductIDAndVersion(String productID, Integer productVersion) {

        String searchSQL = "select * from workproducts where ProductID='" + productID + "'"
            + " and ProductVersion =" + productVersion;
        log.debug("findByProductIDAndVersion. SQL - "+searchSQL);
        return findBySerachCritia(searchSQL);
    }

    @Override
    public List<WorkProduct> findByProductType(String productType) {

        String searchSQL = "select * from workproducts where WPType='" + productType + "'";
        log.debug("findByProductType. SQL - "+searchSQL);
        return findWPListBySearch(searchSQL);
    }

    @Override
    public WorkProduct findByWorkProductIdentification(IdentificationType pkgId) {

    	log.debug("findByWorkProductIdentification - start");

        if (pkgId != null) {
            String productID = "";
            String cs = "";
            String state = "";
            String type = "";
            int productVersion = 0;

            if (pkgId.getIdentifier() != null) {
                productID = pkgId.getIdentifier().getStringValue();
            }

            if (pkgId.getChecksum() != null) {
                cs = pkgId.getChecksum().getStringValue();
            }

            if (pkgId.getType() != null) {
                type = pkgId.getType().getStringValue();
            }

            if (pkgId.getState() != null) {
                state = pkgId.getState().toString();
            }

            if (pkgId.getVersion() != null) {
                productVersion = Integer.parseInt(pkgId.getVersion().getStringValue());
            }

            String searchSQL = "select * from workproducts where ProductVersion ="
                + productVersion + " and State ='" + state + "'" + " and WPType='" + type + "'"
                + " and ProductID='" + productID + "'" + " and WPChecksum='" + cs + "'";

            log.debug("findByWorkProductIdentification. SQL - "+searchSQL);

            return findBySerachCritia(searchSQL);
        }

        return null;

    }

    // ------------------End of WorkProductDAO calls ------------------

    // ms sql server database connection
    public Boolean doConnection() {

        // check that all required fields are set
        if (driver != null && connectionURI != null && username != null && password != null) {
        	log.debug("doConnection - start");
        	// Load the JDBC driver
            try {
                Class.forName(driver);

                // Create a connection to the database
                try {
                    connection = DriverManager.getConnection(connectionURI, username, password);
                    stmt = connection.createStatement();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        	log.debug("doConnection - end");
        	
            return true;
        }

        return false;
    }

    public String getDriver() {

        return driver;
    }

    public void setDriver(String driver) {

        this.driver = driver;
        doConnection();
    }

    public String getConnectionURI() {

        return connectionURI;
    }

    public void setConnectionURI(String connectionURI) {

        this.connectionURI = connectionURI;
        doConnection();
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
        doConnection();
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(String password) {

        this.password = password;
        doConnection();
    }

    public Boolean isConnected() {

        Boolean initialized = false;
        if ((connection != null) && (stmt != null)) {
            initialized = true;
        } else {
            initialized = false;
        }

        return initialized;
    }

    private WorkProduct createWorkProduct(ResultSet rs) throws SQLException {

    	log.debug("createWorkProduct - start");
        WorkProduct obj = new WorkProduct();
        obj.setId(rs.getInt("ID"));
        try {
            obj.setProductTypeVersion(rs.getString("ProductTypeVersion"));
        } catch (SQLException e) {
            log.error("no ProductTypeVersion column");
        }
        obj.setProductVersion(rs.getInt("ProductVersion"));
        obj.setProductType(rs.getString("WPType"));
        obj.setChecksum(rs.getString("WPChecksum"));
        obj.setCreatedDate(rs.getDate("Created"));
        obj.setCreatedBy(rs.getString("CreatedBy"));
        obj.setUpdatedDate(rs.getDate("LastUpdated"));
        obj.setUpdatedBy(rs.getString("LastUpdatedBy"));

        // make sure this as below!!
        BigDecimal bg = rs.getBigDecimal("Kilobytes");
        if (bg != null) {
            int wpsize = bg.intValue();
            obj.setSize(wpsize);
        }

        obj.setMimeType("Mimetype");

        XmlObject xobj = null;
        try {
            xobj = createXmlObject(rs);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (xobj != null) {
            obj.setProduct(xobj);
        }

        // here we only have one string in the workproducts table
        obj.setAssociatedInterestGroupIDs(createAGIDs(rs));

        String ste = rs.getString("State");
        if (ste.equalsIgnoreCase("active")) {
            obj.setActive(true);
        } else {
            obj.setActive(false);
        }

        obj.setProductID(rs.getString("ProductID"));

        DigestDocument digestDoc = null;
        try {
            digestDoc = createDigestObject(rs);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (digestDoc != null) {
            obj.setDigest(digestDoc);
        }

        log.debug("createWorkProduct - complete");
        return obj;
    }

    private Set<String> createAGIDs(ResultSet rs) throws SQLException {

        List<String> list = new ArrayList<String>();
        String str = rs.getString("AssociatedGroups");
        if ((str != null) && !str.isEmpty()) {
            list.add(str);
        }

        if (list.size() > 0) {
            Set<String> nset = new HashSet<String>(list);
            return nset;
        }

        return new HashSet<String>();

    }

    private XmlObject createXmlObject(ResultSet rs) throws SQLException, IOException {

        XmlObject xobj = null;
        SQLXML sqlxml = rs.getSQLXML("RawXML");
        if (sqlxml != null) {
            InputStream binaryStream = sqlxml.getBinaryStream();
            if (binaryStream != null && binaryStream.available() > 0) {
                try {
                    xobj = XmlObject.Factory.parse(binaryStream);
                } catch (XmlException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }

        return xobj;
    }

    private DigestDocument createDigestObject(ResultSet rs) throws SQLException, IOException {

        DigestDocument digestDoc = null;
        // XmlObject xobj =null;
        SQLXML sqlxml = rs.getSQLXML("DigestXML");

        if (sqlxml != null) {
            InputStream binaryStream = null;
            binaryStream = sqlxml.getBinaryStream();

            if (binaryStream != null && binaryStream.available() > 0)
                try {
                    digestDoc = DigestDocument.Factory.parse(binaryStream); // stringBuffer.toString());
                } catch (XmlException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            if (digestDoc != null) {
                return digestDoc;
            }
        }

        return null;
    }

}
