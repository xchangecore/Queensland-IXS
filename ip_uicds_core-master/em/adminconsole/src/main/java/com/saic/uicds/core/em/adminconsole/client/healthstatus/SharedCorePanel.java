package com.saic.uicds.core.em.adminconsole.client.healthstatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.CoreConfigGWT;
import com.saic.uicds.core.em.adminconsole.client.view.Util;

/**
 * SharedCorePanel
 * 
 * Contains CorePanel: List of cores.
 * Handles Core create, update, and delete (presence status and
 * list) on getting Xmpp message from core
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @created
 */
public class SharedCorePanel extends VerticalPanel implements HealthStatusConstants {

    VerticalPanel corePanel = new VerticalPanel();

    Map<String, CorePanel> sharedCores = new HashMap<String, CorePanel>();
    
    String coreName;
    
    public String getCoreName() {
        return coreName;
    }

    public void setCoreName(String coreName) {
        this.coreName = coreName;
    }

    public SharedCorePanel() {
        VerticalPanel eastPanel = new VerticalPanel();
        Label componentLabel = new Label("Shared Cores");
        eastPanel.add(componentLabel);
        corePanel.setSpacing(10);
        eastPanel.add(corePanel);
        eastPanel.setCellHorizontalAlignment(componentLabel, HorizontalPanel.ALIGN_CENTER);
        eastPanel.setSize("375px", "500px");
        eastPanel.setSpacing(15);
        VerticalPanel panel = new VerticalPanel();
        panel.setHeight("100px");
        eastPanel.add(panel);
        add(eastPanel);
    }

    /**
     * Initially load list of cores from directory service
     */
    public void addCores() {
        corePanel.clear();
        sharedCores.clear();
        BaseController.directoryServiceProxyAsync.getCoreName(new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable e) {
                Util.ERROR("Failed to retreive the Core Name: " + e.getMessage());
            }

            @Override
            public void onSuccess(String coreName) {
                setCoreName(coreName);
                addCoresToPanel();
            }

            private void addCoresToPanel() {
                BaseController.directoryServiceProxyAsync
                .getCoreList(new AsyncCallback<List<CoreConfigGWT>>() {
                    public void onFailure(Throwable e) {
                        Util.ERROR("Directory Service: getCoreList: failed." + e.getMessage());
                    }

                    public void onSuccess(List<CoreConfigGWT> coreList) {
                        String coreName=getCoreName();
                        for (CoreConfigGWT core : coreList) {
                            if(!coreName.equals(core.getCoreName())){
                                createCoreIdMap(core.getCoreName(), core.getOnlineStatus());
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Create Core panel and store in map with key=coreName and value=corePanel
     */
    public void createCoreIdMap(String coreId, String coreStatus) {
        CorePanel core = new CorePanel(coreId, coreStatus);
        sharedCores.put(coreId, core);
        corePanel.add(core);
    }

    /**
     * Xmpp message for update - updates presence status 
     */
    public void updateCore(final String coreName) {
        final CorePanel corePanel = sharedCores.get(coreName);
        if (corePanel != null) {
            BaseController.directoryServiceProxyAsync
                    .getCoreList(new AsyncCallback<List<CoreConfigGWT>>() {
                        public void onFailure(Throwable e) {
                            Util.ERROR("Directory Service: getCoreList: failed." + e.getMessage());
                        }

                        public void onSuccess(List<CoreConfigGWT> coreList) {
                            for (CoreConfigGWT core : coreList)
                                if (core.getCoreName().equalsIgnoreCase(coreName)) {
                                    corePanel.setCoreStatus(core.getOnlineStatus());
                                }
                        }
                    });
        }
    }

    /**
     * Xmpp message for create - add new core to list of shared cores
     */
    public void createCore(final String coreName) {
        CorePanel corePanel = sharedCores.get(coreName);
        if (corePanel == null) {
            BaseController.directoryServiceProxyAsync
                    .getCoreList(new AsyncCallback<List<CoreConfigGWT>>() {
                        public void onFailure(Throwable e) {
                            Util.ERROR("Directory Service: getCoreList: failed." + e.getMessage());
                        }

                        public void onSuccess(List<CoreConfigGWT> coreList) {
                            for (CoreConfigGWT core : coreList)
                                if (core.getCoreName().equalsIgnoreCase(coreName)) {
                                    createCoreIdMap(core.getCoreName(), core.getOnlineStatus());
                                }
                        }
                    });
        }
    }

    /**
     * Xmpp message for delete - remove core from list of shared cores
     */
    public void removeCore(final String coreName) {
        CorePanel corePanel = sharedCores.get(coreName);
        if (corePanel != null) {
            this.corePanel.remove(corePanel);
            sharedCores.remove(coreName);
        }
    }

    public void updateCoreStatus(String coreName, String status) {
        CorePanel corePanel = sharedCores.get(coreName);
        if (corePanel != null) {
            corePanel.setCoreStatus(status);
        }
    }

    public void updateCorePopup(String coreName) {
        CorePanel corePanel = sharedCores.get(coreName);
        if (corePanel != null) {
            corePanel.setEnhancedLabel("Enhanced Admin console");
        }
    }

}
