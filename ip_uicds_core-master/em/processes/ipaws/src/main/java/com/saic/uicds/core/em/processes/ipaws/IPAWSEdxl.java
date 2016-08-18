package com.saic.uicds.core.em.processes.ipaws;

import java.util.List;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.text.SimpleDateFormat;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import services.dmopen.fema.gov.dmopenEDXLDEService.GetRequestTypeDefDocument;
import services.dmopen.fema.gov.edxlrequest.RequestParameterList;
import services.dmopen.fema.gov.edxlrequest.ParameterListItem;

import services.dmopen.fema.gov.dmopenEDXLDEService.GetMessageTypeDefDocument;
import services.dmopen.fema.gov.dmopenEDXLDEService.PostEdxlRequestTypeDefDocument;
import services.dmopen.fema.gov.dmopenEDXLDEService.PostEdxlRequestTypeDefDocument.PostEdxlRequestTypeDef;


import com.saic.uicds.core.infrastructure.model.WorkProduct;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument;
import x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution;


/**
 * Class IPAWSEdxl connects to IPAWS server and retrieves EDXL messages.
 * It uses the spring framework to fires the method getMessages at 
 * intervals. The spring bean config file is in services/contexts/applicationContext-processes.xml.
 * Spring calls startGettingMessages() at the beginning and then call getMessages()
 * at intervals.
 * It uses the IPAWSSpringClient class to send SOAP messages to IPAWS.
 */
public class IPAWSEdxl
{

    private Logger log = LoggerFactory.getLogger(this.getClass());
    IPAWSSpringClient webServiceClient;
    IPAWSEdxlBroadcast ipawsBroadcast;
    IPAWSCap ipawsCap;

    Calendar lastUpdate = null;
    String cogs = "999";
    String postToCogs = "999";
    String firstRetrievePeriod = "30";

    HashSet<String> uicdsSet = new HashSet<String>();
    HashSet<String> ipawsSet = new HashSet<String>();
    ArrayList<Cog> cogList = new ArrayList<Cog>();

    public void setWebServiceClient(IPAWSSpringClient webServiceClient)
    {
        this.webServiceClient = webServiceClient;
    }

    public IPAWSSpringClient getWebServiceClient()
    {
        return this.webServiceClient;
    }

    public IPAWSCap getIpawsCap()
    {
        return this.ipawsCap;
    }

    public void setIpawsCap(IPAWSCap ipaws)
    {
        this.ipawsCap = ipaws;
    }

    public void setIpawsBroadcast(IPAWSEdxlBroadcast broadcast)
    {
        this.ipawsBroadcast = broadcast;
    }

    public IPAWSEdxlBroadcast getIpawsBroadcast()
    {
        return this.ipawsBroadcast;
    }

    public String getCogs()
    {
        return this.cogs;
    }

    public void setCogs(String cogs)
    {
        this.cogs = cogs;
    }

    public String getPostToCogs()
    {
        return this.postToCogs;
    }

    public void setPostToCogs(String cogs)
    {
        this.postToCogs = cogs;
    }

    public String getFirstRetrievePeriod()
    {
        return this.firstRetrievePeriod;
    }

    public void setFirstRetrievePeriod(String days)
    {
        this.firstRetrievePeriod = days;
    }

    /**
     * method getEdxlMessages
     * is fired by the spring framework at intervals that is set in the
     * file applicationContext-processes.xml.
     */
    public void getEdxlMessages()
    {
        boolean ret = getEDXLACK();
        if (ret) {

            // get the broadcast messages from uicds and send then to ipaws
            getBroadcastMessagesForCogs();

            EDXLDistribution[] edxls = null;
            // get the edxl-de messages from ipaws
            if (this.lastUpdate == null) {
                startGettingMessages();
            }
            else {

                edxls = getEdxlMessagesAfterTime(this.lastUpdate);
                // EDXLDistribution[] edxls = getEdxlMessagesAfterTime(lastUpdate);
                if (edxls != null) {
                    log.info("Retrieved " + edxls.length + 
                             " EDXL distribution messages from IPAWS sent after " +
                             this.lastUpdate);

                    // update the time
                    Calendar latest = this.lastUpdate;
                    for (EDXLDistribution edxl:edxls) {
                        Calendar cal = edxl.getDateTimeSent();
                        if (cal.after(latest)) {
                            latest = cal;
                        }
                    }
                    // update the lastUpdate sent time 
                    this.lastUpdate = latest;
                }

                // broadcast to uicds
                processEDXL(edxls);

            }
        }
    }

