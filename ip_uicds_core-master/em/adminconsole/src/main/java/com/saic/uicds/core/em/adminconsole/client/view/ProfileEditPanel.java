package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.saic.uicds.core.em.adminconsole.client.controller.BaseController;
import com.saic.uicds.core.em.adminconsole.client.healthstatus.CustomDialogBox;
import com.saic.uicds.core.em.adminconsole.client.model.ProfileGWT;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class ProfileEditPanel extends PopupPanel implements UICDSConstants {

	private class EditInterestDialog extends CustomDialogBox implements
			UICDSConstants {

		private class EditInterestCB implements AsyncCallback<String> {
			public void onFailure(Throwable e) {

				Util.ERROR("Add/Remove interest failed: " + e.getMessage());
			}

			public void onSuccess(String arg0) {

				// Util.DEBUG("Add/Remove intest successfully");
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

		private final TextBox interestField = new TextBox();

		private String title;

		public EditInterestDialog(String title) {

			super(title, true);
			setTitle(title);
			setText(getTitle());

			// Create a table to layout the form options
			FlexTable layout = new FlexTable();
			layout.setCellSpacing(LayoutCellSpacing);

			// create the top section
			interestField.setFocus(true);
			layout.setHTML(1, 0, InterestField);
			interestField.getElement().setAttribute("aria-label",
					"Enter interest");
			layout.setWidget(1, 1, interestField);

			// Add a ok button at the bottom of the dialog
			Button okButton = new Button(OkButtonText, new ClickHandler() {
				public void onClick(ClickEvent event) {

					if (isAddInterest()) {
						BaseController.profileServiceProxyAsync.addInterest(
								getProfile().getIdentifier(), interestField
										.getText(), new EditInterestCB());
					} else {
						BaseController.profileServiceProxyAsync.removeInterest(
								getProfile().getIdentifier(), interestField
										.getText(), new EditInterestCB());
					}
					resetFields();
					hide();
				}
			});
			okButton.getElement().setAttribute("aria-label",
					"Add interest OK button");
			layout.setWidget(3, 1, okButton);

			// Add a cancel button at the bottom of the dialog
			Button cancelButton = new Button(CancelButtonText,
					new ClickHandler() {
						public void onClick(ClickEvent event) {

							// Util.DEBUG("Cancel: " + coreNameField.getText());
							resetFields();
							hide();
						}
					});
			cancelButton.getElement().setAttribute("aria-label",
					"Add interest cancel button");
			layout.setWidget(3, 2, cancelButton);

			setWidget(layout);
			center();
			hide();
		}

		@Override
		public String getTitle() {

			return this.title;
		}

		private boolean isAddInterest() {

			return getTitle().equals(AddInterest);
		}

		private void resetFields() {

			interestField.setText("");
		}

		public void setTitle(String title) {

			this.title = title;
		}
	}

	private class RemoveProfileCB implements AsyncCallback<String> {
		public void onFailure(Throwable e) {

			Util.DEBUG("Remove Profile: failed: " + e.getMessage());
		}

		public void onSuccess(String profileID) {

			// Util.DEBUG(profileID + " has been removed successfully");
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
	private final ProfileEditDialog updateProfileDialog = new ProfileEditDialog(
			EditProfile);
	private final EditInterestDialog addInterestDialog = new EditInterestDialog(
			AddInterest);

	private final EditInterestDialog removeInterestDialog = new EditInterestDialog(
			RemoveInterest);

	public ProfileEditPanel() {

		super(true, true);

		MenuBar editMenu = new MenuBar(true);
		MenuItem menuItem = new MenuItem(UpdateProfile, new Command() {

			@Override
			public void execute() {

				updateProfileDialog.setProfile(profile);
				updateProfileDialog.showDialog();
				hide();
			}
		});
		editMenu.addItem(menuItem);

		menuItem = new MenuItem(RemoveProfile, new Command() {

			@Override
			public void execute() {

				BaseController.profileServiceProxyAsync.deleteProfile(
						getProfile().getIdentifier(), new RemoveProfileCB());
				hide();
			}
		});
		editMenu.addItem(menuItem);

		editMenu.addSeparator();

		menuItem = new MenuItem(AddInterest, new Command() {

			@Override
			public void execute() {

				addInterestDialog.showDialog();
				hide();
			}
		});
		editMenu.addItem(menuItem);

		menuItem = new MenuItem(RemoveInterest, new Command() {

			@Override
			public void execute() {

				removeInterestDialog.showDialog();
				hide();
			}
		});
		editMenu.addItem(menuItem);

		setWidget(editMenu);
		center();
		hide();
	}

	public ProfileGWT getProfile() {

		return profile;
	}

	public void setProfile(ProfileGWT profile) {

		this.profile = profile;
	}

	// context menu actions from keyboard
	public void updateProfileFromKeyboard() {
		updateProfileDialog.setProfile(profile);
		updateProfileDialog.showDialog();

	}

	public void removeProfileFromKeyboard() {
		BaseController.profileServiceProxyAsync.deleteProfile(getProfile()
				.getIdentifier(), new RemoveProfileCB());

	}

	public void addInterestFromKeyboard() {
		addInterestDialog.showDialog();

	}

	public void removeInterestFromKeyboard() {
		removeInterestDialog.showDialog();

	}
}
