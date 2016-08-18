package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.IncidentGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;
import com.saic.uicds.core.em.adminconsole.client.model.WorkProductGWT;

public class IncidentTreeItem extends AbstractTreeItem implements UICDSTreeItem, UICDSConstants {

    private class IncidentComparator implements Comparator<IncidentGWT> {

        @Override
        public int compare(IncidentGWT i1, IncidentGWT i2) {

            return i1.getWorkProductID().compareTo(i2.getWorkProductID());
        }
    }

    public static final String rootName = IncidentFolder;

    // the key of the map is a little bit of tricky
    // to implement the sorted list, the key is incident's title plus "/" and incident's id
    private static final Map<String, IncidentGWT> incidentMap = new TreeMap<String, IncidentGWT>();
    private static final Map<String, List<IncidentGWT>> incidentChildrenMap = new TreeMap<String, List<IncidentGWT>>();
    private IncidentCloseArchivePanel incidentCloseArchivePanel = new IncidentCloseArchivePanel();
    ListBox xsltListBox;
    String wpID;
    TextArea xmlDocument;
    HTML htmlDocument;
//    Button moreButton;
    private final Map<String, String> previousXsltId = new HashMap<String, String>();
    String xsltDirectory;

    public IncidentTreeItem() {

        super(rootName);
    }

    public void cleanUp() {

        incidentMap.clear();
        this.removeItems();
    }

    public void onLoad() {

        reload();
        getElement().getFirstChildElement().setAttribute("aria-label", INCIDENT_DESCRIPTION);
        getElement().getFirstChildElement().setAttribute("aria-live", "off");
        Accessibility.setRole(getElement(), "status");
    }

    @Override
    public void onRightClick(String itemName, final int x, final int y) {

        final IncidentGWT incident = incidentMap.get(itemName);
        if (incident != null) {
            /*
             * Util.DEBUG("TreeItem: " + incident.getIncidentID() +
             * " has been selected to close/archive");
             */
            BaseController.incidentManagementServiceProxyAsync.isIncidentActive(incident.getWorkProductID(),new AsyncCallback<Boolean>() {
                @Override
                public void onFailure(Throwable arg0) {
                    Util.ERROR("Failed to get status of incident"+arg0.getMessage());
                }

                @Override
                public void onSuccess(Boolean active) {
                    boolean isClosed=false;
                    if(!active){
                        isClosed=true;
                    }
                    incidentCloseArchivePanel.setIncidentID(incident.getIncidentID(), isClosed);
                    incidentCloseArchivePanel.setPopupPosition(x + LocationOffset, y + LocationOffset);
                    incidentCloseArchivePanel.show();
                }
            });
            
            
        }
    }

