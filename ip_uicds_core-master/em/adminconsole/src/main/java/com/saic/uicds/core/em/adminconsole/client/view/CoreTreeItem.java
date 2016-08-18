package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.TreeItem;

import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.CoreConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class CoreTreeItem extends AbstractTreeItem implements UICDSTreeItem, UICDSConstants {

    public static final String rootName = CoreFolder;

    private final Set<CoreConfigGWT> coreSet = new HashSet<CoreConfigGWT>();

    public CoreTreeItem() {

        super(rootName);
    }

    @Override
    public void cleanUp() {

        coreSet.clear();
        this.removeItems();
    }

    public void onLoad() {

        reload();
        getElement().getFirstChildElement().setAttribute("aria-label", CORE_DESCRIPTION);
        getElement().getFirstChildElement().setAttribute("aria-live", "off");
        Accessibility.setRole(getElement(), "status");
    }

    @Override
    public void onRightClick(String itemName, int x, int y) {

    }

    public void onSelection(SelectionEvent<TreeItem> e) {
    	getElement().getFirstChildElement().setAttribute("aria-live", "polite");
    	
        /*
         * BaseController.workProductServiceProxyAsync.getProduct(e.getSelectedItem().getText(), new
         * AsyncCallback<WorkProductGWT>() { public void onSuccess(WorkProductGWT result) {
         * Window.alert(result.toString()); }
         * 
         * public void onFailure(Throwable arg0) { Window.alert("Unable to retrieve XML"); } });
         */
    }

    @Override
    public void refreshFolder() {

        cleanUp();
        reload();
    }

    private void reload() {

        BaseController.directoryServiceProxyAsync.getCoreList(new AsyncCallback<List<CoreConfigGWT>>() {

            public void onFailure(Throwable e) {

                Util.ERROR("Directory Service: getCoreList: failed." + e.getMessage());
            }

            public void onSuccess(List<CoreConfigGWT> coreList) {

                for (CoreConfigGWT core : coreList) {

                    addItem(core.getCoreName() + " [" + core.getOnlineStatus() + "]");
                    coreSet.add(core);

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
