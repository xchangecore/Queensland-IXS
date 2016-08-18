package com.saic.uicds.core.em.adminconsole.client.view;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.saic.uicds.core.em.adminconsole.client.model.UICDSConstants;

public class ProfileCreatePanel extends PopupPanel implements UICDSConstants {

	private final ProfileEditDialog createProfileDialog = new ProfileEditDialog(
			CreateProfile);

	public ProfileCreatePanel() {

		super(true, true);

		// Create the edit menu
		MenuBar createMenu = new MenuBar(true);
		MenuItem menuItem = new MenuItem(CreateProfile, new Command() {
			@Override
			public void execute() {
				createProfileDialog.getElement().setAttribute("tabIndex", "-1");
				createProfileDialog.showDialog();
				hide();
			}
		});
		createMenu.addItem(menuItem);

		setWidget(createMenu);
		hide();
	}

	public ProfileCreatePanel(boolean keyboard) {

		super(true, true);
		createProfileDialog.getElement().setAttribute("tabIndex", "0");
		createProfileDialog.showDialog();

	}
}
