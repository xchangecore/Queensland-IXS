package com.saic.uicds.core.em.adminconsole.client.model;

public interface UICDSConstants {

    public static final String UICDS = "Uicds Explorer";
    public static final String HEALTHSTATUS = "Health-Status";
    public static final String XmlView = "XML";
    public static final String FormView = "FORM";
    public static final int FormViewIndex = 0;
    public static final int XmlViewIndex = 1;
    public static final String ALT_TEXT = "Expand/Collapse";
    public static final String ALT_EXPANDED = "Expanded";
    public static final String ALT_COLLAPSED = "Collapsed";

    public static final String UICDSExplorer = "UICDS v1.2.2 Explorer";
    public static final String InterestGroupFolder = "Interest Groups";
    public static final String IncidentFolder = "Incidents";
    public static final String WorkProductFolder = "WorkProducts";
    public static final String CoreFolder = "Cores";
    public static final String AgreementFolder = "Agreements";
    public static final String ResourceInstanceFolder = "ResourceInstances";

    public static final String ProfileFolder = "Profiles";
    public static final String ServiceFolder = "Services";

    public static final String IncidentRSSFeed = "Incident RSS Feed";
    public static final String IncidentRSSFeedUrl = "/pub/search?productType=Incident&format=rss";

    // Close/Archive
    public static final String CloseProduct = "Close";
    public static final String CloseAndArchiveProduct = "Close And Archive";
    public static final String CloseAndArchiveAll = "Close And Archive All";
    public static final String ArchiveProduct = "Archive";

    // Agreement
    public static final String CreateAgreement = "Create Agreement";
    public static final String ToggleAgreementStatus = "Toggle Agreement Status";
    public static final String AddRule = "Add Rule";
    public static final String DeleteRule = "Delete Rule";
    public static final String RescindAgreement = "Rescind Agreement";

    public static final String OkButtonText = "OK";
    public static final String CancelButtonText = "Cancel";

    // for Create Agreement dialog
    public static final String RemoteCoreField = "Remote Core:";
    public static final String RemoteCoreExampleField = "Example: uicds@yourhost.com";

    // for Add Rule dialog
    public static final String RuleIdField = "Rule Id:";
    public static final String IncidentTypeField = "Incident Type:";

    // Profile
    public static final String CreateProfile = "Create Resource Profile";
    public static final String CreateResourceInstance = "Create Resource Instance";
    public static final String EditProfile = "Edit Resource Profile";
    public static final String EditResourceInstance = "Edit Resource Instance";

    public static final String IdentifierField = "Identifier";
    public static final String DescriptionField = "Description";
    public static final String InterestField = "Interest";
    public static final String ResourceTypeInfo = "Resource Type Info";
    public static final String ResourceField = "Resource";
    public static final String CategoryField = "Category";
    public static final String KindField = "Kind";
    public static final String MinCapField = "Minimum Capabilities";
    public static final String UpdateProfile = "Update Profile";
    public static final String UpdateResourceInstance = "Update Resource Instance";
    public static final String RemoveProfile = "Remove Profile";
    public static final String RemoveResourceInstance = "Remove Resource Instance";
    public static final String AddInterest = "Add Interest";
    public static final String RemoveInterest = "Remove Interest";
    public static final String Endpoint = "Endpoint";
    public static final String Profile = "Profile";

    public static final String LocalResourceIdField = "LocalResourceID";

    public static final int LayoutCellSpacing = 6;
    public static final int LocationOffset = 10;
    
    // TreeItem Descriptions
    public static final String CORE_DESCRIPTION="Cores: List of owning and shared cores registered";
    public static final String SERVICE_DESCRIPTION="Services: List of Infrastructure and Emergency Management services supported by core";
    public static final String AGREEMENT_DESCRIPTION="Agreements: List of agreements which are relationships between Cores in the form of information sharing agreements";
    public static final String PROFILE_DESCRIPTION="Profiles: List of Resource Profiles which is information about data interests for different roles with respect to the UICDS core";
    public static final String INTERESTGROUP_DESCRIPTION="Interest Groups: List of Interest Groups with a sublist of IG instances";
    public static final String INCIDENT_DESCRIPTION="Incident: List of Incidents";
    public static final String WORKPRODUCT_DESCRIPTION="WorkProduct: List of work products which are the primary data package used in the UICDS system";
    public static final String RESOURCEINSTANCE_DESCRIPTION="Resource Instances: List of resource instances which represent applications that are connected to the core or that have checked into a core";
    
    // public static final int LocationLeftOffset = 0;
    // public static final int LocationTopOffset = -100;

}