    public void onSelection(SelectionEvent<TreeItem> e) {

        TreeItem incidentTree = e.getSelectedItem();
        String incidentTitle = incidentTree.getText();
        String key = incidentTree.getTitle();

        if (!incidentTitle.equals("Select Incident to retrieve its work products")) {

            IncidentGWT incident = incidentMap.get(key);
            // selection is not incident document
            if (incident == null) {
                for (String inc : incidentChildrenMap.keySet()) {
                    List<IncidentGWT> children = incidentChildrenMap.get(inc);
                    for (IncidentGWT child : children) {
                        if (child.getWorkProductID().equals(incidentTitle)) {
                            incident = child;
                            break;
                        }
                    }
                    if (incident != null) {
                        break;
                    }
                }
            } else {
                loadIncidentWorkProducts(incidentTree, incident.getIncidentID());
            }

            if (incident == null) {
                Window.alert(incidentTitle + " not found");
            } else {
                ExplorerTree tree = (ExplorerTree) e.getSelectedItem().getTree();
                VerticalPanel formPanel = (VerticalPanel) ((DecoratedTabPanel) ((HorizontalSplitPanel) tree
                        .getParent()).getRightWidget()).getWidget(0);
                HorizontalPanel xsltPanel = (HorizontalPanel) formPanel.getWidget(0);
//                moreButton = (Button) formPanel.getWidget(2);
//                moreButton.setVisible(false);
//                moreButton.addClickHandler(moreInfoHandler);
                xmlDocument = ((ExplorerTree) e.getSelectedItem().getTree()).getXmlDocument();
                htmlDocument = ((ExplorerTree) e.getSelectedItem().getTree()).getHtmlDocument();
                htmlDocument.setHTML("");
                xsltPanel.setVisible(true);
                xsltListBox = (ListBox) xsltPanel.getWidget(1);
                xsltListBox.setEnabled(true);
                xsltListBox.clear();

                wpID = incident.getWorkProductID();

                BaseController.workProductServiceProxyAsync.getProductXsltIds(wpID,
                        new AsyncCallback<List<String>>() {
                            public void onFailure(Throwable e) {
                                Window.alert("Cannot get XSLT Ids: " + e.getMessage());
                            }

                            public void onSuccess(List<String> xsltIds) {
                                xsltListBox.clear();
                                if (xsltIds.size() > 0) {
                                    String prevId = previousXsltId.get(wpID);
                                    boolean contains = false;
                                    if (prevId == null) {
                                        prevId = xsltIds.get(0).replace("default:", "");
                                        previousXsltId.put(wpID, prevId);
                                        xsltListBox.setSelectedIndex(0);
                                        contains = true;
                                    }
                                    for (String xsltId : xsltIds) {
                                        if (xsltId.contains("default:")) {
                                            xsltId = xsltId.replace("default:", "");
                                        }
                                        xsltListBox.addItem(xsltId);
                                        if (prevId.equals(xsltId)) {
                                            xsltListBox.setSelectedIndex(xsltListBox.getItemCount() - 1);
                                            contains = true;
                                        }
                                    }
                                    xsltListBox.setVisibleItemCount(1);
                                    // xsltListBox.setSelectedIndex(0);
                                    if (!contains) {
                                        xsltListBox.setSelectedIndex(0);
                                        previousXsltId.put(wpID, xsltListBox.getItemText(0));
                                    }
                                    String id = previousXsltId.get(wpID);
                                    boolean showDefaultWP = false;
                                    if (id.equals("default")) {
                                        showDefaultWP = true;
                                    }
                                    if (id.equals(xsltListBox.getItemText(0))) {
                                        showDefault(showDefaultWP);
                                    } else {
                                        BaseController.workProductServiceProxyAsync.getProduct(
                                                wpID, id, showDefaultWP,
                                                new AsyncCallback<WorkProductGWT>() {
                                                    public void onFailure(Throwable e) {

                                                        Window.alert("Cannot get work product: "
                                                                + e.getMessage());
                                                    }

                                                    public void onSuccess(WorkProductGWT result) {

                                                        Util.setXmlDocument(xmlDocument,
                                                                result.getProduct());
                                                        htmlDocument.setHTML(result
                                                                .getProductHtml());
                                                    }
                                                });
                                    }
                                } else {
                                    int ind=xsltDirectory.lastIndexOf("ServerApps");
                                    String dir=xsltDirectory.substring(0,ind);
                                    Util.ERROR("No default xslt found: default.xsl file is missing from directory "
                                            + xsltDirectory
                                            + ". Copy the file from "+dir+"InstallFiles\\conf\\ directory");
                                }
                            }
                        });

                xsltListBox.addChangeHandler(changeHandler);
            }
        }
    }

    private void loadIncidentWorkProducts(final TreeItem incidentTree, final String incidentId) {

        try {
            if (incidentTree.getChildCount() > 0) {
                incidentTree.removeItems();
                incidentChildrenMap.remove(incidentId);
            }
            BaseController.incidentManagementServiceProxyAsync.getListOfWorkProductIncidents(
                    incidentId, new AsyncCallback<List<IncidentGWT>>() {

                        @Override
                        public void onFailure(Throwable e) {
                            Util.ERROR("GetListOfWorkProducts for " + incidentId + " incident: "
                                    + e.getMessage());
                        }

                        @Override
                        public void onSuccess(List<IncidentGWT> workProductsGwt) {

                            incidentChildrenMap.put(incidentId, workProductsGwt);
                            TreeSet<IncidentGWT> sortedChildren = new TreeSet<IncidentGWT>(
                                    new IncidentComparator());
                            for (IncidentGWT workProductGwt : workProductsGwt) {
                                sortedChildren.add(workProductGwt);
                            }

                            for (IncidentGWT child : sortedChildren) {
                                incidentTree.addItem(SafeHtmlUtils.fromString(child.getWorkProductID()));
                            }
                            if(incidentTree.getElement().getElementsByTagName("img").getLength()>0)
                            	incidentTree.getElement().getElementsByTagName("img").getItem(0).setAttribute("alt", ALT_TEXT);
                        }
                    });
        } catch (Exception e) {
            Util.ERROR(e.getMessage());
        }

    }

