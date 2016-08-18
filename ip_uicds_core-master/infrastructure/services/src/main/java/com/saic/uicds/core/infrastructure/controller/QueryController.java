package com.saic.uicds.core.infrastructure.controller;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.w3c.dom.Document;

import com.saic.uicds.core.infrastructure.service.ISearchService;
import com.saic.uicds.core.infrastructure.util.NodeRenderer;
import com.saic.uicds.core.infrastructure.util.WorkProductView;

/**
 * The Search Service provides UICDS clients with services to discover and
 * access work products using OpenSearch enabled feeds.
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
 * <td>The beginning date and time of a range in which the Work Product was
 * created</td>
 * </tr>
 * <tr>
 * <td width="20%">createdEnd</td>
 * <td>The ending date and time of a range in which the Work Product was created
 * </td>
 * </tr>
 * <tr>
 * <td width="20%">createdBy</td>
 * <td>The name of the user that created the Work Product</td>
 * </tr>
 * <tr>
 * <td width="20%">updatedBegin</td>
 * <td>The beginning date and time of a range in which the Work Product was last
 * updated</td>
 * </tr>
 * <tr>
 * <td width="20%">updatedEnd</td>
 * <td>The ending date and time of a range in which the Work Product was last
 * updated</td>
 * </tr>
 * <tr>
 * <td width="20%">updatedBy</td>
 * <td>The name of the user that last updated the Work Product</td>
 * </tr>
 * <tr>
 * <tr>
 * <td width="20%">bbox</td>
 * <td>A geospatial bounding box ( bbox=minX,minY,maxX,maxY ) intersecting or
 * containing the Work Product (based on the Digest)</td>
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
 * The format of the results is controlled by the format parameter. For
 * instance:
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
 * @see <a
 *      href="http://www.opensearch.org/Specifications/OpenSearch/1.1">OpenSearch
 *      1.1 Specification</a>
 * @see <a href="http://cyber.law.harvard.edu/rss/rss.html">RSS 2.0
 *      Specification</a>
 * @see <a href="http://www.georss.org/">GeoRSS Specification</a>
 * @see <a href="http://www.georss.org/W3C_Basic">W3C Basic Geo Vocabulary</a>
 * @author William Summers
 * @idd
 */

public class QueryController extends AbstractController {

	private ISearchService service;

	private String serviceName;

	private NodeRenderer nodeRenderer;

	public NodeRenderer getNodeRenderer() {
		return nodeRenderer;
	}

	public void setNodeRenderer(NodeRenderer nodeRenderer) {
		this.nodeRenderer = nodeRenderer;
	}

	Logger log = LoggerFactory.getLogger(getClass());

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		// a copy must be made to avoid synchronization issues
		HashMap<String, String[]> params = new HashMap<String, String[]>(
				request.getParameterMap());
		if (request.getPathInfo() != null) {
			String[] resourcePath = { StringEscapeUtils.escapeXml(request
					.getPathInfo()) };
			params.put("req.resourcePath", resourcePath);
		}
		if (request.getRemoteUser() != null) {
			String[] remoteUser = { StringEscapeUtils.escapeXml(request
					.getRemoteUser()) };
			params.put("req.remoteUser", remoteUser);
		}
		if (request.getQueryString() != null) {
			String[] queryString = { StringEscapeUtils.escapeXml(request
					.getQueryString()) };
			params.put("req.queryString", queryString);
		}

		Document workProductDoc = getService().searchAsDocument(params);

		ModelAndView mav = new ModelAndView(new WorkProductView());
		mav.getModel().put("output", workProductDoc);
		mav.getModel().put("renderer", getNodeRenderer());
		mav.getModel().put("propertiesMap", request.getParameterMap());
		mav.getModel().put("format", request.getParameter("format"));

		return mav;
	}

	public ISearchService getService() {
		return service;
	}

	public void setService(ISearchService service) {
		this.service = service;
	}

}