    /**
     * method startGettingMessages
     * is fired by the spring framework at for the first time
     * file applicationContext-processes.xml.  It retrieves EDXL messages for 
     * a period set in the context xml. timeToRetrieve is set in days.  If this
     * period cannot be determined, then all EDXL messages for the cog 
     * are retrieved.  
     */
    public void startGettingMessages()
    {

        setupCogs();

        boolean ret = getEDXLACK();
        if (ret) {

            // get the broadcast messages in uicds and send them to ipaws
            getBroadcastMessagesForCogs();
            
            // get the messages from ipaws
            EDXLDistribution[] edxls = null;
            try {
                // IPAWS tries to parse the firstRetrievePeriod.  To start, 
                // IPAWS retrieve the messages created during this period.
                // if it cannot parse this then it gets all messages in IPAWS.
                // setting the last update to current - firstRetrievePeriod
                long period = Long.parseLong(this.firstRetrievePeriod);
                lastUpdate = Calendar.getInstance();
                long timeToRetrieve = 
                    this.lastUpdate.getTimeInMillis() - period *24L*3600000L;
                this.lastUpdate.setTimeInMillis(timeToRetrieve);

                SimpleDateFormat df = 
                    new SimpleDateFormat(IPAWSCap.DATE_FORMAT);
                log.info(
                    "Retrieving EDXL distribution message from IPAWS since " + 
                    IPAWSCap.formatCalendarStringToRfc3339(
                        df.format(this.lastUpdate.getTime())));

                // retrieve the edxl message from IPAWS
                edxls = getEdxlMessagesAfterTime(this.lastUpdate);
            }
            catch(NumberFormatException e)
            {
                if (this.firstRetrievePeriod.equals("ALL")) {
                    log.info("Retrieving all messages from IPAWS");
                }
                else {
                    log.error("Unable to parse firstRetrievePeriod.  Retrieving all messages from IPAWS");
                }

                // if cannot get the messages in the period, then get all edxl messages
                edxls = getEdxlMessagesByCogID(this.cogs);
            }

            // synchronize the alerts between ipaws and uicds
            if (edxls != null) {
                log.info("Retrieved " + edxls.length + 
                         " original EDXL distribution messages from IPAWS");

                // update the time
                Calendar latest = edxls[0].getDateTimeSent();
                for (EDXLDistribution edxl:edxls) {
                    Calendar cal = edxl.getDateTimeSent();
                    if (cal.after(latest)) {
                        latest = cal;
                    }
                }

                this.lastUpdate = latest;
            }

            // broadcast to uicds
            processEDXL(edxls);
        }
        
    }


    /**
     * method getEDXLACK
     * get the aknowledgement from IPAWS
     * mainly to test the security credentials and the availability of the 
     * IPAWS server.
     */
    public boolean getEDXLACK()
    {
        // create the request message
        GetRequestTypeDefDocument request = 
            GetRequestTypeDefDocument.Factory.newInstance();

        RequestParameterList reqParamList = request.addNewGetRequestTypeDef();
        reqParamList.setRequestAPI(IPAWSCap.REQUEST_API);
        reqParamList.setRequestOperation(IPAWSCap.GET_ACK_OP);

        ParameterListItem listItem = reqParamList.addNewParameters();
        listItem.setParameterName("ping");

        log.info(request.toString());
        
        // send the request and get the response
        XmlObject response = this.webServiceClient.sendRequest(request);
        log.debug(response.toString());

        return IPAWSEdxlServiceResponse.getAckFromResponse(response);

    }

    /**
     * method getCogList
     * retrieves the list of cogname, cogid from IPAWS.
     * @return the List of IPAWSCog.
     */
    public List<IPAWSCog> getCogList()
    {
        // create the request message
        GetRequestTypeDefDocument request = 
            GetRequestTypeDefDocument.Factory.newInstance();

        RequestParameterList paramList = request.addNewGetRequestTypeDef();
        paramList.setRequestAPI(IPAWSCap.REQUEST_API);
        paramList.setRequestOperation(IPAWSCap.GET_COG_OP);

        ParameterListItem listItem = paramList.addNewParameters();
        listItem.setParameterName("ALL");

        log.debug(request.toString());
        
        // send the request and get the response
        XmlObject response = this.webServiceClient.sendRequest(request);
        
        // log.debug(response.toString());

        return IPAWSEdxlServiceResponse.getCogListFromResponse(response);
    }

