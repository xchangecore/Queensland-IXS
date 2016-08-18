package com.saic.uicds.core.em.adminconsole.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowCloseListener;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.saic.uicds.core.em.adminconsole.client.healthstatus.HealthStatus;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;
import com.saic.uicds.core.em.adminconsole.client.view.ExplorerTree;
import com.saic.uicds.core.em.adminconsole.client.view.Util;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Console implements EntryPoint, UICDSConstants {

    private DecoratedTabPanel rootTabPanel = new DecoratedTabPanel();
    private final HorizontalSplitPanel uicdsServicePanel = new HorizontalSplitPanel();
    private VerticalPanel healthstatusPanel = new VerticalPanel();
    private HealthStatus healthStatusDock = new HealthStatus();
    private Tree tree;
    private VerticalPanel formPanel = new VerticalPanel();
    public final TextArea xmlDocument = new TextArea();
    private final HTML htmlDocument = new HTML();
    private ListBox xsltList = new ListBox();
    HorizontalPanel xsltPanel;
    Button moreInfo = new Button("More Information");

    TabBar rootTabBar;
    NodeList<Node> rootTabNodes;
    Element uicdsTabElem;
    Element hnsTabElem;

    TabPanel contentsTabPanel;
    TabBar contentsTabBar;
    NodeList<Node> contentsTabNodes;
    Element formTabElem;
    Element xmlTabElem;

    private void createDockPanel() {
        healthstatusPanel.setSpacing(10);
        healthstatusPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        healthstatusPanel.setSize("100%", "100%");
        healthstatusPanel.add(healthStatusDock);
    }

    private Widget createServiceElementsTab() {
        contentsTabPanel = new DecoratedTabPanel();
        xmlDocument.setReadOnly(true);
        xmlDocument.setStylePrimaryName("gwt-TextArea-readonly");
        xmlDocument.setTitle(XmlView);
        Util.setBlankXmlDocument(xmlDocument);

        xsltPanel = new HorizontalPanel();
        xsltPanel.setVisible(false);
        xsltList.setEnabled(false);
        xsltList.setStyleName("listBorder");
        xsltList.setTitle("XSLT List for products");

        Label xsltLabel = new Label("Select XSLT:");
        xsltPanel.add(xsltLabel);
        xsltPanel.add(xsltList);
        xsltPanel.setCellVerticalAlignment(xsltLabel, VerticalPanel.ALIGN_MIDDLE);
        xsltPanel.setCellVerticalAlignment(xsltList, VerticalPanel.ALIGN_MIDDLE);
        xsltPanel.setSpacing(5);

        moreInfo.setVisible(false);
        formPanel.setSize("100%", "100%");
        formPanel.add(xsltPanel);
        formPanel.add(htmlDocument);
        formPanel.add(moreInfo);
        formPanel.setCellHorizontalAlignment(xsltPanel, HorizontalPanel.ALIGN_RIGHT);
        formPanel.setCellHorizontalAlignment(moreInfo, HorizontalPanel.ALIGN_RIGHT);

        contentsTabPanel.add(formPanel, FormView);
        contentsTabPanel.add(xmlDocument, XmlView);
        contentsTabPanel.selectTab(0);
        contentsTabPanel.setAnimationEnabled(true);
        contentsTabPanel.setSize("100%", "100%");
        contentsTabPanel.setStylePrimaryName("gwt-TabPanelBottom");
        setAriaForContentsTab();
        return contentsTabPanel;
    }

    private void createServicePanel() {
        tree = new ExplorerTree(xmlDocument, htmlDocument, true);
        tree.setSize("100%", "100%");
        uicdsServicePanel.setLeftWidget(tree);
        uicdsServicePanel.setRightWidget(createServiceElementsTab());
        uicdsServicePanel.setSplitPosition("35%");
        uicdsServicePanel.setSize("100%", (Window.getClientHeight() - 50) + "px");
        com.google.gwt.user.client.Element elem = uicdsServicePanel.getElement();
        if(elem!=null){
        	if(elem.getFirstChild()!=null)
        		if(elem.getFirstChild().getChildCount()>0){
        			Node child = elem.getFirstChild().getChild(1);
        			if(child!=null){
        				NodeList<Element> imgs = ((Element)child).getElementsByTagName("img");
        				if(imgs.getLength()>0)
        					imgs.getItem(0).setAttribute("alt", "");
        			}
        		}
        }
    }

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        createServicePanel();
        createDockPanel();
        rootTabPanel.setSize("100%", "100%");
        HorizontalPanel uicdsPanel = new HorizontalPanel();
        uicdsPanel.setSize("100%", "100%");
        uicdsPanel.add(uicdsServicePanel);
        uicdsPanel.setStyleName("background1");
        rootTabPanel.add(uicdsPanel, UICDS);
        rootTabPanel.add(healthstatusPanel, HEALTHSTATUS);
        rootTabPanel.setAnimationEnabled(true);
        rootTabPanel.selectTab(0);
        rootTabPanel.setStyleName("background");
        setAriaForRootTab();
        RootPanel.get().add(rootTabPanel);
        Window.addWindowCloseListener(windowCloseListener);
    }

    private void setAriaForRootTab() {
        rootTabBar = rootTabPanel.getTabBar();
        rootTabNodes = rootTabBar.getElement().getFirstChild().getFirstChild().getChildNodes();

        uicdsTabElem = (Element) rootTabNodes.getItem(1).getFirstChild();
        hnsTabElem = (Element) rootTabNodes.getItem(2).getFirstChild();

    	// set Title fro error in wave toolbar
        Element uicdsInputElem=(Element)uicdsTabElem.getFirstChild().getChild(1).getChild(1).getFirstChild().getFirstChild().getFirstChild();
        uicdsInputElem.setTitle(UICDS);
        Element hnsInputElem=(Element)hnsTabElem.getFirstChild().getChild(1).getChild(1).getFirstChild().getFirstChild().getFirstChild();
        hnsInputElem.setTitle(HEALTHSTATUS);
        
        uicdsTabElem.setAttribute("aria-selected", "true");
        uicdsTabElem.setAttribute("aria-label",
                "Uicds Explorer View with treeitems and its contents");
        hnsTabElem.setAttribute("aria-selected", "false");
        hnsTabElem.setAttribute("aria-label",
                "Health and Status View with core components and shared core statuses");
        rootTabPanel.getTabBar().addSelectionHandler(rootTabHandler);
    }

    private void setAriaForContentsTab() {
        contentsTabBar = contentsTabPanel.getTabBar();
        contentsTabNodes = contentsTabBar.getElement().getFirstChild().getFirstChild()
                .getChildNodes();

        formTabElem = (Element) contentsTabNodes.getItem(1).getFirstChild();
        xmlTabElem = (Element) contentsTabNodes.getItem(2).getFirstChild();

        Element formInputElem=(Element)formTabElem.getFirstChild().getChild(1).getChild(1).getFirstChild().getFirstChild().getFirstChild();
        formInputElem.setTitle(FormView);
        Element xmlInputElem=(Element)xmlTabElem.getFirstChild().getChild(1).getChild(1).getFirstChild().getFirstChild().getFirstChild();
        xmlInputElem.setTitle(XmlView);
        
        formTabElem.setAttribute("aria-selected", "true");
        formTabElem.setAttribute("aria-label",
                "Form or Html description of the chosen treeitem of UicdsExplorer tree");

        xmlTabElem.setAttribute("aria-selected", "false");
        xmlTabElem.setAttribute("aria-label",
                "Xml description of the chosen treeitem of UicdsExplorer tree");

        contentsTabPanel.getTabBar().addSelectionHandler(contentsTabHandler);
    }

    SelectionHandler<Integer> rootTabHandler = new SelectionHandler<Integer>() {

        @Override
        public void onSelection(SelectionEvent<Integer> arg0) {
            Integer item = arg0.getSelectedItem();
            if (item == 0) {
                uicdsTabElem.setAttribute("aria-selected", "true");
                hnsTabElem.setAttribute("aria-selected", "false");
            } else {
                uicdsTabElem.setAttribute("aria-selected", "false");
                hnsTabElem.setAttribute("aria-selected", "true");
            }
        }
    };

    SelectionHandler<Integer> contentsTabHandler = new SelectionHandler<Integer>() {

        @Override
        public void onSelection(SelectionEvent<Integer> arg0) {
            Integer item = arg0.getSelectedItem();
            if (item == 0) {
                formTabElem.setAttribute("aria-selected", "true");
                xmlTabElem.setAttribute("aria-selected", "false");
            } else {
                formTabElem.setAttribute("aria-selected", "false");
                xmlTabElem.setAttribute("aria-selected", "true");
            }
        }
    };

    @SuppressWarnings("deprecation")
    WindowCloseListener windowCloseListener = new WindowCloseListener() {
        public void onWindowClosed() {
        }

        public String onWindowClosing() {
            healthStatusDock.initiateShutdown();
            return null;
        }

    };
}
