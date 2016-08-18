package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.healthstatus.CustomDialogBox;
import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;
import com.saic.uicds.core.em.adminconsole.client.model.ResourceInstanceGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

/**
 * ResourceInstanceEditDialog [DialogBox with fields for ResourceInstance
 * params: Identifier, ResourceId, and ProfielIds. CreateResourceInstance
 * operation is initiated here]
 * 
 * @author Santhosh Amanchi - Image Matters, LLC
 * @package com.saic.uicds.core.em..adminconsole.client.view
 */

public class ResourceInstanceEditDialog extends CustomDialogBox implements
		UICDSConstants {

	private class UpdateResourceInstanceAyncCB implements
			AsyncCallback<ResourceInstanceGWT> {
		public void onFailure(Throwable e) {
			Util.ERROR("Create/Update ResorceInstance instance: failed: "
					+ e.getMessage());
		}

		public void onSuccess(ResourceInstanceGWT resourceInstance) {
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

	private final TextBox identifierField = new TextBox();
	private final TextBox localResourceIdField = new TextBox();
	private final TextBox profilesField = new TextBox();
	private final ListBox profileIdListBox = new ListBox();
	int selectedProfile = 0;

	public ResourceInstanceEditDialog(ResourceInstanceGWT resourceInstance) {

		super(null, true);
		this.resourceInstance = resourceInstance;
		resetFields();
	}

	public ResourceInstanceEditDialog(String title) {
		super(title, true);
		setTitle(title);
		setText(title);
	}

	public void createResourceInstanceDialogItems() {

		// Create a table to layout the form options
		FlexTable layout = new FlexTable();
		layout.setCellSpacing(LayoutCellSpacing);

		// Identifier
		layout.setHTML(1, 0, IdentifierField);
		identifierField.setCursorPos(0);
		layout.setWidget(1, 1, identifierField);

		// localResourceId
		layout.setHTML(2, 0, LocalResourceIdField);
		layout.setWidget(2, 1, localResourceIdField);
		setAriaLabels();
		profileIdListBox.clear();
		profileIdListBox.setVisibleItemCount(1);
		profileIdListBox.setSelectedIndex(0);
		profileIdListBox.addItem("Select Profile");

		BaseController.profileServiceProxyAsync
				.getProfileList(new AsyncCallback<List<ProfileGWT>>() {
					public void onFailure(Throwable e) {
						Util.DEBUG("Unable to retrieve XML");
					}

					public void onSuccess(List<ProfileGWT> profileList) {
						for (ProfileGWT profile : profileList) {
							profileIdListBox.addItem(profile.getRefName());
						}
					}
				});
		ChangeHandler handler = new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				selectedProfile = profileIdListBox.getSelectedIndex();
			}
		};
		profileIdListBox.addChangeHandler(handler);
		// ProfileId
		layout.setHTML(3, 0, Profile);
		layout.setWidget(3, 1, profileIdListBox);

		// Add a ok button at the bottom of the dialog
		Button okButton = new Button(OkButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (selectedProfile == 0) {
					Util.ERROR("Select a Resource Profile!");
				} else {
					ResourceInstanceGWT resourceInstanceGWT = new ResourceInstanceGWT();
					resourceInstanceGWT
							.setIdentifier(identifierField.getText());
					// SourceIdentification
					resourceInstanceGWT.setLocalResourceId(localResourceIdField
							.getText());
					// Profiles
					Set<String> profiles = new HashSet<String>();
					profiles.add(profileIdListBox.getItemText(selectedProfile));
					resourceInstanceGWT.setProfiles(profiles);

					// Send request to ResourceInstanceService to create a new
					// instance
					if (isCreateResourceInstance()) {
						BaseController.resourceInstanceServiceProxyAsync
								.createResourceInstance(resourceInstanceGWT,
										new UpdateResourceInstanceAyncCB());
					}
					resetFields();
					hide();
				}
			}
		});
		okButton.getElement().setAttribute("aria-label",
				"Create resource instance OK button");
		layout.setWidget(4, 1, okButton);

		// Add a cancel button at the bottom of the dialog
		Button cancelButton = new Button(CancelButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {
				resetFields();
				hide();
			}
		});
		cancelButton.getElement().setAttribute("aria-label",
				"Create Resource instance cancel button");
		layout.setWidget(4, 2, cancelButton);

		setWidget(layout);
		center();
		hide();
	}

	private void setAriaLabels() {
		identifierField.getElement().setAttribute("aria-label",
				"Enter resource identifier");
		localResourceIdField.getElement().setAttribute("aria-label",
				"Enter localResourceID");
		profileIdListBox.getElement().setAttribute("aria-label",
				"select profile");

	}

	public ResourceInstanceGWT getResource() {
		return resourceInstance;
	}

	private boolean isCreateResourceInstance() {

		return getTitle().equals(CreateResourceInstance);
	}

	private void resetFields() {
		identifierField.setText("");
		localResourceIdField.setText("");
		profilesField.setText("");
	}

	public void setResourceInstance(ResourceInstanceGWT resourceInstance) {
		this.resourceInstance = resourceInstance;
	}

}
