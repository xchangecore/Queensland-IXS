package com.saic.uicds.core.em.service;

import org.uicds.organizationElement.OrganizationElementDocument;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;

/**
 * The IncidentCommandService manages ICS and MACS type work products that associate Resource
 * Instances to Resource Profiles in an organizational structure.
 * 
 * @author Nathan Lewnes
 * @since 1.0
 * @ssdd
 */
public interface IncidentCommandService {

    public static final String IncidentCommanderRole = "Incident Commander";
    public static final String ICSType = "ICS";
    public static final String MACSType = "MACS";

    public static final String ICS_SERVICE_NAME = "IncidentCommandService";

    /**
     * Get an Incident Command Structure for a particular incident
     * 
     * @param incidentID - the id of the incident
     * @return WorkProduct - the WorkProduct includes the WorkProductIdenitification as the
     *         Identification
     * @see IncidentCommandStructureDocument
     * @ssdd
     */
    public WorkProduct getCommandStructureByIncident(String incidentID);

    /**
     * Create/Update the organization and associate with incidentId if existed.
     * 
     * @param org - Organization you want to update
     * @return organizationID
     * @ssdd
     */
    public ProductPublicationStatus updateCommandStructure(IdentificationType pkgId,
        OrganizationElementDocument org, String incidentID);

    /**
     * System initialized handler.
     * 
     * @param message the message
     * @ssdd
     */
    public void systemInitializedHandler(String message);
}