    private void showDefault(boolean showDefaultWP) {
        BaseController.workProductServiceProxyAsync.getProduct(wpID, showDefaultWP,
                new AsyncCallback<WorkProductGWT>() {
                    public void onFailure(Throwable e) {

                        Window.alert("Cannot get the Incident: " + e.getMessage());
                    }

                    public void onSuccess(WorkProductGWT result) {

                        Util.setXmlDocument(xmlDocument, result.getProduct());
                        htmlDocument.setHTML(result.getProductHtml());
                    }
                });
        previousXsltId.put(wpID, xsltListBox.getItemText(0));
    }

    ChangeHandler changeHandler = new ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
            loadWorkProduct();
        }
    };

    public void loadWorkProduct() {
        int index = xsltListBox.getSelectedIndex();
        String xsltId = xsltListBox.getItemText(index);
        previousXsltId.put(wpID, xsltId);
        boolean showDefaultWP = false;
        if (xsltId.equals("default")) {
            showDefaultWP = true;
        }
        if (index == 0) {
            xsltId = "default:" + xsltId;
        }
        BaseController.workProductServiceProxyAsync.getProduct(wpID, xsltId,
                showDefaultWP, new AsyncCallback<WorkProductGWT>() {
                    public void onFailure(Throwable e) {

                        Window.alert("Cannot get work product: " + e.getMessage());
                    }

                    public void onSuccess(WorkProductGWT result) {

                        Util.setXmlDocument(xmlDocument, result.getProduct());
                        htmlDocument.setHTML(result.getProductHtml());
                    }
                });
    }

    @Override
    public void refreshFolder() {

        cleanUp();
        reload();
    }

    private void reload() {

        BaseController.incidentManagementServiceProxyAsync
                .getListOfIncidents(new AsyncCallback<List<IncidentGWT>>() {
                    @Override
                    public void onFailure(Throwable e) {

                        Util.ERROR("GetListOfIncidents: " + e.getMessage());
                    }

                    @Override
                    public void onSuccess(List<IncidentGWT> incidentList) {

                        for (IncidentGWT incident : incidentList) {
                            incidentMap.put(incident.getTitle() + "/" + incident.getIncidentID(),
                                    incident);
                        }
                        Set<String> incidentIDSet = incidentMap.keySet();
                        for (String incidentID : incidentIDSet) {
                            // here is a little bit of tricky in terms of naming
                            int index = incidentID.indexOf("/");
                            String name = incidentID.substring(0, index);
                            TreeItem iTree = addItem(SafeHtmlUtils.fromString(name));
                            iTree.setTitle(incidentID);
                            HTML html=new HTML("Select Incident to retrieve its work products");
                            html.setStyleName("childNode");
                            iTree.addItem(html);
                            if(iTree.getElement().getElementsByTagName("img").getLength()>0)
                            	iTree.getElement().getElementsByTagName("img").getItem(0).setAttribute("alt", ALT_TEXT);
                        }
                        if(getElement().getElementsByTagName("img").getLength()>0)
                        	getElement().getElementsByTagName("img").getItem(0).setAttribute("alt", ALT_TEXT);
                    }
                });
    }

    @Override
    public String type() {

        return rootName;
    }
    
    public void setXsltDirectory(String url) {
        this.xsltDirectory = url;
    }

    @Override
	public void agreementCreatePanelHide(){
		
	}

}
