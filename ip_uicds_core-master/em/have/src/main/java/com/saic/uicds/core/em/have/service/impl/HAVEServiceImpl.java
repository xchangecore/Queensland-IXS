package com.saic.uicds.core.em.have.service.impl;

import gov.ucore.ucore.x20.DigestDocument;
import gov.ucore.ucore.x20.DigestType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.oasisOpen.docs.wsn.b2.NotificationMessageHolderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import org.uicds.directoryServiceData.WorkProductTypeListType;
import org.uicds.haveService.EdxlDeResponseDocument;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;
import x0.oasisNamesTcEmergencyEDXLDE1.ValueSchemeType;
import x0.oasisNamesTcEmergencyEDXLHAVE1.HospitalStatusDocument;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.StateType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.uicds.core.em.exceptions.SendMessageErrorException;
import com.saic.uicds.core.em.have.service.HAVEService;
import com.saic.uicds.core.em.util.BroadcastUtil;
import com.saic.uicds.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.saic.uicds.core.infrastructure.exceptions.InvalidXpathException;
import com.saic.uicds.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.NoShareRuleInAgreementException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnavailableException;
import com.saic.uicds.core.infrastructure.exceptions.RemoteCoreUnknownException;
import com.saic.uicds.core.infrastructure.messages.Core2CoreMessage;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.CommunicationsService;
import com.saic.uicds.core.infrastructure.service.DirectoryService;
import com.saic.uicds.core.infrastructure.service.NotificationService;
import com.saic.uicds.core.infrastructure.service.WorkProductService;
import com.saic.uicds.core.infrastructure.service.impl.ProductPublicationStatus;
import com.saic.uicds.core.infrastructure.util.InfrastructureNamespaces;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;
import com.saic.uicds.core.infrastructure.util.UicdsStringUtil;
import com.saic.uicds.core.infrastructure.util.ValidationUtil;
import com.saic.uicds.core.infrastructure.util.WorkProductHelper;

/**
 * The ResourceManagementService implementation.
 * 
 * @author roger
 * @see com.saic.uicds.core.infrastructure.model.WorkProduct WorkProduct Data Model
 * @ssdd
 */

