package com.saic.uicds.core.em.have.endpoint;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.uicds.haveService.EdxlDeMessageErrorType;
import org.uicds.haveService.EdxlDeRequestDocument;
import org.uicds.haveService.EdxlDeResponseDocument;
import org.uicds.haveService.GetHAVEMessagesRequestDocument;
import org.uicds.haveService.GetHAVEMessagesResponseDocument;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;

import com.saic.uicds.core.em.exceptions.SendMessageErrorException;
import com.saic.uicds.core.em.have.service.HAVEService;
import com.saic.uicds.core.infrastructure.exceptions.EmptyCoreNameListException;
import com.saic.uicds.core.infrastructure.exceptions.LocalCoreNotOnlineException;
import com.saic.uicds.core.infrastructure.util.ServiceNamespaces;



@Endpoint
public class HaveServiceEndpoint implements ServiceNamespaces {

    private static Logger log = LoggerFactory.getLogger(HaveServiceEndpoint.class);

    @Autowired
    private HAVEService haveService;

    /**
	 * @return the haveService
	 */
	public HAVEService getHaveService() {
		return haveService;
	}

	/**
	 * @param haveService the haveService to set
	 */
	public void setHaveService(HAVEService haveService) {
		this.haveService = haveService;
	}

	/**
     * Allows the client to submit an EDXL-RM document wrapped in EDXL-DE
     * 
     * @param EdxlDeRequestDocument
     * 
     * @return WorkProductIdResponseDocument
     * @see <a href="services/ResourceManagement/0.1/ResourceManagementService.xsd">Appendix:
     *      ResourceManagementService.xsd</a>
     * @see <a href="services/ResourceManagement/0.1/ResourceManagementServiceData.xsd">Appendix:
     *      ResourceManagementServiceData.xsd</a>
     * @see <a href="http://docs.oasis-open.org/emergency/edxl-de/v1.0/EDXL-DE_Spec_v1.0.pdf">OASIS
     *      EDXL-DE Specification</a>
     * @see <a href="http://docs.oasis-open.org/emergency/edxl-rm/v1.0/EDXL-RM-SPEC-V1.0.pdf">OASIS
     *      EDXL-RM Specification</a>
     * @idd
     */
    // TODO: translate exceptions into the response
    @PayloadRoot(namespace = "http://uicds.org/HAVEService", localPart = "EdxlDeRequest")
    public EdxlDeResponseDocument edxldeRequest(EdxlDeRequestDocument requestDoc) {
    	log.debug("Received reqeust:" + requestDoc.xmlText());
        EDXLDistribution request = requestDoc.getEdxlDeRequest().getEDXLDistribution();
        
        
        EdxlDeResponseDocument response = EdxlDeResponseDocument.Factory.newInstance();
        response.addNewEdxlDeResponse();
        
        try {
            response = haveService.edxldeRequest(request);
        } catch (IllegalArgumentException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString(e.getMessage());
        } catch (EmptyCoreNameListException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString("Empty Explicit Address List");
        } catch (SendMessageErrorException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString(
                "Failure to send message to one or more cores");
            Set<String> coresWithError = e.getErrors().keySet();
            for (String core : coresWithError) {
                EdxlDeMessageErrorType error = response.getEdxlDeResponse().addNewCoreError();
                error.setCoreName(core);
                error.setError(e.getErrors().get(core).toString());
            }
        } catch (LocalCoreNotOnlineException e) {
            response.getEdxlDeResponse().setErrorExists(true);
            response.getEdxlDeResponse().setErrorString("Local Core is not online");
        } catch (Throwable e) {
        	log.error(e.getMessage());
        }

        log.info(response.toString());
        return response;
    }

    @PayloadRoot(namespace = "http://uicds.org/HAVEService", localPart = "GetHAVEMessagesRequest")
    public GetHAVEMessagesResponseDocument getHaveMessages(GetHAVEMessagesRequestDocument request) {
    	GetHAVEMessagesResponseDocument response = GetHAVEMessagesResponseDocument.Factory.newInstance();
    	response.addNewGetHAVEMessagesResponse();
    	
    	WorkProductList workProducts = haveService.getHAVEMessages(request.getGetHAVEMessagesRequest().getIncidentID());
    	
    	response.getGetHAVEMessagesResponse().addNewWorkProductList().set(workProducts);
    	
		return response;
    }
}
