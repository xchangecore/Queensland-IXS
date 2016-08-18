package com.saic.uicds.core.em.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.taskingService.TaskListDocument;
import org.uicds.taskingService.TaskListType;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.uicds.core.em.service.TaskingService;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

/**
 * The TaskingService implementation.
 * 
 * @author ron
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */
public class TaskingServiceImpl
    implements TaskingService, ServiceNamespaces {

    Logger log = LoggerFactory.getLogger(TaskingServiceImpl.class);

    private WorkProductService workProductService;

    private DirectoryService directoryService;

    @PostConstruct
    public void init() {

    }

    public void systemInitializedHandler(String messgae) {

        WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(TASKING_PRODUCT_TYPE);
        directoryService.registerUICDSService(NS_TaskingService, TASKING_SERVICE_NAME, typeList,
            typeList);
    }

    /** {@inheritDoc} */
    @Override
    public void setDirectoryService(DirectoryService service) {

        this.directoryService = service;
    }

    /** {@inheritDoc} */
    @Override
    public DirectoryService getDirectoryService() {

        return this.directoryService;
    }

    /** {@inheritDoc} */
    @Override
    public WorkProductService getWorkProductService() {

        return this.workProductService;
    }

    /** {@inheritDoc} */
    public void setWorkProductService(WorkProductService service) {

        this.workProductService = service;
    }

    /**
     * Creates the task list.
     * 
     * @param entityId the entity id
     * @param incidentId the incident id
     * @param taskList the task list
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus createTaskList(String entityId, String incidentId,
        TaskListType taskList) {

        log.debug("createTaskList");

        if (taskList.getEntityId() == null) {
            taskList.setEntityId(entityId);
        }
        log.debug("taskList.toString=" + taskList.toString());

        TaskListDocument taskListDocument = TaskListDocument.Factory.newInstance();
        taskListDocument.setTaskList(taskList);

        WorkProduct product = new WorkProduct();
        product.setProductType(TaskingService.TASKING_PRODUCT_TYPE);
        product.setCreatedDate(new Date());
        product.setProduct(taskListDocument);

        if (incidentId != null) {
            product.getAssociatedInterestGroupIDs().add(incidentId);
        }

        log.debug("product=" + taskList.toString());
        ProductPublicationStatus status = workProductService.publishProduct(product);

        log.debug("createTaskList DONE");
        return status;
    }

    /**
     * Gets the task list.
     * 
     * @param wpId the wp id
     * @return the task list
     * @ssdd
     */
    @Override
    public WorkProduct getTaskList(String wpId) {

        return getWorkProductService().getProduct(wpId);
    }

    /**
     * Gets the task list by incident id.
     * 
     * @param entityId the entity id
     * @param incidentId the incident id
     * @return the task list by incident id
     * @ssdd
     */
    public WorkProduct getTaskListByIncidentId(String entityId, String incidentId) {

        TaskListDocument taskListDocument = null;
        TaskListType taskList = null;

        // GetByType and IncidentID
        List<WorkProduct> wpList = workProductService.findByInterestGroupAndType(incidentId,
            TaskingService.TASKING_PRODUCT_TYPE);
        for (WorkProduct wp : wpList) {
            try {
                taskListDocument = (TaskListDocument) wp.getProduct();
                taskList = taskListDocument.getTaskList();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            if ((taskList != null) && (entityId != null)) {
                if (taskList.getEntityId().equals(entityId)) {
                    log.debug("getTaskList taskList.toString:\n" + taskList.toString());
                    return wp;
                }
            }
        }
        return null;

    }

    /**
     * Gets the task list by entity id and incident id.
     * 
     * @param entityId the entity id
     * @param incidentId the incident id
     * @return the task list by entity id and incident id
     * @ssdd
     */
    @Override
    public WorkProduct getTaskListByEntityIdAndIncidentId(String entityId, String incidentId) {

        log.debug("getTaskList");

        TaskListDocument taskListDocument = null;
        TaskListType taskList = null;

        // GetByType and IncidentID
        List<WorkProduct> wpList = workProductService.findByInterestGroupAndType(incidentId,
            TaskingService.TASKING_PRODUCT_TYPE);
        for (WorkProduct wp : wpList) {
            try {
                taskListDocument = (TaskListDocument) wp.getProduct();
                taskList = taskListDocument.getTaskList();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            if ((taskList != null) && (taskList.getEntityId().equals(entityId))) {
                log.debug("getTaskList taskList.toString:\n" + taskList.toString());
                return wp;
            }
        }
        return null;
    }

    /**
     * Delete task list.
     * 
     * @param wpId the wp id
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus deleteTaskList(String wpId) {

        log.debug("deleteTaskList");
        WorkProduct wp = workProductService.getProduct(wpId);

        ProductPublicationStatus status;

        if (wp == null) {
            status = new ProductPublicationStatus();
            status.setStatus(ProductPublicationStatus.FailureStatus);
            status.setReasonForFailure(wpId + " doesn't existed");
            return status;
        }

        // if it's still not closed, we need to close it first
        if (wp.isActive() == true) {
            status = getWorkProductService().closeProduct(
                WorkProductHelper.getWorkProductIdentification(wp));
            if (status.getStatus().equals(ProductPublicationStatus.FailureStatus))
                return status;
        }

        return getWorkProductService().archiveProduct(
            WorkProductHelper.getWorkProductIdentification(wp));
    }

    /**
     * Update task list.
     * 
     * @param taskList the task list
     * @param workProductIdentification the work product identification
     * @return the product publication status
     * @ssdd
     */
    @Override
    public ProductPublicationStatus updateTaskList(TaskListType taskList,
        IdentificationType workProductIdentification) {

        WorkProduct wp = workProductService.getProduct(workProductIdentification.getIdentifier().getStringValue());

        TaskListDocument taskListDoc = TaskListDocument.Factory.newInstance();
        taskListDoc.addNewTaskList().set(taskList);
        WorkProduct newWP = new WorkProduct(wp);
        newWP.setProduct(taskListDoc);

        newWP = WorkProductHelper.setWorkProductIdentification(newWP, workProductIdentification);

        ProductPublicationStatus status = workProductService.publishProduct(newWP);

        return status;
    }
}