public class HAVEServiceImpl implements HAVEService, ServiceNamespaces,
    InfrastructureNamespaces {

    Logger log = LoggerFactory.getLogger(this.getClass());

    private WorkProductService workProductService;

    private DirectoryService directoryService;

    private CommunicationsService communicationsService;

    private NotificationService notificationService;

    public static final QName HAVE_RESOURCE_QNAME = HospitalStatusDocument.type.getDocumentElementName();

    public static final String NIMS_NS = "http://nimsonline.org/2.0";
    
    private String xsltFilePath;
    
    private javax.xml.transform.Source xsltSource;
    
    private ClassPathResource xsltResource;
    
    private javax.xml.transform.TransformerFactory transformerFactory;
    
    private javax.xml.transform.Transformer transformer;

    @Transactional
    public void systemInitializedHandler(String messgae) {

    	// Register service with the directory service.
        WorkProductTypeListType typeList = WorkProductTypeListType.Factory.newInstance();
        typeList.addProductType(HAVE_PRODUCT_TYPE);
        directoryService.registerUICDSService(NS_ResourceManagementService, HAVE_SERVICE_NAME,
            typeList, typeList);
        
        setupDigestTransformation();

    }

	public void setupDigestTransformation() {
		// Setup XSLT source for creating the digest of HAVE work products 
        xsltResource = new ClassPathResource(xsltFilePath);
        if (!xsltResource.exists()) {
        	log.error("Can't find XSLT to create HAVE digest: " + xsltFilePath);
        }

        xsltSource = null;
		try {
			xsltSource = new javax.xml.transform.stream.StreamSource(xsltResource.getInputStream());
		} catch (IOException e1) {	
			log.error("Error reading " + xsltFilePath + "as XSLT source.");
		}
		
		// create an instance of TransformerFactory
		try {
			transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
		} catch (TransformerFactoryConfigurationError e) {
			log.error("Error creating TransformerFactory for HAVE: " + e.getMessage());
		}
		
		// create the transformer
		try {
			transformer = transformerFactory.newTransformer(xsltSource);
		} catch (TransformerConfigurationException e) {
			log.error("Error creating Transformer for HAVE: " + e.getMessage());
		}
	}

    public void setDirectoryService(DirectoryService directoryService) {

        this.directoryService = directoryService;
    }

    public DirectoryService getDirectoryService() {

        return this.directoryService;
    }

    public WorkProductService getWorkProductService() {

        return this.workProductService;
    }

    public void setWorkProductService(WorkProductService service) {

        this.workProductService = service;
    }

    public CommunicationsService getCommunicationsService() {

        return communicationsService;
    }

    public void setCommunicationsService(CommunicationsService communicationsService) {

        this.communicationsService = communicationsService;
    }

    public NotificationService getNotificationService() {

        return notificationService;
    }

    public void setNotificationService(NotificationService notificationService) {

        this.notificationService = notificationService;
    }

	/**
	 * @return the xsltFilePath
	 */
	public String getXsltFilePath() {
		return xsltFilePath;
	}

	/**
	 * @param xsltFilePath the xsltFilePath to set
	 */
	public void setXsltFilePath(String xsltFilePath) {
		this.xsltFilePath = xsltFilePath;
	}

	@Transactional
	@Override
	public WorkProductList getHAVEMessages(String incidentID) {
	    /*
	     * Get a list of WP by Type from Work Product
	     */
	    try {
	        List<WorkProduct> listOfProducts =
	            getWorkProductService().getProductByTypeAndXQuery(HAVE_PRODUCT_TYPE,
	                null, null);
	        if (listOfProducts != null && listOfProducts.size() > 0) {
	            return createWorkProductList(listOfProducts, incidentID);
	        }
	    } catch (InvalidXpathException e) {
	        log.error("getRequestResources: " + e.getMessage());
	    }
	    return null;
	}

	private WorkProductList createWorkProductList(List<WorkProduct> workProducts, String incidentID) {
        WorkProductList productList = WorkProductList.Factory.newInstance();

        if (workProducts != null && workProducts.size() > 0) {
            for (WorkProduct product : workProducts) {
            	if (incidentID == null ||
            		(product.getFirstAssociatedInterestGroupID() != null &&
            		product.getFirstAssociatedInterestGroupID().equals(incidentID)) ) {
            		productList.addNewWorkProduct().set(WorkProductHelper.toWorkProductSummary(product));
            	}
            }
        }

        return productList;		
	}
	
	@Override
	public WorkProductDocument findHAVEMessage(String distributionReference) {
		WorkProductDocument workProduct = WorkProductDocument.Factory.newInstance();
		workProduct.addNewWorkProduct();
		
		String[] elements = distributionReference.split(",");
		if (elements.length == 3) {
			StringBuffer sb = new StringBuffer();
			String DID = "/de:EDXLDistribution[de:distributionID/text()='";
			// /urn:EdxlDeRequest/de:EDXLDistribution[de:distributionID='DE_DISTRIBUTION_ID']
			sb.append(DID);
			sb.append(elements[0]);
			sb.append("']");
			// and /urn:EdxlDeRequest/de:EDXLDistribution[de:senderID='RMApplication1@core1']
			sb.append("and /de:EDXLDistribution[de:senderID='");
			sb.append(elements[1]);
			sb.append("']");
			// /de:EDXLDistribution[de:dateTimeSent='2010-11-11T13:42:36.890-05:00']
			sb.append("and /de:EDXLDistribution[de:dateTimeSent='");
			sb.append(elements[2]);
			sb.append("']");
			Map<String, String> namespaceMap = new HashMap<String,String>();
			namespaceMap.put("de", "urn:oasis:names:tc:emergency:EDXL:DE:1.0");
			try {
				List<WorkProduct> list = workProductService.getProductByTypeAndXQuery(HAVE_PRODUCT_TYPE, sb.toString(), namespaceMap);
				if (list.size() == 1) {
					WorkProduct wp = list.get(0);
					workProduct.setWorkProduct(WorkProductHelper.toWorkProduct(wp));
				}
			} catch (InvalidXpathException e) {
				log.error("Invalid XPath expression finding HAVE message: " + e.getMessage());
			}
		}
		
		return workProduct;
	}
	
	/**
     * Parse the content of the edxl message, determine the type of request, process the request
     * appropriately and send the message to each core that has a user in an explictAddress
     * 
     * @param edxl the edxl
     * 
     * @return the edxl de response document
     * 
     * @throws IllegalArgumentException the illegal argument exception
     * @throws EmptyCoreNameListException the empty core name list exception
     * @throws SendMessageErrorException the send message error exception
     * @throws LocalCoreNotOnlineException the local core not online exception
     * @ssdd
     */
	@Transactional
    @Override
    public EdxlDeResponseDocument edxldeRequest(EDXLDistribution edxl)
        throws IllegalArgumentException, EmptyCoreNameListException, SendMessageErrorException,
        LocalCoreNotOnlineException {

        EdxlDeResponseDocument response = EdxlDeResponseDocument.Factory.newInstance();
        response.addNewEdxlDeResponse();
        
        // See if this is an update to a current work product.
        WorkProductDocument currentWorkProduct = null;
        if (edxl.sizeOfDistributionReferenceArray() > 0) {
        	for (String dis : edxl.getDistributionReferenceArray()) {
//            	String distributionRef = edxl.getDistributionReferenceArray(0);
            	if (dis != null && !dis.isEmpty()) {
            		currentWorkProduct = findHAVEMessage(dis);
            		if (currentWorkProduct != null && currentWorkProduct.getWorkProduct() != null && currentWorkProduct.getWorkProduct().sizeOfStructuredPayloadArray() > 0) {
            			break;
            		}
            	}        		
        	}
        }

        // Check if we have the content we want else return null
        if (edxl.sizeOfContentObjectArray() > 0
            && edxl.getContentObjectArray(0).getXmlContent() != null
            && edxl.getContentObjectArray(0).getXmlContent().sizeOfEmbeddedXMLContentArray() > 0) {

            // Determine what type of RM message is in the embedded xml content
        	QName qname = getContentsQName(edxl);

            if (qname != null) {
                if (qname == HAVE_RESOURCE_QNAME) {

            		ByteArrayOutputStream baos = new ByteArrayOutputStream();
                	// Create the digest if we have an XSLT 
                	if (xsltSource == null) {
                		response.getEdxlDeResponse().setErrorExists(false);
                		response.getEdxlDeResponse().setErrorString("Cannot find the HAVE Digest XSLT" + xsltFilePath);
                		log.warn("HAVE message not digested because XSLT is missing");
                	}
                	else {
                		baos = createDigest(edxl, baos);
                	}
                    
                    try {
                    	DigestDocument digestDoc = DigestDocument.Factory.newInstance();
                    	if (baos.size() > 0) {
                    		//DigestDocument d = DigestDocument.Factory.parse(digest.getDomNode());
                    		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                    		digestDoc = DigestDocument.Factory.parse(bais);
                        	response.getEdxlDeResponse().addNewDigest().set(digestDoc.getDigest());
                        	response.getEdxlDeResponse().setDigest(digestDoc.getDigest());
                    	}
                    	else {
                    		digestDoc.addNewDigest();
                    	}
                    	
                        ProductPublicationStatus status = publishWorkProduct(edxl, digestDoc.getDigest(), currentWorkProduct);
                        if (status.getStatus() == ProductPublicationStatus.SuccessStatus) {
                        	response.getEdxlDeResponse().setErrorExists(false);
                        }
                        else if (status.getStatus() == ProductPublicationStatus.PendingStatus) {
                        	response.getEdxlDeResponse().setErrorExists(false);
                        	response.getEdxlDeResponse().setErrorString("Product publication is pending");
                        }
                        else {
                        	response.getEdxlDeResponse().setErrorExists(true);
                        	response.getEdxlDeResponse().setErrorString("Product publication status was: " + status.getStatus());
                        }
					} catch (XmlException e) {
						log.error("Failed parsing digest: " + e.getMessage());
					} catch (IOException e) {
						log.error("Failed parsing digest: " + e.getMessage());
					}
                }

                // send the message
                log.debug("Sending HAVE message ");
                sendEdxlDeMessage(edxl);

                return response;
            }  else {
            	log.error("HAVE is missing content from: " + edxl);
        		response.getEdxlDeResponse().setErrorExists(true);
        		response.getEdxlDeResponse().setErrorString("EDXL-HAVE Content Missing");
        		return response;
        	}

        } else {
            return null;
        }
        
    }

	public ByteArrayOutputStream createDigest(EDXLDistribution edxl, ByteArrayOutputStream baos)
	{
		// DigestType digest = DigestType.Factory.newInstance();
		// javax.xml.transform.Result result = new javax.xml.transform.dom.DOMResult(digest.getDomNode());
		javax.xml.transform.Result result = new javax.xml.transform.stream.StreamResult(baos);

		try {
			javax.xml.transform.Source xmlSource = new javax.xml.transform.dom.DOMSource(
					edxl.getDomNode());
			transformer.transform(xmlSource, result);

		} catch (Throwable e) {
			log.error("Transformation failed: " + e.getMessage() + ".");
			e.printStackTrace();
		}
		return baos;
	}

    private ProductPublicationStatus publishWorkProduct(EDXLDistribution edxl, DigestType digest,
    		WorkProductDocument currentWorkProduct) {
    	log.debug("Creating work product for HAVE message");
    	ValidationUtil.validate(edxl, true);
    	ValidationUtil.validate(digest, true);
    	
        // Create a work product
        WorkProduct workProduct = new WorkProduct();
        workProduct.setProductType(HAVE_PRODUCT_TYPE);
        
        // If updating set the work product identification
        if (currentWorkProduct != null && currentWorkProduct.getWorkProduct() != null) {
        	IdentificationType id = WorkProductHelper.getIdentificationElement(currentWorkProduct.getWorkProduct());
        	if (id != null) {
        		try {
        			Integer version = Integer.parseInt(id.getVersion().getStringValue());
        			workProduct.setProductVersion(version);
        			workProduct.setChecksum(id.getChecksum().getStringValue());
        			workProduct.setProductID(id.getIdentifier().getStringValue());
        			workProduct.setProductType(id.getType().getStringValue());
        			workProduct.setActive(id.getState() == StateType.ACTIVE);
        		} catch (NumberFormatException e) {
        			log.error("Error parsing version number to update HAVE message: " + e.getMessage());
        		}
        	}
        	else {
        		log.error("Cannot find WorkProductIdentification in matching HAVE message");
        	}
        }
        
//        workProduct.setProductID(wpIDBuffer.toString());
        EDXLDistributionDocument edxlDoc = EDXLDistributionDocument.Factory.newInstance();
        edxlDoc.addNewEDXLDistribution().set(edxl);
        workProduct.setProduct(edxlDoc.xmlText().getBytes());

        DigestDocument digestDoc = DigestDocument.Factory.newInstance();
        digestDoc.setDigest((DigestType)digest.copy());
        workProduct.setDigest(digestDoc);
        
        if (edxl.sizeOfContentObjectArray() > 0 && edxl.getContentObjectArray(0).getIncidentID() != null) {
            workProduct.getAssociatedInterestGroupIDs().add(
            		edxl.getContentObjectArray(0).getIncidentID());
        }
//        System.out.println(WorkProductHelper.toWorkProductXmlDocument(workProduct));

        // publish the work product
        ProductPublicationStatus status = workProductService.publishProduct(workProduct);

        return status;
    }
    
	public QName getContentsQName(EDXLDistribution edxl) {
		QName qname = null;
		XmlCursor c =
		    edxl.getContentObjectArray(0).getXmlContent().getEmbeddedXMLContentArray(0).newCursor();            
		try {
			if (c.toFirstChild()) {
				qname = c.getObject().schemaType().getOuterType().getDocumentElementName();
			}
		} catch (Exception e) {
			log.error("Error finding EDXL-DE content type: " + e.getMessage());
			log.error("From EDXL-DE: " + edxl.toString());
		}
		finally {
			c.dispose();
		}
		return qname;
	}


    private void sendEdxlDeMessage(EDXLDistribution edxl) throws IllegalArgumentException,
        EmptyCoreNameListException, SendMessageErrorException, LocalCoreNotOnlineException {

        HashSet<String> cores = BroadcastUtil.getCoreList(edxl);

        // Send the message to each core that has a user in an explictAddress
        // element
        if (cores.size() == 0) {
            return;
        } else {
            SendMessageErrorException errorException = new SendMessageErrorException();
            for (String core : cores) {
                try {
                    log.info("sendMessage to: " + core);
                    EDXLDistributionDocument doc = EDXLDistributionDocument.Factory.newInstance();
                    doc.setEDXLDistribution(edxl);
                    communicationsService.sendMessage(doc.xmlText(),
                        CommunicationsService.CORE2CORE_MESSAGE_TYPE.RESOURCE_MESSAGE, core);
                    log.debug("called communicationsService.sendMessage");
                } catch (RemoteCoreUnknownException e1) {
                    errorException.getErrors().put(core,
                        SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.CORE_UNKNOWN);
                } catch (RemoteCoreUnavailableException e2) {
                    errorException.getErrors().put(core,
                        SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.CORE_UNAVAILABLE);
                } catch (LocalCoreNotOnlineException e) {
                    // TODO: this short circuit for the local core should be in the
                    // CommunicationService
                    log.info("Sending to local core");
                    Core2CoreMessage message = new Core2CoreMessage();

                    message.setFromCore(core);
                    message.setToCore(core);
                    message.setMessageType(CommunicationsService.CORE2CORE_MESSAGE_TYPE.RESOURCE_MESSAGE.toString());
                    // Core2CoreMessageDocument doc =
                    // Core2CoreMessageDocument.Factory.newInstance();
                    // doc.addNewCore2CoreMessage().set(edxl);
                    // message.setMessage(doc.toString());
                    EDXLDistributionDocument doc = EDXLDistributionDocument.Factory.newInstance();
                    doc.setEDXLDistribution(edxl);
                    message.setMessage(doc.toString());
                    haveMessageNotificationHandler(message);
                    // communicationsService.core2CoreMessageNotificationHandler(message);
                } catch (NoShareAgreementException e) {
                    errorException.getErrors().put(core,
                        SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.NO_SHARE_AGREEMENT);
                } catch (NoShareRuleInAgreementException e) {
                    errorException.getErrors().put(
                        core,
                        SendMessageErrorException.SEND_MESSAGE_ERROR_TYPE.NO_SHARE_RULE_IN_AGREEMENT);
                }
            }

            if (errorException.getErrors().size() > 0) {
                throw errorException;
            }
        }
    }

    /**
     * HAVE message notification handler.
     * 
     * @param message the message
     * @ssdd
     */
    @Override
    public void haveMessageNotificationHandler(Core2CoreMessage message) {

        log.debug("haveMessageNotificationHandler: received messagefrom "
            + message.getFromCore());
        // =[" + message.getMessage()+ "]

        XmlObject xmlObj;
        try {

            EDXLDistributionDocument edxlDoc =
                EDXLDistributionDocument.Factory.parse(message.getMessage());

            if (edxlDoc.getEDXLDistribution().sizeOfExplicitAddressArray() > 0) {
                // Find core name for each explicit address.
                for (ValueSchemeType type : edxlDoc.getEDXLDistribution().getExplicitAddressArray()) {
                    if (type.getExplicitAddressScheme().equals(
                        CommunicationsService.UICDSExplicitAddressScheme)) {
                        for (String address : type.getExplicitAddressValueArray()) {
                            xmlObj = XmlObject.Factory.parse(edxlDoc.toString());
                            // log.debug("broadcastMessageNotificationHandler: sending notification ["
                            // + xmlObj.toString() + "]  to " + address);
                            sendMessageNotification(xmlObj, address);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            log.error("resourceMessageNotificationHandler: Error parsing message - not a valid XML string");
            throw new IllegalArgumentException("Message is not a valid XML string");
        }
    }

    private void sendMessageNotification(XmlObject xmlObj, String address) {

        ArrayList<NotificationMessageHolderType> messages =
            new ArrayList<NotificationMessageHolderType>();

        NotificationMessageHolderType t = NotificationMessageHolderType.Factory.newInstance();
        NotificationMessageHolderType.Message m = t.addNewMessage();

        try {
            m.set(xmlObj);
            messages.add(t);

            NotificationMessageHolderType[] notification =
                new NotificationMessageHolderType[messages.size()];

            notification = messages.toArray(notification);
            log.debug("===> sending Core2Core message: array size=" + notification.length);
            notificationService.notify(UicdsStringUtil.getSubmitterResourceInstanceName(address),
                notification);
        } catch (Throwable e) {
            log.error("productPublicationStatusNotificationHandler: error creating and sending  Core2Core message  notification to "
                + address);
            e.printStackTrace();
        }
    }

    private HashSet<String> getRecipientCores(EDXLDistribution edxl) {

        HashSet<String> cores = new HashSet<String>();
        if (edxl.sizeOfExplicitAddressArray() > 0) {
            // Find core name for each explicit address.
            for (ValueSchemeType type : edxl.getExplicitAddressArray()) {
                if (type.getExplicitAddressScheme().equals(
                    CommunicationsService.UICDSCoreAddressScheme)) {
                    for (String address : type.getExplicitAddressValueArray()) {
                        cores.add(address);
                    }
                }
            }
        }
        return cores;
    }

}
