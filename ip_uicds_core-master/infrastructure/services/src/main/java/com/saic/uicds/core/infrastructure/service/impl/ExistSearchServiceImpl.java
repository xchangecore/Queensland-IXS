package com.saic.uicds.core.infrastructure.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;

import com.saic.uicds.core.infrastructure.service.ISearchService;
import com.saic.uicds.core.xmldb.XMLDBDAOInterface;
import com.usersmarts.util.DOMUtils;

public class ExistSearchServiceImpl
    implements ISearchService {

    private static final String workProductRoot = "WorkProductList";
    private static final String workProductElement = "WorkProduct";

    Logger log = LoggerFactory.getLogger(getClass());

    private XMLDBDAOInterface workProductDAO;

    public ExistSearchServiceImpl() {

    }

    public ExistSearchServiceImpl(XMLDBDAOInterface workProductDAO) {

        setWorkProductDAO(workProductDAO);
    }

    private String buildDispatchQuery(HashMap<String, String[]> params) {

        StringBuilder dispatchQuery = new StringBuilder();
        dispatchQuery.append("xquery version \"1.0\";\n");
        dispatchQuery.append("import module namespace dispatch = \"http://uicds.org/modules/dispatch\"\n");
        dispatchQuery.append("at \"xmldb:exist:///db/UICDS/modules/dispatch.xq\";\n");
        dispatchQuery.append("let $response := dispatch:dispatch(");
        dispatchQuery.append(mapToXML(params));
        dispatchQuery.append(")\n");
        dispatchQuery.append("return\n");
        dispatchQuery.append("$response");
        return dispatchQuery.toString();
    }

    private Document buildWPDocument(List<Node> workProductNodes) {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage());
            // e.printStackTrace();
        }
        Document document = builder.newDocument();
        Element results = document.createElementNS("http://uicds.org/WorkProductService",
            workProductRoot);
        document.appendChild(results);

        for (Node workProductNode : workProductNodes) {
            List<Element> nodes = DOMUtils.getChildren(workProductNode.getFirstChild(),
                "WorkProduct");
            for (Element elem : nodes) {
                // Element row = document.createElement(workProductElement);
                // Node child = document.importNode(elem, true);
                // row.appendChild(child);
                // results.appendChild(row);
                Node wp = document.importNode(elem, true);
                results.appendChild(wp);
            }
        }
        return document;
    }

    private ResourceSet executeQuery(String dispatchQuery) {

        ResourceSet resultSet = null;

        if (workProductDAO != null) {
            CompiledExpression expression;
            try {
                expression = workProductDAO.getXQueryService().compile(dispatchQuery);
                resultSet = workProductDAO.getXQueryService().execute(expression);

                log.debug("  Result Set");
                log.debug(resultSet.getMembersAsResource().getContent().toString());

            } catch (XMLDBException e) {
                log.error(e.getMessage());
                // e.printStackTrace();
            }
        }

        return resultSet;
    }

    private List<Object> getStringResources(ResourceSet resultSet) {

        List<Object> workProducts = new ArrayList<Object>();
        ResourceIterator i;
        try {
            i = resultSet.getIterator();
            while (i.hasMoreResources()) {
                Resource r = i.nextResource();
                Object content = r.getContent();
                workProducts.add(content);
            }
        } catch (XMLDBException e) {
            log.error(e.getMessage());
            // e.printStackTrace();
        }
        return workProducts;
    }

    public XMLDBDAOInterface getWorkProductDAO() {

        return workProductDAO;
    }

    private List<Node> getXmlResources(ResourceSet resultSet) {

        List<Node> workProducts = new ArrayList<Node>();
        ResourceIterator i;
        try {
            i = resultSet.getIterator();
            while (i.hasMoreResources()) {
                XMLResource resource = (XMLResource) i.nextResource();
                Node node = resource.getContentAsDOM();
                workProducts.add(node);
            }
        } catch (XMLDBException e) {
            log.error(e.getMessage());
            // e.printStackTrace();
        }
        return workProducts;
    }

    private String mapToXML(Map<String, String[]> map) {

        StringBuilder xml = new StringBuilder();

        if (map.containsKey("format")) {
            String[] format = new String[1];
            format[0] = "xml";
            map.put("format", format);
        }
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
    public List<Object> search(HashMap<String, String[]> params) {

        String dispatchQuery = buildDispatchQuery(params);
        ResourceSet resultSet = executeQuery(dispatchQuery);
        List<Object> workProducts = getStringResources(resultSet);
        return workProducts;
    }

    @Override
    public Document searchAsDocument(HashMap<String, String[]> params) {

        String dispatchQuery = buildDispatchQuery(params);
        ResourceSet resultSet = executeQuery(dispatchQuery);
        List<Node> workProductNodes = getXmlResources(resultSet);
        Document document = buildWPDocument(workProductNodes);
        return document;
    }

    public void setWorkProductDAO(XMLDBDAOInterface workProductDAO) {

        this.workProductDAO = workProductDAO;
    }

}
