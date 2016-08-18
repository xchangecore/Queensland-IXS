package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.healthstatus.CustomDialogBox;
import com.saic.uicds.core.em.adminconsole.client.model.ResourceInstanceGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

/**
 * ResourceInstanceEditPanel [Popup Panel with options: {Remove} ]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.client.view
 */

public class ResourceInstanceEditPanel extends PopupPanel implements
		UICDSConstants {

	private class RemoveResourceInstanceCB implements AsyncCallback<String> {
		public void onFailure(Throwable e) {

			Util.DEBUG("Remove Resource: failed: " + e.getMessage());
		}

		public void onSuccess(String profileID) {
			DecoratedTabPanel rootTabPanel = (DecoratedTabPanel) RootPanel
					.get().getWidget(0);
			HorizontalPanel hPanel = (HorizontalPanel) rootTabPanel
					.getWidget(0);
			HorizontalSplitPanel splitPanel = (HorizontalSplitPanel) hPanel
					.getWidget(0);
			ExplorerTree tree = (ExplorerTree) splitPanel.getLeftWidget();
			tree.refreshFolder(ResourceInstanceFolder);
		}
	}

	private ResourceInstanceGWT resourceInstance;
	CustomDialogBox confirmDialogBox;

	public ResourceInstanceEditPanel() {

		super(true, true);

		MenuBar editMenu = new MenuBar(true);
		MenuItem menuItem = new MenuItem(RemoveResourceInstance, new Command() {

			@Override
			public void execute() {
				confirmRemoveResourceInstance();

				hide();
			}

		});
		editMenu.addItem(menuItem);

		setWidget(editMenu);
		center();
		hide();
	}

	private void confirmRemoveResourceInstance() {
		// remove resourceInstance by unregistering it with
		// resourceInstanceService
		Button okButton = new Button("OK");
		okButton.getElement().setAttribute("aria-label", "Confirm OK button");
		VerticalPanel panel = new VerticalPanel();
		panel.setSpacing(10);
		panel.add(new Label("Confirm Deleting ResourceInstance:  Click OK"));
		panel.add(okButton);
		panel
				.setCellHorizontalAlignment(okButton,
						HorizontalPanel.ALIGN_CENTER);
		okButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent arg0) {
				confirmDialogBox.hide();
				BaseController.resourceInstanceServiceProxyAsync
						.deleteResourceInstance(getResourceInstance()
								.getIdentifier(),
								new RemoveResourceInstanceCB());
			}
		});
		confirmDialogBox = new CustomDialogBox("Remove Resource Instance", true);
		confirmDialogBox.setWidget(panel);
		confirmDialogBox.center();
		confirmDialogBox.showDialog();

	}

	public ResourceInstanceGWT getResourceInstance() {

		return resourceInstance;
	}

	public void setResourceInstance(ResourceInstanceGWT resourceInstance) {

		this.resourceInstance = resourceInstance;
	}

	public void removeResourceInstanceFromKeyboard() {
		confirmRemoveResourceInstance();
	}

}