    /**
     * method getEdxlMessagesByCogID
     * retrieve the list of EDXL message for the cog
     * @param String cogId: the id of the COG
     * @return EDXLDistribution[] an array of
     * x0.oasisNamesTcEmergencyEDXLDE1.EDXLDistributionDocument.EDXLDistribution.
     */
    public EDXLDistribution[] getEdxlMessagesByCogID(String cogId)
    {
        // create the request message
        GetMessageTypeDefDocument requestDoc = 
            GetMessageTypeDefDocument.Factory.newInstance();

        RequestParameterList paramList = requestDoc.addNewGetMessageTypeDef();
        paramList.setRequestAPI(IPAWSCap.REQUEST_API);
        paramList.setRequestOperation(IPAWSCap.GET_MESSAGE_OP);


        ParameterListItem listItem = paramList.addNewParameters();
        listItem.setParameterName("cogid");
        listItem.setComparisonOp("equalto");
        listItem.addParameterValue(cogId);

        log.info(requestDoc.toString());
        
        // send the request and get the response
        XmlObject response = this.webServiceClient.sendRequest(requestDoc);
        log.debug(response.toString());

        EDXLDistribution[] edxls = 
            IPAWSEdxlServiceResponse.getEdxlMessagesFromResponse(response);

        // if no message is found, return null
        if (edxls == null || 
            edxls[0].getDistributionID().equals("NO MESSAGE FOUND")) {
            return null;
        }

        return edxls;
    }

    
    public EDXLDistribution[] getEdxlMessagesAfterTime(Calendar cal)
    {
        // create the request message
        GetMessageTypeDefDocument requestDoc = 
            GetMessageTypeDefDocument.Factory.newInstance();

        RequestParameterList paramList = requestDoc.addNewGetMessageTypeDef();
        paramList.setRequestAPI(IPAWSCap.REQUEST_API);
        paramList.setRequestOperation(IPAWSCap.GET_MESSAGE_OP);


        ParameterListItem listItem = paramList.addNewParameters();
        listItem.setParameterName("dateTimeSent");
        listItem.setComparisonOp("greaterthan");

        // now change the date time format to yyyy-MM-dd'T'HH:mm:ss[+/-]HH:mm
        SimpleDateFormat df = new SimpleDateFormat(IPAWSCap.DATE_FORMAT);
        String dateStr = df.format(cal.getTime());
        listItem.addParameterValue(
            IPAWSCap.formatCalendarStringToRfc3339(dateStr));

        log.info(requestDoc.toString());

        // send the request and get the response
        XmlObject response = this.webServiceClient.sendRequest(requestDoc);
        //log.info(response.toString());

        EDXLDistribution[] edxls = 
            IPAWSEdxlServiceResponse.getEdxlMessagesFromResponse(response);

        // if no message is found, return null
        if (edxls == null || 
            edxls[0].getDistributionID().equals(IPAWSCap.NO_MESSAGE_FOUND)) {
            return null;
        }
        
        return edxls;
    }


    /**
     * method processEDXL
     * find which type of content is in the EDXL and depending on the type
     * broadcast the EDXL message accordingly.
     * @parameter EDXLDistribution[] edxls array of EDXL-DE messages
     */
    private void processEDXL(EDXLDistribution[] edxls)
    {
        if (edxls != null) {
            for (EDXLDistribution edxl:edxls) {

                // check if the message originated from uicds
                if (this.uicdsSet.contains(edxl.getDistributionID())) {
                    this.uicdsSet.remove(edxl.getDistributionID());
                    continue;
                }
                int type = IPAWSEdxlBroadcast.findEDXLType(edxl);
                switch(type) {
                    case IPAWSEdxlBroadcast.RM_TYPE:
                        log.info("Processing RM request");
                        this.ipawsBroadcast.broadcastRm(edxl);
                        this.ipawsSet.add(edxl.getDistributionID());
                        break;

                    case IPAWSEdxlBroadcast.ALERT_TYPE:
                        log.info("Processing CAP Alert request");
                        this.ipawsCap.createAlertFromEdxl(edxl);
                        
                        break;


                    case IPAWSEdxlBroadcast.HAVE_TYPE:
                        log.info("Processing HAVE request");
                        this.ipawsBroadcast.broadcastDe(edxl);
                        this.ipawsSet.add(edxl.getDistributionID());
                        break;

                    case IPAWSEdxlBroadcast.UICDS_TYPE:
                        log.info("Processing UICDS WP request");
                        log.info("Currently UICDS WP is not supported");
                        // this is deferred to later releases
                        // ipawsBroadcast.broadcastDe(edxl);
                        // ipawsSet.add(edxl.getDistributionID());
                        break;

                    default:
                        log.info("Unable to process document in edxl-de: UNKOWN_TYPE");
                }
            }
        }
    }

