package com.saic.uicds.core.em.adminconsole.client.view;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.healthstatus.CustomDialogBox;
import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class ProfileEditDialog extends CustomDialogBox implements
		UICDSConstants {

	private class UpdateProfileAyncCB implements AsyncCallback<ProfileGWT> {
		public void onFailure(Throwable e) {

			Util.ERROR("Create/Update Profile: failed: " + e.getMessage());
		}

		public void onSuccess(ProfileGWT profile) {
			DecoratedTabPanel rootTabPanel = (DecoratedTabPanel) RootPanel
					.get().getWidget(0);
			HorizontalPanel hPanel = (HorizontalPanel) rootTabPanel
					.getWidget(0);
			HorizontalSplitPanel splitPanel = (HorizontalSplitPanel) hPanel
					.getWidget(0);
			ExplorerTree tree = (ExplorerTree) splitPanel.getLeftWidget();
			tree.refreshFolder(ProfileFolder);
		}
	}

	private ProfileGWT profile;
	private final TextBox identifierField = new TextBox();
	private final TextArea descriptionField = new TextArea();
	private final TextBox interestField = new TextBox();
	private final TextBox resourceField = new TextBox();
	private final TextBox categoryField = new TextBox();
	private final TextBox kindField = new TextBox();
	private final TextBox minCapField = new TextBox();

	public ProfileEditDialog(ProfileGWT profile) {

		super(null, true);
		this.profile = profile;
		resetFields();

		this.addAttachHandler(new AttachEvent.Handler() {

			@Override
			public void onAttachOrDetach(AttachEvent event) {
				if (isAttached()) {
					// setTabOrder();
					identifierField.setFocus(true);
					identifierField.setTabIndex(0);
				} else {
					// clearTabOrder();
				}
			}
		});
	}

	public ProfileEditDialog(String title) {
		super(title, true);
		setTitle(title);
		setText(title);
		getElement().setAttribute("tabIndex", "-1");
		// Create a table to layout the form options
		FlexTable layout = new FlexTable();
		layout.setCellSpacing(LayoutCellSpacing);
		FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();
		setARIALabels();

		// create the top section
		layout.setHTML(1, 0, IdentifierField);
		if (isCreateProfile() == false) {
			identifierField.setReadOnly(true);
		}

		identifierField.setTabIndex(0);

		layout.setWidget(1, 1, identifierField);

		descriptionField.setCharacterWidth(80);
		layout.setHTML(2, 0, DescriptionField);
		layout.setWidget(2, 1, descriptionField);
		if (isCreateProfile()) {
			layout.setHTML(3, 0, InterestField);

			layout.setWidget(3, 1, interestField);
		}

		// create bottom section
		Grid resourceTypes = new Grid(4, 4);
		resourceTypes.setCellSpacing(LayoutCellSpacing);
		resourceTypes.setHTML(0, 0, ResourceField);
		resourceTypes.setWidget(0, 1, resourceField);
		resourceTypes.setHTML(1, 0, CategoryField);
		resourceTypes.setWidget(1, 1, categoryField);
		resourceTypes.setHTML(2, 0, KindField);
		resourceTypes.setWidget(2, 1, kindField);
		resourceTypes.setHTML(3, 0, MinCapField);
		resourceTypes.setWidget(3, 1, minCapField);

		DisclosurePanel discPanel = new DisclosurePanel(ResourceTypeInfo);
		discPanel.setAnimationEnabled(true);
		discPanel.setContent(resourceTypes);
		layout.setWidget(4, 0, discPanel);
		cellFormatter.setColSpan(4, 0, 2);

		// Add a ok button at the bottom of the dialog
		Button okButton = new Button(OkButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				// submit form, by calling create resource profile
				ProfileGWT profileGWT = new ProfileGWT();
				profileGWT.setEntityID(identifierField.getText());
				profileGWT.setIdentifier(identifierField.getText());
				profileGWT.setDescription(descriptionField.getText());

				Map<String, String> resourceTyping = new HashMap<String, String>();
				resourceTyping.put(ResourceField, resourceField.getText());
				resourceTyping.put(CategoryField, categoryField.getText());
				resourceTyping.put(KindField, kindField.getText());
				resourceTyping.put(MinCapField, minCapField.getText());
				profileGWT.setResourceTyping(resourceTyping);

				if (isCreateProfile()
						&& interestField.getText().trim().length() > 0) {
					Set<String> interests = new HashSet<String>();
					interests.add(interestField.getText().trim());
					profileGWT.setInterests(interests);
				}

				if (isCreateProfile()) {
					BaseController.profileServiceProxyAsync.createProfile(
							profileGWT, new UpdateProfileAyncCB());
				} else {
					BaseController.profileServiceProxyAsync.updateProfile(
							profileGWT, new UpdateProfileAyncCB());
				}
				resetFields();
				hide();
			}
		});
		okButton.getElement().setAttribute("aria-label",
				"Create profile OK button");
		layout.setWidget(6, 1, okButton);

		// Add a cancel button at the bottom of the dialog
		Button cancelButton = new Button(CancelButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				// Util.DEBUG("Cancel: " + coreNameField.getText());
				resetFields();
				hide();
			}
		});
		cancelButton.getElement().setAttribute("aria-label",
				"Create profile cancel button");
		layout.setWidget(6, 2, cancelButton);

		setWidget(layout);
		center();
		hide();
	}

	private void setARIALabels() {
		identifierField.getElement().setAttribute("aria-label",
				"Enter profile identifier");
		descriptionField.getElement().setAttribute("aria-label",
				"Enter profile description");
		interestField.getElement().setAttribute("aria-label",
				"Enter profile interest");
		resourceField.getElement().setAttribute("aria-label", "Enter resource");
		categoryField.getElement().setAttribute("aria-label",
				"Enter resource category");
		kindField.getElement()
				.setAttribute("aria-label", "Enter resource kind");
		minCapField.getElement().setAttribute("aria-label",
				"Enter resource minimum capabilities");

	}

	public ProfileGWT getProfile() {

		return profile;
	}

	private boolean isCreateProfile() {

		return getTitle().equals(CreateProfile);
	}

	private void resetFields() {

		identifierField.setText("");
		interestField.setText("");
		descriptionField.setText("");
		resourceField.setText("");
		categoryField.setText("");
		kindField.setText("");
		minCapField.setText("");
	}

	private void setFields(ProfileGWT profile) {

		// Util.DEBUG("ProfileEditDialog: setFields ...");
		identifierField.setText(profile.getIdentifier());
		descriptionField.setText(profile.getDescription());

		Map<String, String> resourceType = profile.getResourceTyping();
		Set<String> keys = resourceType.keySet();
		for (String key : keys) {
			if (key.equals(ResourceField))
				resourceField.setText(resourceType.get(key));
			else if (key.equals(CategoryField))
				categoryField.setText(resourceType.get(key));
			else if (key.equals(KindField))
				kindField.setText(resourceType.get(key));
			else if (key.equals(MinCapField))
				minCapField.setText(resourceType.get(key));
		}
	}

	public void setProfile(ProfileGWT profile) {

		this.profile = profile;
		setFields(profile);
	}
}
