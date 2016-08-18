package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class IncidentCloseArchivePanel extends PopupPanel implements
		UICDSConstants {

	private class CloseArchiveConfirmationCB implements AsyncCallback<String> {

		public void onFailure(Throwable e) {

			Util.ERROR("Close/Archive Incident: " + e.getMessage());
		}

		public void onSuccess(String returnMessage) {

			Util.DEBUG(returnMessage);
			DecoratedTabPanel rootTabPanel = (DecoratedTabPanel) RootPanel
					.get().getWidget(0);
			HorizontalPanel hPanel = (HorizontalPanel) rootTabPanel
					.getWidget(0);
			HorizontalSplitPanel splitPanel = (HorizontalSplitPanel) hPanel
					.getWidget(0);
			ExplorerTree tree = (ExplorerTree) splitPanel.getLeftWidget();
			tree.refreshFolder(InterestGroupFolder);
		}
	}

	private DialogBox confirmationDialogBox = new DialogBox();
	private Label operationLabel = new Label();
	private Label idLabel = new Label();
	private String incidentID;
	private boolean closed;
	private boolean closeAndArchive;

	public boolean isCloseAndArchive() {
		return closeAndArchive;
	}

	public void setCloseAndArchive(boolean closeAndArchive) {
		this.closeAndArchive = closeAndArchive;
	}

	public IncidentCloseArchivePanel() {

		super(true, true);

		MenuBar mb = new MenuBar(true);

		initConfirmationDialogBox();

		setWidget(mb);
		hide();
	}

	public String getIncidentID() {

		return incidentID;
	}

	private void initConfirmationDialogBox() {

		String title = "Close/Archive Incident Confirmation";
		confirmationDialogBox.setText(title);

		FlexTable layout = new FlexTable();
		layout.setCellSpacing(LayoutCellSpacing);
		FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

		cellFormatter.setColSpan(0, 0, 2);
		cellFormatter.setHorizontalAlignment(0, 0,
				HasHorizontalAlignment.ALIGN_CENTER);

		// layout.setText(1, 0, operation + " " + productID);
		layout.setWidget(1, 0, operationLabel);
		layout.setText(1, 1, "Incident");
		layout.setWidget(1, 2, idLabel);

		// Add a ok button at the bottom of the dialog
		Button okButton = new Button(OkButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				if (isClosed()) {
					BaseController.incidentManagementServiceProxyAsync
							.archiveIncident(getIncidentID(),
									new CloseArchiveConfirmationCB());
				} else {
					if (isCloseAndArchive()) {
						BaseController.incidentManagementServiceProxyAsync
								.closeAndArchiveIncident(getIncidentID(),
										new CloseArchiveConfirmationCB());
					} else {
						BaseController.incidentManagementServiceProxyAsync
								.closeIncident(getIncidentID(),
										new CloseArchiveConfirmationCB());
					}
				}
				confirmationDialogBox.hide();
			}
		});
		layout.setWidget(3, 1, okButton);

		// Add a cancel button at the bottom of the dialog
		Button cancelButton = new Button(CancelButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {

				confirmationDialogBox.hide();
			}
		});
		layout.setWidget(3, 2, cancelButton);

		// Wrap the content in a DecoratorPanel
		DecoratorPanel decPanel = new DecoratorPanel();
		decPanel.setWidget(layout);
		confirmationDialogBox.setWidget(decPanel);
		confirmationDialogBox.center();
		confirmationDialogBox.hide();
	}

	public boolean isClosed() {

		return closed;
	}

	public void setClosed(boolean closed) {

		this.closed = closed;
	}

	public void setIncidentID(String incidentID, boolean closed) {

		setClosed(closed);
		this.incidentID = incidentID;

		MenuBar mb = (MenuBar) this.getWidget();
		mb.clearItems();

		if (isClosed()) {
			operationLabel.setText(ArchiveProduct);
			MenuItem menuItem = new MenuItem(ArchiveProduct, new Command() {

				@Override
				public void execute() {

					confirmationDialogBox.show();
					hide();
				}
			});
			mb.addItem(menuItem);
		} else {
			operationLabel.setText(CloseProduct);
			MenuItem closeMenuItem = new MenuItem(CloseProduct, new Command() {
				@Override
				public void execute() {
					setCloseAndArchive(false);
					confirmationDialogBox.show();
					hide();
				}
			});
			mb.addItem(closeMenuItem);
			MenuItem closeAndArchiveMenuItem = new MenuItem(
					CloseAndArchiveProduct, new Command() {
						@Override
						public void execute() {
							operationLabel.setText(CloseAndArchiveProduct);
							setCloseAndArchive(true);
							confirmationDialogBox.show();
							hide();
						}
					});
			mb.addItem(closeAndArchiveMenuItem);
		}
		idLabel.setText(getIncidentID());
	}
}