    /**
     * method postEDXL
     * post a edxl-de message to ipaws
     * @paramter EDXLDistribution edxl the exdl-de to send to ipaws
     * @return true if successfull false if not
     */
    public boolean postEDXL(EDXLDistribution edxl)
    {
        log.info("posting edxl " + edxl.getDistributionID() + " to IPAWS");
        PostEdxlRequestTypeDefDocument postRequestDoc =
            PostEdxlRequestTypeDefDocument.Factory.newInstance();
        PostEdxlRequestTypeDef postRequest = 
            postRequestDoc.addNewPostEdxlRequestTypeDef();

        // the original address was for uicds, change it to ipaws
        // change the explicit request to destCogs
        // remove the explicit address and add the address values for the cogs
        for (int i=0; i<edxl.sizeOfExplicitAddressArray(); i++) {
            edxl.removeExplicitAddress(i);
        }

        x0.oasisNamesTcEmergencyEDXLDE1.ValueSchemeType address = 
            edxl.addNewExplicitAddress();
        address.setExplicitAddressScheme("dmcog");

        for (Cog cog:cogList) {
            address.addExplicitAddressValue(cog.cogId);
        }

        postRequest.setEDXLDistribution(edxl);

        log.debug(postRequestDoc.toString());
        
        XmlObject response = 
            this.webServiceClient.sendRequest(postRequestDoc);
        log.info(response.toString());

        return IPAWSEdxlServiceResponse.getPostEdxlResultFromResponse(response); 
    }


    /**
     * method setupCogs()
     * parses the destination Cog string and creates the Cog objects
     * to save the cogid and the endpoint addresses, and get the messages
     * for these cogs
     *
     */
    private void setupCogs()
    {
        // create the resource instance for the destination cogs
        String [] destCogs = this.postToCogs.split("\\s");
        for (String cogId:destCogs) {
            String  address = 
                this.ipawsBroadcast.CreateResourceInstanceForCog(cogId);
            Cog cog = new Cog(cogId, address);
            cogList.add(cog);
        }
    }

    /**
     * method getMessagesForCogs()
     * retrieves message notifications for cogs registered
     */
    private void getBroadcastMessagesForCogs()
    {
        for (Cog cog:cogList) {
            XmlObject[] xmlDocs = this.ipawsBroadcast.getMessages(cog.cogId);
            if (xmlDocs != null) {
                for (XmlObject doc:xmlDocs) {
                    try {
                        XmlCursor c = doc.newCursor();
                        c.toFirstChild();

                        // check if the message originated from ipaws
                        EDXLDistribution edxlde = (EDXLDistribution) c.getObject();

                        // if it does then ignore it
                        if (ipawsSet.contains(edxlde.getDistributionID())) {
                            ipawsSet.remove(edxlde.getDistributionID());
                            continue;
                        }

                        // if the payload is an alert, create an alert wp
                        if (IPAWSEdxlBroadcast.findEDXLType(edxlde) == 
                            this.ipawsBroadcast.ALERT_TYPE) {

                            this.ipawsCap.createAlertFromEdxl(edxlde);
                        }
                        // send to ipaws
                        postEDXL(edxlde);
                        this.uicdsSet.add(edxlde.getDistributionID());
                    }
                    catch(ClassCastException e)
                    {
                        log.info("the message is not an edxl message.");
                    }
                }
            }
        }
    }

    /**
     * inner class Cog
     * to hold cog information
     */
    private class Cog
    {
        public String cogId;
        public String address;

        Cog(String cogId, String address)
        {
            this.cogId = cogId;
            this.address = address;
        }
    }

}



