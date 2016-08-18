package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TreeItem;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.ServiceConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class ServiceTreeItem extends AbstractTreeItem implements UICDSTreeItem, UICDSConstants {

    public static final String rootName = ServiceFolder;

    private final Map<String, ServiceConfigGWT> serviceMap = new TreeMap<String, ServiceConfigGWT>();

    public ServiceTreeItem() {
        super(rootName);
    }

    @Override
    public void cleanUp() {

        serviceMap.clear();
        this.removeItems();
    }

    public void onLoad() {

        reload();
        getElement().getFirstChildElement().setAttribute("aria-label", SERVICE_DESCRIPTION);
        getElement().getFirstChildElement().setAttribute("aria-live", "off");
        Accessibility.setRole(getElement(), "status");
    }

    @Override
    public void onRightClick(String itemName, int x, int y) {

    }

    public void onSelection(SelectionEvent<TreeItem> e) {

        String serviceName = e.getSelectedItem().getText();

        ServiceConfigGWT serviceGWT = serviceMap.get(serviceName);
        if (serviceGWT == null) {
            Window.alert("Cannot find the service: " + serviceName);
        } else {
            final TextArea xmlDocument = ((ExplorerTree) e.getSelectedItem().getTree()).getXmlDocument();
            Util.setXmlDocument(xmlDocument, serviceGWT.getXML());
            final HTML htmlDocument = ((ExplorerTree) e.getSelectedItem().getTree()).getHtmlDocument();
            htmlDocument.setHTML("");
        }
    }

    @Override
    public void refreshFolder() {

        cleanUp();
        reload();
    }

    private void reload() {

        BaseController.directoryServiceProxyAsync.getServiceList("",
            new AsyncCallback<List<ServiceConfigGWT>>() {
                public void onFailure(Throwable e) {

                    Window.alert("GetListOfServices: " + e.getMessage());
                }

                public void onSuccess(List<ServiceConfigGWT> serviceList) {

                    for (ServiceConfigGWT service : serviceList) {
                        serviceMap.put(service.getServiceName(), service);
                    }
                    Set<String> serviceNameSet = serviceMap.keySet();
                    for (String sName : serviceNameSet) {
                        addItem(SafeHtmlUtils.fromString(sName));
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
    
    @Override
	public void agreementCreatePanelHide(){
		
	}

}
