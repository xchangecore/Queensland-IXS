package com.saic.uicds.core.infrastructure.controller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Sequence;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;

import com.saic.uicds.core.xmldb.XMLDBDAOInterface;

/**
 * The Search Service provides UICDS clients with services to discover and access work products
 * using OpenSearch enabled feeds.
 *
 * The Search Service accepts queries in the following format:
 *
 * <pre>
 *     ?productType=Incident&updatedBy=bob@core1
 * </pre>
 *
 * The table follow describes the supported query terms.
 * <table>
 * <thead>
 * <tr>
 * <td width="20%"><b>Term</b></td>
 * <td><b>Description</b></td></thead> <tbody>
 * <tr>
 * <td width="20%">productID</td>
 * <td>The identifier of a Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">productType</td>
 * <td>The type of a Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">productVersion</td>
 * <td>The version of a Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">productState</td>
 * <td>The Work Product status (Open, Closed, Archive)</td>
 * </tr>
 * <tr>
 * <td width="20%">createdBegin</td>
 * <td>The beginning date and time of a range in which the Work Product was created</td>
 * </tr>
 * <tr>
 * <td width="20%">createdEnd</td>
 * <td>The ending date and time of a range in which the Work Product was created</td>
 * </tr>
 * <tr>
 * <td width="20%">createdBy</td>
 * <td>The name of the user that created the Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">updatedBegin</td>
 * <td>The beginning date and time of a range in which the Work Product was last updated</td>
 * </tr>
 * <tr>
 * <td width="20%">updatedEnd</td>
 * <td>The ending date and time of a range in which the Work Product was last updated</td>
 * </tr>
 * <tr>
 * <td width="20%">updatedBy</td>
 * <td>The name of the user that last updated the Work Product</td>
 * </tr>
 * <tr>
 * <tr>
 * <td width="20%">bbox</td>
 * <td>A geospatial bounding box ( bbox=minX,minY,maxX,maxY ) intersecting or containing the Work
 * Product (based on the Digest)</td>
 * </tr>
 * <tr>
 * <td width="20%">what</td>
 * <td>The type of any "Thing" referenced within the Work Product metadata</td>
 * </tr>
 * <tr>
 * <td width="20%">mimetype</td>
 * <td>The mimetype of the Work Product payload</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * The format of the results is controlled by the format parameter. For instance:
 *
 * <pre>
 *     ?productType=Incident&updatedBy=bob@core1&format=rss
 * </pre>
 *
 * Currently supported formats:
 * <table>
 * <thead>
 * <tr>
 * <td width="20%"><b>Format</b></td>
 * <td><b>Description</b></td>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td width="20%">kml</td>
 * <td>KML 2.2</td>
 * </tr>
 * <tr>
 * <td width="20%">w3crss</td>
 * <td>RSS 2.0 with W3C Basic Geo extensions</td>
 * </tr>
 * <tr>
 * <td width="20%">rss</td>
 * <td>RSS 2.0 with GeoRSS GML extensions</td>
 * </tr> 
 * <tr>
 * <td width="20%">xml</td>
 * <td>UICDS Work Product Format (UCore 2.0 DataItemPackage)</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/1.1">OpenSearch 1.1
 *      Specification</a>
 * @see <a href="http://cyber.law.harvard.edu/rss/rss.html">RSS 2.0 Specification</a>
 * @see <a href="http://www.georss.org/">GeoRSS Specification</a>
 * @see <a href="http://www.georss.org/W3C_Basic">W3C Basic Geo Vocabulary</a>
 * @author William Summers
 * @idd
 */

public class XMLDBQueryController extends AbstractController {

    private MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();
    private XMLDBDAOInterface xmldbDAO;

    private String mapToXML(Map<String, String[]> map) {

        StringBuilder xml = new StringBuilder();

        xml.append("<props xmlns=\"util:properties\">");
        for (String key : map.keySet()) {
            for (String value : map.get(key)) {
                xml.append("<prop name=\"" + key + "\" value=\"" + value + "\"/>");
            }
        }
        xml.append("</props>");
        return xml.toString();
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request,
        HttpServletResponse response) throws Exception {

    	// a copy must be made to avoid synchronization issues
    	// TODO: is there a better way to make a parameterized copy?
        HashMap<String, String[]> params = new HashMap<String, String[]>(request.getParameterMap());
                
        // add some additional request information
        String[] resourcePath = {StringEscapeUtils.escapeXml(request.getPathInfo())};
        String[] remoteUser = {StringEscapeUtils.escapeXml(request.getRemoteUser())};
        String[] queryString = {StringEscapeUtils.escapeXml(request.getQueryString())};
        params.put("req.resourcePath", resourcePath);
        params.put("req.queryString", queryString);
        params.put("req.remoteUser", remoteUser);
        
        StringBuilder dispatchQuery = new StringBuilder();
        dispatchQuery.append("xquery version \"1.0\";\n");
        dispatchQuery.append("import module namespace dispatch = \"http://uicds.org/modules/dispatch\"\n");
        dispatchQuery.append("at \"xmldb:exist:///db/UICDS/modules/dispatch.xq\";\n");
        dispatchQuery.append("let $response := dispatch:dispatch(");        
        dispatchQuery.append(mapToXML(params));
        dispatchQuery.append(")\n");
        dispatchQuery.append("return\n");
        dispatchQuery.append("$response");

        CompiledExpression expression = xmldbDAO.getXQueryService().compile(dispatchQuery.toString());

        // set the return type based on the format parameter
        String format = request.getParameter("format");
        if (StringUtils.isBlank(format) ){
            response.setContentType("application/octet-stream");
        } else if (format.equals("debug")) {
        	response.setContentType("application/xml");
        } else {
        	// set the type 
        	// if no matching type is found application/octet-stream is used
            response.setContentType(mimeTypes.getContentType("response." + format));
        }

        if (xmldbDAO != null) {

            ResourceSet results = xmldbDAO.getXQueryService().execute(expression);
            ResourceIterator i = results.getIterator();
            while (i.hasMoreResources()) {
                Resource r = i.nextResource();
                
                response.getOutputStream().write(((String) r.getContent()).getBytes());
            }
            response.getOutputStream().close();

        } else {
            String tmp = "<html><h3>Search Controller XMLDBDAO is null.</h3></html>";
            response.getOutputStream().write(tmp.getBytes());
            response.getOutputStream().close();
        }

        // return null to indicate that the response stream was handled
        // internally without the use of a view.
        return null;

    }

    public XMLDBDAOInterface getXmldbDAO() {
        return xmldbDAO;
    }

    public void setXmldbDAO(XMLDBDAOInterface xmldbDAO) {
        this.xmldbDAO = xmldbDAO;
    }

    
}
