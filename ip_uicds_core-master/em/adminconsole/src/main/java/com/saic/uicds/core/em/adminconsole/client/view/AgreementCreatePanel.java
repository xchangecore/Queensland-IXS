package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
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
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.healthstatus.CustomDialogBox;
import com.saic.uicds.core.em.adminconsole.client.model.AgreementGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class AgreementCreatePanel extends PopupPanel implements UICDSConstants {

	private class CreateAgreementCB implements AsyncCallback<AgreementGWT> {

		public void onFailure(Throwable arg0) {

			Util.ERROR("Failed to create agreement");
		}

		public void onSuccess(AgreementGWT createdAgreementGWT) {
			DecoratedTabPanel rootTabPanel = (DecoratedTabPanel) RootPanel
					.get().getWidget(0);
			HorizontalPanel hPanel = (HorizontalPanel) rootTabPanel
					.getWidget(0);
			HorizontalSplitPanel splitPanel = (HorizontalSplitPanel) hPanel
					.getWidget(0);
			ExplorerTree tree = (ExplorerTree) splitPanel.getLeftWidget();
			tree.refreshFolder(AgreementFolder);
			tree.refreshFolder(CoreFolder);
		}
	}

	private final DialogBox createAgreementDialog = new DialogBox();
	private final CustomDialogBox createAgreementCustomDialog = new CustomDialogBox(
			CreateAgreement, false);

	private final TextBox coreNameField = new TextBox();

	// FocusPanel focusPanel = new FocusPanel();

	public AgreementCreatePanel() {

		super(true, true);

		// Create the edit menu
		MenuBar createMenu = new MenuBar(true);

		MenuItem menuItem = new MenuItem(CreateAgreement, new Command() {

			@Override
			public void execute() {

				// createAgreementDialog.show();

				createAgreementCustomDialog.showDialog();
				hide();

			}
		});
		createMenu.addItem(menuItem);
		// initAgreementCreateDialog();
		initAgreementCreateCustomDialog();

		setWidget(createMenu);
		hide();
		// focusPanel.setFocus(true);
	}

	public AgreementCreatePanel(boolean keyboard) {

		super(true, true);
		if (keyboard) {
			// initAgreementCreateDialog();
			initAgreementCreateCustomDialog();
			// createAgreementDialog.show();
			// createAgreementCustomDialog.setWidth("600px");
			createAgreementCustomDialog.showDialog();
		}

	}

	private void initAgreementCreateDialog() {

		// Create a table to layout the form options
		FlexTable layout = new FlexTable();
		layout.setWidth("100%");
		layout.setCellSpacing(LayoutCellSpacing);
		FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

		// Add a title to the form
		// layout.setHTML(0, 0, constants.cwDecoratorPanelFormTitle());
		cellFormatter.setColSpan(0, 0, 2);
		cellFormatter.setHorizontalAlignment(0, 0,
				HasHorizontalAlignment.ALIGN_CENTER);

		layout.setHTML(1, 0, RemoteCoreField);
		coreNameField.setFocus(true);
		coreNameField.setCursorPos(1);
		coreNameField.setTabIndex(1);
		layout.setWidget(1, 1, coreNameField);

		// Add the remote core example
		layout.setText(2, 0, RemoteCoreExampleField);

		// Add a ok button at the bottom of the dialog
		Button okButton = new Button(OkButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				// Util.DEBUG("Ok: " + coreNameField.getText());
				AgreementGWT createAgreementGWT = new AgreementGWT();
				createAgreementGWT.setAgreementTreeTitle(coreNameField
						.getText());
				createAgreementGWT.setRemoteCore(coreNameField.getText());
				BaseController.agreementServiceProxyAsync.createAgreement(
						createAgreementGWT, new CreateAgreementCB());
				coreNameField.setText("");
				coreNameField.setTabIndex(-1);
				createAgreementDialog.hide();

			}
		});
		layout.setWidget(4, 1, okButton);

		// Add a cancel button at the bottom of the dialog
		Button cancelButton = new Button(CancelButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				coreNameField.setText("");
				createAgreementDialog.hide();

			}
		});
		layout.setWidget(4, 2, cancelButton);

		// Wrap the content in a DecoratorPanel
		DecoratorPanel decPanel = new DecoratorPanel();
		decPanel.setWidget(layout);
		createAgreementDialog.setWidget(decPanel);
		createAgreementDialog.center();
		createAgreementDialog.hide();

		new Tree().addKeyPressHandler(new KeyPressHandler() {

			@Override
			public void onKeyPress(KeyPressEvent e) {
				Window.alert("esc event in createAgreementDialog");
				createAgreementDialog.hide();

			}
		});
	}

	private void initAgreementCreateCustomDialog() {

		FlexTable layout = new FlexTable();
		layout.setCellSpacing(LayoutCellSpacing);

		FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

		// Add a title to the form

		cellFormatter.setColSpan(0, 0, 2);
		cellFormatter.setHorizontalAlignment(0, 0,
				HasHorizontalAlignment.ALIGN_CENTER);

		layout.setHTML(1, 0, RemoteCoreField);

		coreNameField.setFocus(true);
		coreNameField.setTabIndex(0);
		coreNameField.getElement().setAttribute("aria-label",
				"Enter the Agreement name");
		layout.setWidget(1, 1, coreNameField);

		// Add the remote core example
		layout.setText(2, 0, RemoteCoreExampleField);

		// Add a ok button at the bottom of the dialog
		Button okButton = new Button(OkButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				// Util.DEBUG("Ok: " + coreNameField.getText());
				AgreementGWT createAgreementGWT = new AgreementGWT();
				createAgreementGWT.setAgreementTreeTitle(coreNameField
						.getText());
				createAgreementGWT.setRemoteCore(coreNameField.getText());
				BaseController.agreementServiceProxyAsync.createAgreement(
						createAgreementGWT, new CreateAgreementCB());
				coreNameField.setText("");
				coreNameField.setTabIndex(-1);
				createAgreementCustomDialog.hide();

			}
		});

		okButton.getElement().setAttribute("aria-label", "Agreement OK button");
		layout.setWidget(4, 1, okButton);

		// Add a cancel button at the bottom of the dialog
		Button cancelButton = new Button(CancelButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				coreNameField.setText("");
				createAgreementCustomDialog.hide();

			}
		});
		cancelButton.getElement().setAttribute("aria-label",
				"Agreement cancel button");
		layout.setWidget(4, 2, cancelButton);

		createAgreementCustomDialog.setWidget(layout);
		createAgreementCustomDialog.center();
		createAgreementCustomDialog.hide();

		new Tree().addKeyPressHandler(new KeyPressHandler() {

			@Override
			public void onKeyPress(KeyPressEvent e) {
				Window.alert("esc event in createAgreementDialog");
				createAgreementCustomDialog.hide();

			}
		});

		createAgreementCustomDialog.getElement().setAttribute("tabIndex", "0");
		createAgreementCustomDialog.hide();

	}
}
