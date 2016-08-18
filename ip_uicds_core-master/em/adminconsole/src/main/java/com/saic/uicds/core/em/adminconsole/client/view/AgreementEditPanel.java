package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.healthstatus.CustomDialogBox;
import com.saic.uicds.core.em.adminconsole.client.model.AgreementGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class AgreementEditPanel extends PopupPanel implements UICDSConstants {

	private class AddRuleCB implements AsyncCallback<AgreementGWT> {

		public void onFailure(Throwable arg0) {

			Util.DEBUG("Failed to add rule");
		}

		public void onSuccess(AgreementGWT updatedAgreementGWT) {

			if (updatedAgreementGWT.getStatusMessage() != null) {
				updatedAgreementGWT.setStatusMessage(null);
			}
			setAgreement(updatedAgreementGWT);
			updateTreeView();
		}

	}

	public void updateTreeView() {
		DecoratedTabPanel rootTabPanel = (DecoratedTabPanel) RootPanel.get()
				.getWidget(0);
		HorizontalPanel hPanel = (HorizontalPanel) rootTabPanel.getWidget(0);
		HorizontalSplitPanel splitPanel = (HorizontalSplitPanel) hPanel
				.getWidget(0);
		ExplorerTree tree = (ExplorerTree) splitPanel.getLeftWidget();
		tree.refreshFolder(AgreementFolder);
		tree.refreshFolder(CoreFolder);
		tree.setTabIndex(0);
	}

	private class DeleteRuleCB implements AsyncCallback<AgreementGWT> {
		public void onFailure(Throwable arg0) {

			Util.DEBUG("Failed to delete rule");
		}

		public void onSuccess(AgreementGWT updatedAgreementGWT) {

			if (updatedAgreementGWT.getStatusMessage() != null) {
				updatedAgreementGWT.setStatusMessage(null);
			}
			setAgreement(updatedAgreementGWT);

			updateTreeView();
		}
	}

	private class RescindAgreementCB implements AsyncCallback<String> {

		public void onFailure(Throwable arg0) {

			Util.DEBUG("Failed to rescind agreement");
		}

		public void onSuccess(String remoteCore) {

			updateTreeView();
		}
	}

	private class ToggleAgreementStatusCB implements
			AsyncCallback<AgreementGWT> {
		public void onFailure(Throwable arg0) {

			Util.DEBUG("Failed to update agreement");
		}

		public void onSuccess(AgreementGWT updatedAgreement) {

			setAgreement(updatedAgreement);
			updateTreeView();
		}
	}

	private AgreementGWT agreement;
	private final DialogBox addRuleDialog = new DialogBox();
	private final DialogBox deleteRuleDialog = new DialogBox();

	private final CustomDialogBox addRuleCustomDialog = new CustomDialogBox(
			AddRule, true);
	private final CustomDialogBox deleteRuleCustomDialog = new CustomDialogBox(
			DeleteRule, true);

	private final TextBox addRuleIdField = new TextBox();
	private final TextBox deleteRuleIdField = new TextBox();
	private final TextBox incidentTypeField = new TextBox();

	public AgreementEditPanel() {

		super(true, true);

		// Create the edit menu
		MenuBar editMenu = new MenuBar(true);
		MenuItem menuItem = new MenuItem(ToggleAgreementStatus, new Command() {
			@Override
			public void execute() {

				BaseController.agreementServiceProxyAsync.updateAgreement(
						agreement, new ToggleAgreementStatusCB());
				hide();
			}
		});
		editMenu.addItem(menuItem);

		// Add Rule
		menuItem = new MenuItem(AddRule, new Command() {
			@Override
			public void execute() {

				addRuleCustomDialog.showDialog();
				hide();
			}
		});
		editMenu.addItem(menuItem);
		initAddRuleDialog();

		// Delete Rule
		menuItem = new MenuItem(DeleteRule, new Command() {
			@Override
			public void execute() {

				deleteRuleCustomDialog.showDialog();
				hide();
			}
		});
		editMenu.addItem(menuItem);
		initDeleteRuleDialog();

		// Rescind Agreement
		menuItem = new MenuItem(RescindAgreement, new Command() {
			@Override
			public void execute() {

				BaseController.agreementServiceProxyAsync.rescindAgreement(
						agreement.getRemoteCore(), new RescindAgreementCB());
				hide();
			}
		});
		editMenu.addItem(menuItem);

		setWidget(editMenu);
		center();
		hide();

	}

	public AgreementEditPanel(AgreementGWT agreement) {
		this();
	}

	public AgreementGWT getAgreement() {

		return agreement;
	}

	private void initAddRuleDialog() {

		addRuleCustomDialog.setText(AddRule);
		FlexTable layout = new FlexTable();
		layout.setCellSpacing(LayoutCellSpacing);
		// FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

		addRuleIdField.setFocus(true);
		addRuleIdField.setTabIndex(0);
		layout.setHTML(1, 0, RuleIdField);
		addRuleIdField.getElement().setAttribute("aria-label", "Rule Id");
		layout.setWidget(1, 1, addRuleIdField);

		layout.setHTML(2, 0, IncidentTypeField);
		incidentTypeField.setTabIndex(0);
		incidentTypeField.getElement().setAttribute("aria-label",
				"Incident Type");
		layout.setWidget(2, 1, incidentTypeField);

		// Add a ok button at the bottom of the dialog
		Button okButton = new Button(OkButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				AgreementGWT addRuleAgreement = new AgreementGWT();
				addRuleAgreement.setShareRulesEnabled(true);
				addRuleAgreement.setRemoteCore(getAgreement().getRemoteCore());
				addRuleAgreement.setRuleId(addRuleIdField.getText());
				addRuleAgreement.setRuleIncidentType(incidentTypeField
						.getText());
				addRuleAgreement.setAgreementXml(getAgreement()
						.getAgreementXml());
				addRuleAgreement.setOperation(AgreementGWT.OPERATION_ADD_RULE);

				BaseController.agreementServiceProxyAsync.updateAgreement(
						addRuleAgreement, new AddRuleCB());
				addRuleIdField.setText("");
				incidentTypeField.setText("");
				addRuleCustomDialog.hide();
			}
		});

		okButton.getElement().setAttribute("aria-label", "Add Rule OK button");
		layout.setWidget(4, 1, okButton);

		// Add a cancel button at the bottom of the dialog
		Button cancelButton = new Button(CancelButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				addRuleIdField.setText("");
				incidentTypeField.setText("");
				addRuleCustomDialog.hide();
			}
		});
		cancelButton.getElement().setAttribute("aria-label",
				"Add Rule cancel button");
		layout.setWidget(4, 2, cancelButton);

		addRuleCustomDialog.setWidget(layout);
		addRuleCustomDialog.center();
		addRuleCustomDialog.hide();
	}

	private void initDeleteRuleDialog() {

		deleteRuleCustomDialog.setText(DeleteRule);
		FlexTable layout = new FlexTable();
		layout.setCellSpacing(LayoutCellSpacing);
		FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

		// add the dialog
		cellFormatter.setColSpan(0, 0, 2);
		cellFormatter.setHorizontalAlignment(0, 0,
				HasHorizontalAlignment.ALIGN_CENTER);

		layout.setHTML(1, 0, RuleIdField);
		deleteRuleIdField.setTabIndex(0);
		deleteRuleIdField.getElement().setAttribute("aria-label",
				"Rule Id to be deleted");
		layout.setWidget(1, 1, deleteRuleIdField);

		// Add a ok button at the bottom of the dialog
		Button okButton = new Button(OkButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				String ruleId = deleteRuleIdField.getText();
				if (ruleId.length() > 0) {
					AgreementGWT delRuleAgreement = new AgreementGWT();
					delRuleAgreement.setShareRulesEnabled(true);
					delRuleAgreement.setRemoteCore(getAgreement()
							.getRemoteCore());
					delRuleAgreement.setAgreementTreeTitle(getAgreement()
							.getRemoteCore());
					delRuleAgreement.setRuleId(ruleId);
					delRuleAgreement.setAgreementXml(getAgreement()
							.getAgreementXml());
					delRuleAgreement
							.setOperation(AgreementGWT.OPERATION_DELETE_RULE);
					BaseController.agreementServiceProxyAsync.updateAgreement(
							delRuleAgreement, new DeleteRuleCB());
				}
				deleteRuleIdField.setText("");
				deleteRuleCustomDialog.hide();
			}
		});

		okButton.getElement().setAttribute("aria-label",
				"delete rule OK button");
		layout.setWidget(4, 1, okButton);

		// Add a cancel button at the bottom of the dialog
		Button cancelButton = new Button(CancelButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				deleteRuleIdField.setText("");
				deleteRuleCustomDialog.hide();
			}
		});
		cancelButton.getElement().setAttribute("aria-label",
				"Rule delete cancel button");
		layout.setWidget(4, 2, cancelButton);

		deleteRuleCustomDialog.setWidget(layout);
		deleteRuleCustomDialog.center();
		deleteRuleCustomDialog.hide();
	}

	public void setAgreement(AgreementGWT agreement) {

		this.agreement = agreement;
	}

	// context menu actions from keyboard
	public void toggleFromKeyboard() {
		BaseController.agreementServiceProxyAsync.updateAgreement(agreement,
				new ToggleAgreementStatusCB());
	}

	public void addRuleFromKeyboard() {
		addRuleCustomDialog.showDialog();
	}

	public void deleteRuleFromKeyboard() {
		deleteRuleCustomDialog.showDialog();
	}

	public void rescindAgrementFromKeyboard() {

		BaseController.agreementServiceProxyAsync.rescindAgreement(agreement
				.getRemoteCore(), new RescindAgreementCB());

	}
}