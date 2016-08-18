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

public class InterestGroupCloseArchivePanel extends PopupPanel implements
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
	private String igType;
	private boolean closed;
	private boolean closeAndArchive;

	public boolean isCloseAndArchive() {
		return closeAndArchive;
	}

	public void setCloseAndArchive(boolean closeAndArchive) {
		this.closeAndArchive = closeAndArchive;
	}

	public String getIgType() {
		return igType;
	}

	public InterestGroupCloseArchivePanel() {

		super(true, true);

		MenuBar mb = new MenuBar(true);

		initConfirmationDialogBox();

		setWidget(mb);
		hide();
	}


	private void initConfirmationDialogBox() {

		String title = "Close/Archive InterestGroup Instances Confirmation";
		confirmationDialogBox.setText(title);

		FlexTable layout = new FlexTable();
		layout.setCellSpacing(LayoutCellSpacing);
		FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

		cellFormatter.setColSpan(0, 0, 2);
		cellFormatter.setHorizontalAlignment(0, 0,
				HasHorizontalAlignment.ALIGN_CENTER);

		// layout.setText(1, 0, operation + " " + productID);
		layout.setWidget(1, 0, operationLabel);
		layout.setText(1, 1, "InterestGroup");
		layout.setWidget(1, 2, idLabel);

		// Add a ok button at the bottom of the dialog
		Button okButton = new Button(OkButtonText, new ClickHandler() {
			public void onClick(ClickEvent event) {
				BaseController.interestGroupManagementServiceProxyAsync.closeArchiveAllInstancesOfIG(getIgType(), new CloseArchiveConfirmationCB());
//				Util.DEBUG("Close and archive all: "+ getIgType());
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

	public void setIgType(String igType) {

		this.igType = igType;

		MenuBar mb = (MenuBar) this.getWidget();
		mb.clearItems();
		operationLabel.setText(CloseAndArchiveAll);
		MenuItem menuItem = new MenuItem(CloseAndArchiveAll, new Command() {

			@Override
			public void execute() {

				confirmationDialogBox.show();
				hide();
			}
		});
		mb.addItem(menuItem);
		idLabel.setText(getIgType());
	}
}
