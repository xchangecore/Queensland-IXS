package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.ui.TreeItem;

public interface UICDSTreeItem {

    public void cleanUp();

    public void onLoad();

    public void onRightClick(String itemName, int x, int y);

    public void onSelection(SelectionEvent<TreeItem> event);

    public void refreshFolder();

    public String type();

	public void agreementCreatePanelHide();
    
}
