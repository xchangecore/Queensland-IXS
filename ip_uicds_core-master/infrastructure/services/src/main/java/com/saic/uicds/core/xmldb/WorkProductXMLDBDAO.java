package com.saic.uicds.core.xmldb;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.exist.xmldb.RemoteXMLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XQueryService;
import org.xmldb.api.modules.XUpdateQueryService;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.uicds.core.infrastructure.dao.WorkProductDAO;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.util.InfrastructureNamespaces;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

/**
 * @author summersw
 * 
 */
/**
 * @author summersw
 * 
 */
public class WorkProductXMLDBDAO
    extends AbstractXMLDBDAO
    implements WorkProductDAO {

    private Logger log = LoggerFactory.getLogger(WorkProductXMLDBDAO.class);

    public WorkProductXMLDBDAO() {

        super("/WorkProducts");
    }

    /**
     * Check to see if the identified entity exists.
     * 
     * @return true if the entity exists, false otherwise.
     */

    protected void refreshServices() {

        if (this.getXmldbConnection() != null && this.getXmldbConnection().isConnected()) {
            try {

                CollectionManagementService tmpSvc = (CollectionManagementService) this.getXmldbConnection().getRootCollection().getService(
                    "CollectionManagementService", "1.0");
                String path = this.getXmldbConnection().getRootCollection().getName()
                    + this.getSubCollectionName();

                this.setCollection(tmpSvc.createCollection(path));
                log.debug("Created collection: " + this.getCollection().getName());

                this.setMgtService((CollectionManagementService) this.getCollection().getService(
                    "CollectionManagementService", "1.0"));
                log.debug("Initialized CollectionManagementService using "
                    + this.getMgtService().getName());

                this.setXPathQueryService((XPathQueryService) this.getCollection().getService(
                    "XPathQueryService", "1.0"));
                log.debug("Initialized XPathQueryService using "
                    + this.getXPathQueryService().getName());

                this.setXQueryService((XQueryService) this.getCollection().getService(
                    "XQueryService", "1.0"));
                log.debug("Initialized XQueryService using " + this.getXQueryService().getName());

                this.setXUpdateQueryService((XUpdateQueryService) this.getCollection().getService(
                    "XUpdateQueryService", "1.0"));
                log.debug("Initialized XUpdateQueryService using "
                    + this.getXUpdateQueryService().getName());

                // set some namespaces
                this.getXPathQueryService().setNamespace("NS_UICDS",
                    InfrastructureNamespaces.NS_UICDS);
                this.getXPathQueryService().setNamespace("NS_NIEM_CORE",
                    InfrastructureNamespaces.NS_NIEM_CORE);
                this.getXPathQueryService().setNamespace("NS_UCORE",
                    InfrastructureNamespaces.NS_UCORE);
                this.getXPathQueryService().setNamespace("NS_UCORE_CODESPACE",
                    InfrastructureNamespaces.NS_UCORE_CODESPACE);
                this.getXPathQueryService().setNamespace("NS_ULEX_STRUCTURE",
                    InfrastructureNamespaces.NS_ULEX_STRUCTURE);
                this.getXPathQueryService().setNamespace("NS_PRECIS_BASE",
                    InfrastructureNamespaces.NS_PRECIS_BASE);
                this.getXPathQueryService().setNamespace("NS_PRECIS_STRUCTURES",
                    InfrastructureNamespaces.NS_PRECIS_STRUCTURES);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.error("Failed to instantiate DAO - XMLDBComponent is not intialized!");
        }

    }

    // TODO; change return type to WorkProductDocument
    private WorkProduct findByQuery(String query) {

        RemoteXMLResource document = null;
        CompiledExpression expression = null;

        try {

        	log.debug("findByQuery - start\nQuery="+query);
            expression = this.getXQueryService().compile(query);
            ResourceSet result = this.getXQueryService().execute(expression);

            if (result.getSize() >= 1) {
                ResourceIterator it = result.getIterator();
                document = (RemoteXMLResource) it.nextResource();
                //log.debug("findByQuery - remoteXMLResource doc="+document.getContent().toString());
                WorkProductDocument wpd = WorkProductDocument.Factory.parse(document.getContent().toString());
                //log.debug("findByQuery - work product=\n"+wpd.xmlText());
                return WorkProductHelper.toModel(wpd.getWorkProduct());
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        } finally {
            if (document != null) {
                try {
                    document.freeLocalResources();
                } catch (Exception e) {
                    log.error("failed to free local resources: " + e.getMessage());
                }
            }
            log.debug("findByQuery - end");
        }
    }

    // TODO; change return type to WorkProductDocument
    private List<WorkProduct> findListByQuery(String query) {

        ArrayList<WorkProduct> wpList = new ArrayList<WorkProduct>();
        RemoteXMLResource document = null;

        CompiledExpression expression = null;

        try {

            expression = this.getXQueryService().compile(query);
            ResourceSet result = this.getXQueryService().execute(expression);

            ResourceIterator it = result.getIterator();
            while (it.hasMoreResources()) {
                document = (RemoteXMLResource) it.nextResource();

                WorkProductDocument wpd = WorkProductDocument.Factory.parse(document.getContent().toString());
                wpList.add(WorkProductHelper.toModel(wpd.getWorkProduct()));
            }

            return wpList;

        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        } finally {
            if (document != null) {
                try {
                    document.freeLocalResources();
                } catch (Exception e) {
                    log.error("failed to free local resources: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Find all instances.
     */
    @Override
    public List<WorkProduct> findAll() {

        String query = "xquery version \"1.0\"; "
            + "declare namespace NS_PRECIS_STRUCTURES = \"http://www.saic.com/precis/2009/06/structures\"; "
            + "(: return the current version of all work products :) "
            + "for $wp in /NS_PRECIS_STRUCTURES:WorkProduct " + "return " + "$wp ";
        return this.findListByQuery(query);
    }

    /**
     * Make the entity persistent. If the entity is already persistent, then ensure that it is
     * saved.
     */
    @Override
    public WorkProduct makePersistent(WorkProduct entity) {

        String documentVer = entity.getProductVersion().toString();
        String documentType = null;
        String productID = null;

        Collection collection = null;
        RemoteXMLResource document = null;

        try {

            documentType = URLEncoder.encode(entity.getProductType(), "UTF-8");
            productID = URLEncoder.encode(entity.getProductID(), "UTF-8");

            log.debug("makePersistent: type: " + documentType + ", id: " + productID + ", ver: "
                + documentVer);

            // ensure collection exists for document by document type
            collection = this.getMgtService().createCollection(
                this.getCollection().getName() + "/" + documentType + "/" + productID);

            // Store the document (using the WP ID as the db ID)
            document = (RemoteXMLResource) collection.createResource(productID, "XMLResource");

            document.setContent(WorkProductHelper.toWorkProductDocument(entity));
            collection.storeResource(document);

        } catch (UnsupportedEncodingException e) {
            log.error("failed to persist document: " + e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("failed to persist document: " + e.getMessage());
            return null;
        } finally {
            if (document != null) {
                try {
                    document.freeLocalResources();
                } catch (Exception e) {
                    log.error("failed to free local resources: " + e.getMessage());
                }
            }
            // close the collection
            if (collection != null) {
                try {
                    collection.close();
                } catch (XMLDBException xmldbe) {
                    log.error("failed to close the collection: " + xmldbe.getMessage());
                }
            }
        }
        return entity;
    }

    /**
     * Make the entity transient. This will cause the entity to be removed from the database.
     * 
     * @param entity
     */
    @Override
    public void makeTransient(WorkProduct entity) {

        archiveProduct(entity);
    }

    public void archiveProduct(WorkProduct entity) {

        String documentVer = entity.getProductVersion().toString();
        String documentType = null;
        String productID = null;

        try {

            documentType = URLEncoder.encode(entity.getProductType(), "UTF-8");
            productID = URLEncoder.encode(entity.getProductID(), "UTF-8");

            log.debug("archiveProduct: type: " + documentType + ", id: " + productID + ", ver: "
                + documentVer);

            Collection collection = this.getCollection().getChildCollection(documentType).getChildCollection(
                productID);

            // remove current WP
            this.getMgtService().removeCollection(collection.getName());
            // remove WP revisions
            this.getMgtService().removeCollection("/db/system/versions" + collection.getName());

            // release the collection
            collection.close();

        } catch (XMLDBException e) {
            log.error("failed to archive document: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            log.error("failed to archive document: " + e.getMessage());
        } catch (Exception e) {
            log.error("may have failed to archive document: " + e.getMessage());
        }
    }

    /**
     * Ensure that the xmldb connection is active
     * 
     * @param entity
     */
    @Override
    public boolean isSessionInitialized() {

        return this.getXmldbConnection().isConnected();
    }

    @Override
    public List<WorkProduct> findAllClosedVersionOfProduct(String productID) {

        log.debug("findAllClosedVersionOfProduct " + productID);

        String state = "Inactive";
        String query = "xquery version \"1.0\"; "
            + "declare namespace NS_PRECIS_STRUCTURES = \"http://www.saic.com/precis/2009/06/structures\"; "
            + "declare namespace NS_PRECIS_BASE = \"http://www.saic.com/precis/2009/06/base\"; "
            + "declare namespace NS_ULEX_STRUCTURE = \"ulex:message:structure:1.0\"; "
            + "(: return all closed versions by product ID :) "
            + "let $id := '"
            + productID
            + "' "
            + "let $state:= '"
            + state
            + "' "
            + "for $wp in /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier=$id and NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:State=$state] "
            + "order by $wp/NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version "
            + "return " + "$wp ";

        return this.findListByQuery(query);
    }

    @Override
    public List<WorkProduct> findAllVersionOfProduct(String productID) {

        log.debug("findAllVersionOfProduct " + productID);

        String query = "xquery version \"1.0\"; "
            + "import module namespace v=\"http://exist-db.org/versioning\"; "
            + "declare namespace NS_PRECIS_STRUCTURES = \"http://www.saic.com/precis/2009/06/structures\"; "
            + "declare namespace NS_PRECIS_BASE = \"http://www.saic.com/precis/2009/06/base\"; "
            + "declare namespace NS_ULEX_STRUCTURE = \"ulex:message:structure:1.0\"; "
            + "(: return the all versions of a work product by id :) " + "let $id := '"
            + productID
            + "' "
            + "let $doc := fn:base-uri ( /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier=$id] ) "
            + "let $cur := "
            + "    if ($doc) "
            + "    then ( "
            + "       fn:doc($doc) "
            + "    ) else "
            + "       () "
            + "let $seq := "
            + "    if ($doc) "
            + "    then ( "
            + "       for $rev in v:revisions(fn:doc($doc)) "
            + "       order by $rev "
            + "       return "
            + "         if ($rev) "
            + "         then ( "
            + "            v:doc(fn:doc($doc), $rev) "
            + "         ) else "
            + "            fn:doc($doc) "
            + "    ) "
            + "    else "
            + "    () "
            + "return "
            + "($seq, $cur)";

        return this.findListByQuery(query);
    }

    @Override
    public List<WorkProduct> findByInterestGroup(String interestGroupID) {

        log.debug("findByInterestGroup " + interestGroupID);

        String query = "xquery version \"1.0\"; "
            + "declare namespace NS_PRECIS_STRUCTURES = \"http://www.saic.com/precis/2009/06/structures\"; "
            + "declare namespace NS_PRECIS_BASE = \"http://www.saic.com/precis/2009/06/base\"; "
            + "declare namespace NS_ULEX_STRUCTURE = \"ulex:message:structure:1.0\";"
            + "(: return the latest version of each work product by interest group :) "
            + "let $groupID := '"
            + interestGroupID
            + "' "
            + "for $wp in /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:AssociatedGroups/NS_PRECIS_BASE:Identifier=$groupID] "
            + "return " + "$wp ";

        return this.findListByQuery(query);
    }

    @Override
    public List<WorkProduct> findByInterestGroupAndType(String interestGroupID, String type) {

        log.debug("findByInterestGroupAndType " + interestGroupID + " and " + type);

        String query = "xquery version \"1.0\"; "
            + "declare namespace NS_PRECIS_STRUCTURES = \"http://www.saic.com/precis/2009/06/structures\"; "
            + "declare namespace NS_PRECIS_BASE = \"http://www.saic.com/precis/2009/06/base\"; "
            + "declare namespace NS_ULEX_STRUCTURE = \"ulex:message:structure:1.0\"; "
            + "(: return the latest version of each work product by interest group and type:) "
            + "let $type := '"
            + type
            + "' "
            + "let $groupID := '"
            + interestGroupID
            + "' "
            + "for $wp in /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductProperties/NS_PRECIS_BASE:AssociatedGroups/NS_PRECIS_BASE:Identifier=$groupID and NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type=$type ] "
            + "return " + "$wp ";

        return this.findListByQuery(query);
    }

    @Override
    public WorkProduct findByProductID(String productID) {

        log.debug("findByProductID " + productID);

        String query = "xquery version \"1.0\"; "
            + "declare namespace NS_PRECIS_STRUCTURES = \"http://www.saic.com/precis/2009/06/structures\"; "
            + "declare namespace NS_PRECIS_BASE = \"http://www.saic.com/precis/2009/06/base\"; "
            + "declare namespace NS_ULEX_STRUCTURE = \"ulex:message:structure:1.0\"; "
            + "let $id := '"
            + productID
            + "' "
            + "let $wp := /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier=$id] "
            + "return " + "$wp ";
        return this.findByQuery(query);

    }

    @Override
    public WorkProduct findByProductIDAndVersion(String productID, Integer productVersion) {

        log.debug("findByProductIDAndVersion " + productID + " and " + productVersion);

        String query = "xquery version \"1.0\"; "
            + "import module namespace v=\"http://exist-db.org/versioning\";  "
            + "declare namespace NS_PRECIS_STRUCTURES = \"http://www.saic.com/precis/2009/06/structures\";  "
            + "declare namespace NS_PRECIS_BASE = \"http://www.saic.com/precis/2009/06/base\";  "
            + "declare namespace NS_ULEX_STRUCTURE = \"ulex:message:structure:1.0\";  "
            + "let $vCollection := \"/db/system/versions\" " + "let $id := '"
            + productID
            + "' "
            + "let $version := '"
            + productVersion
            + "' "
            + "return  "
            + "if ($id) then "
            + "	let $documentURI := fn:base-uri ( /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier=$id] ) "
            + "	return "
            + "	if ($documentURI and $version) then "
            + "		let $revision := collection($vCollection)//v:version/v:properties[v:version=$version and v:document=$id][1]/v:revision "
            + "		return "
            + "		if ($revision) then "
            /*+ "			v:doc(doc($documentURI), $revision) "*/
            + "			fn:doc($documentURI)"
            + "		else "
            + "			let $documentURI := fn:base-uri ( /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Identifier=$id and NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Version=$version ] ) "
            + "			return  "
            + "			if ($documentURI) then "
            + "				fn:doc($documentURI) "
            + "			else " + "				() " + "	else  " + "		fn:doc($documentURI) " + "else " + "	() ";

        return this.findByQuery(query);
    }

    @Override
    public List<WorkProduct> findByProductType(String productType) {

        log.debug("findByProductType " + productType);

        String query = "xquery version \"1.0\"; "
            + "declare namespace NS_PRECIS_STRUCTURES = \"http://www.saic.com/precis/2009/06/structures\"; "
            + "declare namespace NS_PRECIS_BASE = \"http://www.saic.com/precis/2009/06/base\"; "
            + "declare namespace NS_ULEX_STRUCTURE = \"ulex:message:structure:1.0\"; "
            + "(: return the latest version of each work product of a specific type :) "
            + "let $type := '"
            + productType
            + "' "
            + "let $wp := /NS_PRECIS_STRUCTURES:WorkProduct[NS_ULEX_STRUCTURE:PackageMetadata/NS_PRECIS_STRUCTURES:WorkProductIdentification/NS_PRECIS_BASE:Type=$type] "
            + "return " + "$wp ";
        return this.findListByQuery(query);
    }

    // TODO: updated
    @Override
    public WorkProduct findByWorkProductIdentification(IdentificationType pkgId) {

        log.debug("findByWorkProductIdentification ");
        //+ pkgId.getIdentifier().getStringValue()
        // + " and " + pkgId.getVersion().getStringValue());
        String productID = pkgId.getIdentifier().getStringValue();
        int productVersion = Integer.parseInt(pkgId.getVersion().getStringValue());

        return this.findByProductIDAndVersion(productID, productVersion);

    }

    // THESE METHODS RETURN NULL AND SHOULD NOT BE USED - THEY ARE ONLY HERE TO SATISFY THE
    // INTERFACES

    /**
     * @see com.saic.uicds.core.dao.GenericDAO#exists(java.io.Serializable)
     * 
     *      XML:DB implementation doesn't rely on database record IDs
     */
    @Override
    public boolean exists(Integer id) {

        log.error("!!! exists(Integer id)");
        return false;
    }

    /**
     * @see com.saic.uicds.core.dao.GenericDAO#findByExample(java.lang.Object, java.lang.String[])
     * 
     *      Not currently used
     */
    @Override
    public List<WorkProduct> findByExample(WorkProduct exampleInstance, String... excludeProperty) {

        log.error("!!! findByExample(WorkProduct exampleInstance,	String... excludeProperty)");
        return null;
    }

    /**
     * @see com.saic.uicds.core.dao.GenericDAO#findById(java.io.Serializable)
     * 
     *      XML:DB implementation doesn't rely on database record IDs
     */
    @Override
    public WorkProduct findById(Integer id) {

        log.error("!!! findById(Integer id)");
        return null;
    }

    /**
     * @see com.saic.uicds.core.dao.GenericDAO#findById(java.io.Serializable, boolean)
     * 
     *      XML:DB implementation doesn't rely on database record IDs
     */
    @Override
    public WorkProduct findById(Integer id, boolean lock) {

        log.error("!!! findById(Integer id, boolean lock)");
        return null;
    }


}
