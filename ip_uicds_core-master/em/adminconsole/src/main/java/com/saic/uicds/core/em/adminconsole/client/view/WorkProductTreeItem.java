package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
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
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;
import com.saic.uicds.core.em.adminconsole.client.model.WorkProductGWT;

public class WorkProductTreeItem extends AbstractTreeItem implements UICDSTreeItem, UICDSConstants {

    public static final String rootName = WorkProductFolder;

    private final Map<String, WorkProductGWT> wpMap = new TreeMap<String, WorkProductGWT>();
    private final WorkProductCloseArchivePanel closeArchivePanel = new WorkProductCloseArchivePanel();
    private final WorkProductDenyPanel denyPanel = new WorkProductDenyPanel();
    private final Map<String, String> previousXsltId = new HashMap<String, String>();
    ListBox xsltListBox;
    String wpID;
    TextArea xmlDocument;
    HTML htmlDocument;
    boolean getDigest = true;
//    Button moreButton;
    String xsltDirectory;

    public WorkProductTreeItem() {

        super(rootName);

    }

    @Override
    public void cleanUp() {

        wpMap.clear();
        this.removeItems();
    }

    public void onLoad() {

        reload();
        getElement().getFirstChildElement().setAttribute("aria-label", WORKPRODUCT_DESCRIPTION);
        getElement().getFirstChildElement().setAttribute("aria-live", "off");
        Accessibility.setRole(getElement(), "status");
    }

    @Override
    public void onRightClick(String itemName, int x, int y) {

        if (itemName.equals(rootName) == false) {
            WorkProductGWT wp = wpMap.get(itemName);
            if (wp.getProductType().equals("Incident")) {
                // Util.ERROR(((wp.isClosed() ? "Archive " : "Close ") + "Product: "
                // + wp.getProductID() + " is invalid operation.\nPlease "
                // + (wp.isClosed() ? "Archive " : "Close ") + "Incident: " + wp.getIncidentID() +
                // " instead"));
                denyPanel.setPopupPosition(x + LocationOffset, y + LocationOffset);
                denyPanel.setMessage((wp.isClosed() ? "Archive " : "Close "), wp.getProductID(),
                        wp.getIncidentID());
                denyPanel.show();
            } else {
                // Util.DEBUG(itemName + " status: " + (wp.isClosed() ? " closed" : "not closed"));
                closeArchivePanel.setPopupPosition(x + LocationOffset, y + LocationOffset);
                closeArchivePanel.setProduct(wp);
                closeArchivePanel.show();
            }
        }
    }

    public void onSelection(SelectionEvent<TreeItem> e) {

        wpID = e.getSelectedItem().getText();

        // use of xsltDropDown and partial information
        ExplorerTree tree = (ExplorerTree) e.getSelectedItem().getTree();
        VerticalPanel formPanel = (VerticalPanel) ((DecoratedTabPanel) ((HorizontalSplitPanel) tree
                .getParent()).getRightWidget()).getWidget(0);
        HorizontalPanel xsltPanel = (HorizontalPanel) formPanel.getWidget(0);
//        moreButton = (Button) formPanel.getWidget(2);
//        moreButton.setVisible(false);
//        moreButton.addClickHandler(moreInfoHandler);

        xmlDocument = ((ExplorerTree) e.getSelectedItem().getTree()).getXmlDocument();
        htmlDocument = ((ExplorerTree) e.getSelectedItem().getTree()).getHtmlDocument();
        htmlDocument.setHTML("");
        xsltPanel.setVisible(true);
        xsltListBox = (ListBox) xsltPanel.getWidget(1);
        xsltListBox.getElement().setAttribute("aria-label", "List of stylesheets available for the selected workProduct");
        xsltListBox.setEnabled(true);
        xsltListBox.clear();

        WorkProductGWT wp = wpMap.get(wpID);
//        if (wp.hasDigest()) {
//            getDigest = true;
//            moreButton.setVisible(true);
//        } else {
//            getDigest = false;
//        }

        // get all availble xslts and load the dropdown list
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
                            if (!contains) {
                                xsltListBox.setSelectedIndex(0);
                                previousXsltId.put(wpID, xsltListBox.getItemText(0));
                            }
                            xsltListBox.setVisibleItemCount(1);
                            String id = previousXsltId.get(wpID);
                            boolean showDefaultWP = false;
                            if (id.equals("default")) {
                                showDefaultWP = true;
                            }
                            if (id.equals(xsltListBox.getItemText(0))) {
                                showDefault(showDefaultWP);
                            } else {
                                BaseController.workProductServiceProxyAsync.getProduct(wpID, id,
                                        showDefaultWP,
                                        new AsyncCallback<WorkProductGWT>() {
                                            public void onFailure(Throwable e) {

                                                Window.alert("Cannot get work product: "
                                                        + e.getMessage());
                                            }

                                            public void onSuccess(WorkProductGWT result) {

                                                Util.setXmlDocument(xmlDocument,
                                                        result.getProduct());
                                                htmlDocument.setHTML(result.getProductHtml());
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
        xsltListBox.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(KeyUpEvent arg0) {
				loadWorkProduct(getDigest);
			}
		});
    }

    private void showDefault(boolean showDefaultWP) {
        BaseController.workProductServiceProxyAsync.getProduct(wpID, showDefaultWP,
                new AsyncCallback<WorkProductGWT>() {
                    public void onFailure(Throwable e) {

                        Window.alert("Cannot get work product: " + e.getMessage());
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
            loadWorkProduct(getDigest);
//            if (getDigest)
//                moreButton.setVisible(true);
//            else
//                moreButton.setVisible(false);
        }
    };

    ClickHandler moreInfoHandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent arg0) {
            loadWorkProduct(false);
//            moreButton.setVisible(false);
        }
    };

    public void loadWorkProduct(boolean getDigest) {
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

        BaseController.workProductServiceProxyAsync
                .listAllWorkProducts(new AsyncCallback<List<WorkProductGWT>>() {
                    public void onFailure(Throwable e) {

                        Window.alert("GetListOfWorkProducts: " + e.getMessage());
                    }

                    public void onSuccess(List<WorkProductGWT> wpList) {

                        for (WorkProductGWT wp : wpList) {
                            wpMap.put(wp.getProductID(), wp);
                        }
                        Set<String> wpIDSet = wpMap.keySet();
                        for (String wpID : wpIDSet) {
                            addItem(SafeHtmlUtils.fromString(wpID));
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
