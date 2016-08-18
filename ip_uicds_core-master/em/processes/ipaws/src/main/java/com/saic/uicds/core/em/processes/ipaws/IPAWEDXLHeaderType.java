
package com.saic.uicds.core.em.processes.ipaws;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import javax.xml.namespace.QName;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

/**
 * class IPAWSHeaderType
 * is a Soap message client interceptor used by the spring framework to 
 * intercept the request and response soap messages.  It adds the required
 * header type for the IPAWS messages.
 */
class IPAWSEDXLHeaderType implements ClientInterceptor
{
    private static Logger log = LoggerFactory.getLogger(IPAWSEDXLHeaderType.class);

    public boolean handleFault(MessageContext context)
    {
        return true;
    }

    public boolean handleRequest(MessageContext context)
    {
        SaajSoapMessage message = (SaajSoapMessage) context.getRequest();
        SOAPMessage soapMessage = message.getSaajMessage();
        try {
            IPAWSEDXLHeaderType.addEDXLHeaderTypeDef(soapMessage);
        }
        catch(SOAPException e)
        {
            log.info("Unable to add EDXLHeaderTypeDef to SOAPHeader");
            return false;
        }

        try {
            // this looks like a hack but IPAWS will not recognize the signature without it
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            context.getRequest().writeTo(os);
            os.close();
        }
        catch(IOException e)
        {
            log.error("Unable to write MessageContext to output stream");
        }
            
        return true;
    }

    /**
     * method addCAPHeaderTypeDef
     * adds the CAPHeaderTypeDef element to the SOAP message header.
     * @parameter SOAPMessage soapMessage the soap message to which the element is added.
     */
    private static void addEDXLHeaderTypeDef(SOAPMessage soapMessage)
        throws  SOAPException
    {
        SOAPHeader header = soapMessage.getSOAPHeader();
        QName name = new QName("http://gov.fema.dmopen.services/DMOPEN_EDXLDEService/", "EdxlHeaderTypeDef", "dmop");
        SOAPHeaderElement headerElement = header.addHeaderElement(name);
        name = new QName("http://gov.fema.dmopen.services/DMOPEN_EDXLDEService/", "logonUser", "dmop");
        SOAPElement logonUserElement = headerElement.addChildElement(name);
        logonUserElement.addTextNode("dmopentester");
        name = new QName("http://gov.fema.dmopen.services/DMOPEN_EDXLDEService/", "logonCogId", "dmop");
        SOAPElement logonCogIdElement = headerElement.addChildElement(name);
        logonCogIdElement.addTextNode("999");
    }


    public boolean handleResponse(MessageContext context)
    {
        return true;
    }

}
